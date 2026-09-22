package cn.aiedge.base.security;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 「权限码 → 菜单码」派生器（平台-AUTHZ-01，2026-09-22）。
 *
 * <p><b>要解决的问题：</b>{@code sys_role_menu} 全库只有 3 行（只属 SUPER_ADMIN），
 * 非超管用户的菜单接口一律返回空数组。用户 2026-09-21 拍板：菜单可见性**从
 * {@code sys_permission} 派生**，不再双维护 {@code sys_role_menu} / {@code sys_tenant_menu}。
 *
 * <p><b>命中规则（前缀匹配）：</b>菜单 {@code menu_code = C} 被认为「有对应权限码」，
 * 当且仅当权限码库里存在权限码 {@code P}，满足 {@code P = C} 或 {@code P} 以 {@code C + ":"} 开头。
 * 例：菜单 {@code finance:other-income-doc} ↔ 权限 {@code finance:other-income-doc:view}。
 *
 * <p><b>为什么用「前缀集合」而不是逐条 LIKE 匹配：</b>把每条权限码按 {@code ':'} 边界
 * 展开成它的所有前缀（含自身），得到集合 {@code U}。此时
 * 「存在某个权限码命中菜单码 C」等价于「{@code C ∈ U}」——
 * 于是每个菜单只需一次 O(1) 的集合查找，不依赖数据库 LIKE，也不会出现 N+1。
 * 例：{@code finance:other-income-doc:view} 展开为
 * {@code finance}、{@code finance:other-income-doc}、{@code finance:other-income-doc:view}；
 * 菜单码 {@code finance:other-income-doc} 落在 {@code U} 里 ⇒ 命中。
 *
 * <p><b>缓存：</b>全量权限码（约 1800 条）在启动后基本不变，用 60 秒 Caffeine 本地缓存，
 * 避免登录后的高频菜单接口每次都扫 {@code sys_permission}。用户自身的权限码不在这里缓存
 * （由 {@link UnifiedPermissionCacheService} 的 L1/L2 缓存负责，见调用方）。
 *
 * <p><b>失败方向：</b>权限码库读不到时返回空集 ⇒ 所有菜单都被判为「无对应权限码」⇒
 * 按过渡口径全部保持可见（fail-open）。菜单只是导航，真正的访问控制点是后端接口鉴权
 * （{@code @SaCheckPermission} + 模块 entitlement 门），因此这里宁可多显示、不可少显示。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuPermissionDeriver {

    private final SysPermissionService permissionService;

    /** 全量权限码前缀集合的本地缓存时长（秒）。权限码库变更频率极低，60 秒足够把高频调用摊薄。 */
    private static final long CACHE_TTL_SECONDS = 60;

    /** 缓存里最多放 1 个 key（"all"），容量给 2 只是留余量。 */
    private final Cache<String, Set<String>> allPrefixCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
            .maximumSize(2)
            .build();

    /**
     * 权限码库里**全部**有效权限码所覆盖的「菜单码前缀」集合（即上文的前缀集合 U）。
     *
     * <p>调用方用它判断「某个菜单码到底有没有对应的权限码」——
     * 这是过渡口径的分界线：不在 U 里的菜单码表示权限码库尚未覆盖，
     * 必须保持可见（否则权限码覆盖不足时导航会缩水）。
     */
    public Set<String> allMenuCodePrefixes() {
        try {
            return allPrefixCache.get("all", key -> {
                // 只取 permission_code 一列：1800 行左右、单列投影，代价远小于全字段查询。
                // ⚠️ sys_permission.status 的语义是 0=正常 / 1=禁用（见实体注释），与用户自身
                //    权限码查询 selectPermissionCodesByUserId 的口径保持一致，同样只取 status = 0。
                List<SysPermission> rows = permissionService.list(
                        new LambdaQueryWrapper<SysPermission>()
                                .select(SysPermission::getPermissionCode)
                                .eq(SysPermission::getStatus, 0));
                Set<String> prefixes = toMenuCodePrefixes(
                        rows == null ? Collections.emptyList()
                                : rows.stream().map(SysPermission::getPermissionCode).toList());
                log.info("[菜单派生] 权限码前缀集合已重建: 权限码行数={}, 前缀数={}",
                        rows == null ? 0 : rows.size(), prefixes.size());
                return prefixes;
            });
        } catch (Exception e) {
            // 读库失败 ⇒ 返回空集 ⇒ 所有菜单「无对应权限码」⇒ 全部保持可见（fail-open）
            log.error("[菜单派生] 读取全量权限码失败，本次按「权限码库为空」处理（菜单全部保持可见）", e);
            return Collections.emptySet();
        }
    }

    /**
     * 把一组权限码展开成它们覆盖的「菜单码前缀」集合。
     *
     * <p>返回集合同时充当两个角色：判断菜单是否「有对应权限码」，
     * 以及判断该用户是否「持有」该菜单对应的权限。
     *
     * @param permissionCodes 权限码，允许为空或含 null
     */
    public Set<String> coveredMenuCodePrefixes(Collection<String> permissionCodes) {
        return toMenuCodePrefixes(permissionCodes);
    }

    /**
     * 前缀展开：{@code a:b:c} → {@code a:b:c}、{@code a:b}、{@code a}。
     * 前缀按 {@code ':'} 边界切，不做字符级前缀（避免 {@code finance:period} 误命中 {@code finance:periodic}）。
     */
    static Set<String> toMenuCodePrefixes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> prefixes = new HashSet<>(codes.size() * 2);
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String current = code.trim();
            prefixes.add(current);
            int idx = current.lastIndexOf(':');
            while (idx > 0) {
                current = current.substring(0, idx);
                prefixes.add(current);
                idx = current.lastIndexOf(':');
            }
        }
        return prefixes;
    }
}
