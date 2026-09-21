package cn.aiedge.base.util;

import cn.aiedge.base.entity.SysFieldPermission;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.service.SysFieldPermissionService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前用户的字段级权限配置缓存（供 {@link DataMaskSerializer} 序列化时查询）。
 *
 * <p>序列化是高频路径，每个敏感字段都要问一次「当前用户对这个表.字段是怎么配的」，
 * 因此按用户维度缓存整份配置快照，避免逐字段查库。</p>
 *
 * <p><b>为什么用 TTL 而不是显式失效</b>：字段权限配置的写入口在 Controller 层，
 * 而本缓存被 Jackson 回调直接使用，两者没有天然的失效通知路径。
 * TTL 到期自然重载，代价是「管理员改完配置最迟 {@value #TTL_MS} 毫秒后生效」，
 * 这个延迟对权限配置场景可以接受。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
final class FieldPermissionCache {

    private FieldPermissionCache() {
    }

    /** 快照有效期（毫秒）：配置变更后最迟这么久生效 */
    private static final long TTL_MS = 30_000;

    /** 缓存条数上限：超过则整体清空，防止异常场景下无界增长（正常规模远达不到） */
    private static final int MAX_ENTRIES = 5000;

    /** 用户 ID → 该用户的字段权限快照 */
    private static final Map<Long, Entry> CACHE = new ConcurrentHashMap<>();

    /**
     * @param loadedAt     加载时刻
     * @param byTableField 配置索引，key 为 {@code 表名.字段名}
     */
    private record Entry(long loadedAt, Map<String, SysFieldPermission> byTableField) {
    }

    /**
     * 查当前用户对指定「表.字段」的配置。
     *
     * @return 命中的配置；无配置、未登录（定时任务/启动期无请求上下文）、
     *         容器未就绪、查库异常等情况一律返回 null —— 由调用方降级到注解默认值。
     *         脱敏链路的异常绝不能导致整个响应序列化失败
     */
    static SysFieldPermission findForCurrentUser(String table, String field) {
        if (table == null || table.isEmpty() || field == null || field.isEmpty()) {
            return null;
        }
        Map<String, SysFieldPermission> configs = currentUserConfigs();
        return configs.isEmpty() ? null : configs.get(table + "." + field);
    }

    /** 读取当前用户的整份配置（带缓存） */
    private static Map<String, SysFieldPermission> currentUserConfigs() {
        try {
            SysUserRoleMapper userRoleMapper = SpringContextHolder.getBeanOrNull(SysUserRoleMapper.class);
            SysFieldPermissionService fieldPermissionService = SpringContextHolder.getBeanOrNull(SysFieldPermissionService.class);
            if (userRoleMapper == null || fieldPermissionService == null) {
                return Map.of();
            }
            // 无登录态时抛 NotLoginException，由下面的 catch 统一降级
            Long userId = StpUtil.getLoginIdAsLong();
            long now = System.currentTimeMillis();
            Entry cached = CACHE.get(userId);
            if (cached != null && now - cached.loadedAt() < TTL_MS) {
                return cached.byTableField();
            }
            Map<String, SysFieldPermission> loaded = load(userRoleMapper, fieldPermissionService, userId);
            if (CACHE.size() > MAX_ENTRIES) {
                CACHE.clear();
            }
            CACHE.put(userId, new Entry(now, loaded));
            return loaded;
        } catch (Exception e) {
            log.debug("解析当前用户字段级权限失败，按无配置处理: {}", e.getMessage());
            return Map.of();
        }
    }

    /** 查库并整理成索引；同一字段被多个角色命中时取更严格的那条 */
    private static Map<String, SysFieldPermission> load(SysUserRoleMapper userRoleMapper,
                                                        SysFieldPermissionService fieldPermissionService,
                                                        Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        List<SysFieldPermission> list = fieldPermissionService.getByRoleIds(roleIds);
        if (list == null || list.isEmpty()) {
            return Map.of();
        }
        Map<String, SysFieldPermission> map = new HashMap<>();
        for (SysFieldPermission p : list) {
            if (p.getTargetTable() == null || p.getTargetField() == null) {
                continue;
            }
            map.merge(p.getTargetTable() + "." + p.getTargetField(), p, FieldPermissionCache::stricter);
        }
        return map;
    }

    /** 两条配置取更严格的：隐藏 > 打码 > 仅可见 */
    private static SysFieldPermission stricter(SysFieldPermission a, SysFieldPermission b) {
        return strictness(a) >= strictness(b) ? a : b;
    }

    private static int strictness(SysFieldPermission p) {
        if (p.getVisible() != null && p.getVisible() == 0) {
            return 3;
        }
        String maskType = p.getMaskType();
        if (maskType != null && !maskType.isEmpty() && !"NONE".equalsIgnoreCase(maskType)) {
            return 2;
        }
        return 1;
    }

    /** 清空缓存（权限配置变更后如需即时生效可调用） */
    static void clear() {
        CACHE.clear();
    }
}
