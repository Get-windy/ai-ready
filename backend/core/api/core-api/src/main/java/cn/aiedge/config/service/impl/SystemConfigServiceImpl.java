package cn.aiedge.config.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.cache.service.CacheService;
import cn.aiedge.config.mapper.SysConfigMapper;
import cn.aiedge.config.model.ConfigChangeLog;
import cn.aiedge.config.model.SystemConfig;
import cn.aiedge.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务实现（<b>2026-09-18 由「JVM 内存假实现」改造为「真实读写 {@code sys_config}」</b>）
 *
 * <p>改造前的 6 个 P0（《设置模块/系统参数开发文档.md》§9.2 / §12）与本次修法对照：</p>
 * <ol>
 *   <li><b>整页数据来自 static final Map BUILTIN_CONFIGS（12 条）</b> → 删除内存 Map，
 *       读路径全部走 {@link SysConfigMapper}（真读 {@code sys_config}）。</li>
 *   <li><b>saveConfig 不落库</b>（只写 Redis）→ INSERT / UPDATE 真实落库，且返回值是
 *       **回读后的行**（带 id 与时间），前端保存后能立刻在列表里看到。</li>
 *   <li><b>新增配置必然「保存成功但列表不出现」</b> → 新键以 {@code tenant_id = 当前租户}
 *       落库，读路径包含 {@code tenant_id IN (0, 当前租户)} → 新增项立即可见。</li>
 *   <li><b>假分页</b>（pages 恒 1、pageNum/pageSize 被忽略）→ {@code LIMIT/OFFSET} + 独立
 *       COUNT 查询，`pageSize` 真正生效（控制器按 total 算 pages）。</li>
 *   <li><b>deleteConfigByKey 对内置信恒 false / batchDelete / deleteConfig / refreshCache 空实现</b>
 *       → 删除走真逻辑删除并**返回真实影响行数**；批量删除是**一条 SQL**（不再是循环单删）；
 *       refreshCache 真的按模式清理历史遗留缓存键并返回清理条数。</li>
 *   <li><b>内置配置从不 setId → id 全为 null</b> → 行来自 DB，id 由序列
 *       {@code sys_config_id_seq} 给出，行选择 / row-key 恢复正常。</li>
 * </ol>
 *
 * <p><b>保留的取舍</b>：内置配置（{@code builtin = true}）**仍不允许删除**（这是原设计的
 * 业务保护，不是缺陷），但拒绝是「真拒绝」：接口返回 {@code success: false}，
 * 非内置配置则**真的删掉**（旧实现是「内置恒 false、非内置恒 true」的假成功）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SysConfigMapper configMapper;

    /** Redis 缓存（可选注入：无 Redis 时相关能力降级为「不清理」而不报错） */
    @Autowired(required = false)
    private CacheService cacheService;

    /** 历史遗留的缓存键前缀（旧实现写的是 `sys:config:<tenantId>:<configKey>`） */
    private static final String CONFIG_KEY = "sys:config:";
    private static final String LOG_KEY = "sys:config:log:";

    /** 新配置项默认落到「其他」视图（左列 8 视图中的兜底视图，保证新建后立刻可见） */
    private static final String DEFAULT_NAV_GROUP = "other";

    /** 新配置项默认排序号（排在 seed 数据 10~70 之后） */
    private static final int DEFAULT_SORT_ORDER = 999;

    /**
     * 「被商品引用后不能更改」的配置键 ↔ {@code SysConfigMapper#countProductReferences} 的别名。
     *
     * <p>ql361 的原文语义是「此配置被商品启用后不能更改，请慎重选择」（`行业设置` 卡片里
     * 「保质期管理 / 批号管理」的灰色温馨提示）。**这不是编造的链路**：四个键都对应本系统
     * 商品域里真实存在的表/列：</p>
     * <ul>
     *   <li>{@code industry.batch.shelfLife} ← {@code erp_product.is_batch_expiry_managed}
     *       —— 完全落地：商品表单「商品特性（勾选即启用）」里的「保质期/批次号」勾选框
     *       （`erp/product/form.vue`），当前库里有 1 个商品勾了它 → 该配置**真的被锁**。</li>
     *   <li>{@code industry.batch.batchNo} ← {@code erp_product.is_batch_managed}、
     *       {@code industry.serial.enabled} ← {@code erp_product.is_serial_managed}
     *       —— 这两个列在 {@code Product} 实体与前端 {@code erp/product.ts} 的类型里都有，
     *       由商品保存链路统一落库；但商品表单**尚未暴露这两个勾选框**（目前恒为 0）→
     *       现在不会触发锁定，一旦商品侧启用即自动生效。</li>
     *   <li>{@code industry.product.specAttr} ← {@code erp_product_attribute_def} 有行
     *       —— 商品规格属性定义表有独立的实体与控制器（{@code ProductAttributeController}）。</li>
     * </ul>
     *
     * <p>只有这 4 个键参与锁定；其余配置项（配送方式、单据设置、数据权限…）在本系统与
     * ql361 里都没有「引用后不可改」的语义 → 保持 `locked = false`。</p>
     */
    private static final Map<String, String> REFERENCE_LOCKED_KEYS = Map.of(
            "industry.product.specAttr", "specAttr",
            "industry.batch.shelfLife", "batchExpiry",
            "industry.batch.batchNo", "batchNo",
            "industry.serial.enabled", "serial");

    // ────────────────────────── 读 ──────────────────────────

    @Override
    public SystemConfig getConfig(Long id) {
        if (id == null) {
            return null;
        }
        // 改造前这里是 `return null; // 简化实现`
        return configMapper.selectByIdAnyTenant(id);
    }

    @Override
    public SystemConfig getConfigByKey(String configKey, Long tenantId) {
        if (!StringUtils.hasText(configKey)) {
            return null;
        }
        return configMapper.selectByKey(effectiveTenant(tenantId), configKey);
    }

    @Override
    public String getConfigValue(String configKey, Long tenantId) {
        SystemConfig config = getConfigByKey(configKey, tenantId);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public <T> T getConfigValue(String configKey, Class<T> clazz, T defaultValue, Long tenantId) {
        String value = getConfigValue(configKey, tenantId);
        if (value == null) {
            return defaultValue;
        }
        try {
            if (clazz == String.class) {
                return (T) value;
            }
            if (clazz == Integer.class || clazz == int.class) {
                return (T) Integer.valueOf(value);
            }
            if (clazz == Long.class || clazz == long.class) {
                return (T) Long.valueOf(value);
            }
            if (clazz == Boolean.class || clazz == boolean.class) {
                return (T) Boolean.valueOf(value);
            }
            if (clazz == Double.class || clazz == double.class) {
                return (T) Double.valueOf(value);
            }
        } catch (Exception e) {
            log.warn("配置值转换失败: key={}, value={}", configKey, value);
        }
        return defaultValue;
    }

    @Override
    public List<SystemConfig> getConfigList(String configType, String configGroup, Long tenantId) {
        List<SystemConfig> rows = configMapper.selectConfigs(effectiveTenant(tenantId), blankToNull(configType),
                blankToNull(configGroup), null, null, null, null, null, null, null);
        applyReferenceLocks(rows);
        return rows;
    }

    @Override
    public List<SystemConfig> getConfigList(SystemConfig query, Long tenantId) {
        SystemConfig q = normalizeQuery(query);
        List<SystemConfig> rows = configMapper.selectConfigs(effectiveTenant(tenantId), q.getConfigType(),
                q.getConfigGroup(), q.getNavGroup(), q.getConfigKey(), q.getConfigName(), q.getEnabled(),
                q.getSystemConfig(), null, null);
        applyReferenceLocks(rows);
        return rows;
    }

    @Override
    public List<SystemConfig> getConfigPage(SystemConfig query, int pageNum, int pageSize, Long tenantId) {
        SystemConfig q = normalizeQuery(query);
        if (pageSize <= 0) {
            // 不分页：返回全部
            return getConfigList(q, tenantId);
        }
        int safePageNum = Math.max(pageNum, 1);
        // 每页上限 200，避免一次拉全表（配置项总量很小，正常远达不到）
        int safePageSize = Math.min(pageSize, 200);
        int offset = (safePageNum - 1) * safePageSize;
        List<SystemConfig> rows = configMapper.selectConfigs(effectiveTenant(tenantId), q.getConfigType(),
                q.getConfigGroup(), q.getNavGroup(), q.getConfigKey(), q.getConfigName(), q.getEnabled(),
                q.getSystemConfig(), safePageSize, offset);
        applyReferenceLocks(rows);
        return rows;
    }

    @Override
    public long getConfigCount(SystemConfig query, Long tenantId) {
        SystemConfig q = normalizeQuery(query);
        return configMapper.countConfigs(effectiveTenant(tenantId), q.getConfigType(), q.getConfigGroup(),
                q.getNavGroup(), q.getConfigKey(), q.getConfigName(), q.getEnabled(), q.getSystemConfig());
    }

    @Override
    public Map<String, String> getConfigMap(String configGroup, Long tenantId) {
        List<SystemConfig> configs = getConfigList(null, configGroup, tenantId);
        // 保持稳定顺序（LinkedHashMap），便于接口对账与调试
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    // ────────────────────────── 写 ──────────────────────────

    @Override
    public SystemConfig saveConfig(SystemConfig config, Long tenantId) {
        if (config == null || !StringUtils.hasText(config.getConfigKey())) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        Long tenant = effectiveTenant(tenantId);
        String configKey = config.getConfigKey().trim();
        // 只在「真的要改值」时拦截（未提交值的调用方不应被拒绝）
        if (config.getConfigValue() != null) {
            ensureNotReferenceLocked(configKey);
        }

        SystemConfig existing = configMapper.selectByKey(tenant, configKey);
        if (existing == null) {
            return insertNew(config, configKey, tenant);
        }

        // 先读后写：只覆盖调用方**显式提供**的字段，其余保留原值。
        // 必要性：本表有多个调用方（系统参数页按值批量保存、平台「系统配置」页按完整表单保存、
        // 支付配置/供应商页按键保存），若整体覆盖，未提交的 nav_group / parent_key /
        // help_text / tip_text 等列会被冲成 null，配置项会从对应视图里「消失」。
        mergeProvidedFields(config, existing);
        configMapper.updateConfig(existing);
        return configMapper.selectByIdAnyTenant(existing.getId());
    }

    @Override
    public void saveConfigValue(String configKey, String configValue, Long tenantId) {
        if (!StringUtils.hasText(configKey)) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        String key = configKey.trim();
        ensureNotReferenceLocked(key);
        doSaveConfigValue(key, configValue, tenantId);
    }

    /** 真正落库的保存逻辑（锁定校验由调用方按批/按次先行完成，避免逐键重复查询） */
    private void doSaveConfigValue(String key, String configValue, Long tenantId) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        Long tenant = effectiveTenant(tenantId);

        SystemConfig existing = configMapper.selectByKey(tenant, key);
        String oldValue = existing == null ? null : existing.getConfigValue();

        SystemConfig saved;
        if (existing == null) {
            SystemConfig fresh = new SystemConfig();
            fresh.setConfigKey(key);
            fresh.setConfigValue(configValue);
            saved = insertNew(fresh, key, tenant);
        } else {
            existing.setConfigValue(configValue);
            configMapper.updateConfig(existing);
            saved = configMapper.selectByIdAnyTenant(existing.getId());
        }

        log.info("保存配置值: key={}, tenantId={}", key, tenant);
        logConfigChange(saved, oldValue, existing == null ? "create" : "update", null, null, null, tenant);
    }

    @Override
    public void batchSaveConfigs(Map<String, String> configs, Long tenantId) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        // 锁定校验：整批只查一次商品引用计数（逐键查会让「保存一个视图」变成几十次 COUNT）。
        // 被商品引用的不可逆配置**跳过并记日志**，不让整批保存失败（页面上这些项已渲染为禁用，
        // 正常不会提交到；跳过是为了兜底其它调用方）。
        Map<String, Object> counts = referenceCounts();
        int skipped = 0;
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey() == null ? null : entry.getKey().trim();
            if (isReferenceLocked(key, counts)) {
                skipped++;
                log.warn("批量保存跳过「已被商品引用」的配置: key={}", key);
                continue;
            }
            doSaveConfigValue(key, entry.getValue(), tenantId);
        }
        log.info("批量保存配置: count={}, skipped={}, tenantId={}", configs.size(), skipped, tenantId);
    }

    // ────────────────────────── 删 ──────────────────────────

    @Override
    public boolean deleteConfig(Long id, Long tenantId) {
        if (id == null) {
            return false;
        }
        SystemConfig row = configMapper.selectByIdAnyTenant(id);
        if (row == null) {
            log.warn("删除配置失败：记录不存在, id={}", id);
            return false;
        }
        if (Boolean.TRUE.equals(row.getSystemConfig())) {
            log.warn("不能删除内置配置: id={}, key={}", id, row.getConfigKey());
            return false;
        }
        // 真实影响行数：> 0 才算删掉了（旧实现直接 `return true`）
        return configMapper.logicDeleteById(id) > 0;
    }

    @Override
    public boolean deleteConfigByKey(String configKey, Long tenantId) {
        if (!StringUtils.hasText(configKey)) {
            return false;
        }
        SystemConfig row = configMapper.selectByKey(effectiveTenant(tenantId), configKey.trim());
        if (row == null) {
            // 不存在 → 没有删除任何行，如实返回 false（旧实现恒 true 是假成功）
            log.warn("删除配置失败：配置键不存在, key={}", configKey);
            return false;
        }
        if (Boolean.TRUE.equals(row.getSystemConfig())) {
            log.warn("不能删除内置配置: {}", configKey);
            return false;
        }
        boolean removed = configMapper.logicDeleteById(row.getId()) > 0;
        if (removed && cacheService != null) {
            try {
                cacheService.deleteByPattern(CONFIG_KEY + "[0-9]*:" + configKey);
            } catch (Exception e) {
                log.warn("清理配置缓存失败: key={}", configKey, e);
            }
        }
        return removed;
    }

    @Override
    public boolean batchDelete(List<Long> ids, Long tenantId) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 内置配置剔除后**一条 SQL** 真删（不再逐条循环单删，也不再「空实现 + return true」）
        List<Long> deletable = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            SystemConfig row = configMapper.selectByIdAnyTenant(id);
            if (row == null) {
                continue;
            }
            if (Boolean.TRUE.equals(row.getSystemConfig())) {
                log.warn("批量删除跳过内置配置: id={}, key={}", id, row.getConfigKey());
                continue;
            }
            deletable.add(id);
        }
        if (deletable.isEmpty()) {
            return false;
        }
        int removed = configMapper.logicDeleteByIds(deletable);
        log.info("批量删除配置: 请求 {} 条, 实际删除 {} 条", ids.size(), removed);
        // 只有「请求的每一条都真的删掉了」才返回 true，避免再次出现「提示成功但没删」
        return removed == ids.size();
    }

    // ────────────────────────── 变更日志（历史能力，如实保留） ──────────────────────────

    @Override
    public List<ConfigChangeLog> getConfigChangeLogs(String configKey, Long tenantId) {
        // ⚠️ 缺口（文档 §12-⑲）：日志写在 Redis list（logConfigChange），无 DB 表、无写入方覆盖全部写路径，
        //    因此这里通常为空表。本次改造未新增日志表（不发明文档没有的表）。
        if (cacheService != null) {
            try {
                String key = LOG_KEY + tenantId + ":" + configKey;
                List<Object> logs = cacheService.lRange(key, 0, 100);
                List<ConfigChangeLog> result = new ArrayList<>();
                if (logs != null) {
                    for (Object obj : logs) {
                        if (obj instanceof ConfigChangeLog) {
                            result.add((ConfigChangeLog) obj);
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                log.warn("读取配置变更日志失败: key={}", configKey, e);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void logConfigChange(SystemConfig config, String oldValue, String changeType,
                                Long operatorId, String operatorName, String reason, Long tenantId) {
        ConfigChangeLog logEntry = new ConfigChangeLog();
        logEntry.setConfigId(config.getId());
        logEntry.setConfigKey(config.getConfigKey());
        logEntry.setOldValue(oldValue);
        logEntry.setNewValue(config.getConfigValue());
        logEntry.setChangeType(changeType);
        logEntry.setChangeReason(reason);
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorName);
        logEntry.setOperateTime(LocalDateTime.now());
        logEntry.setTenantId(tenantId);

        if (cacheService != null) {
            try {
                String key = LOG_KEY + tenantId + ":" + config.getConfigKey();
                cacheService.lPush(key, logEntry);
                cacheService.expire(key, 365, TimeUnit.DAYS);
            } catch (Exception e) {
                log.warn("写入配置变更日志失败: key={}", config.getConfigKey(), e);
            }
        }
    }

    // ────────────────────────── 缓存 ──────────────────────────

    @Override
    public long refreshCache(Long tenantId) {
        // 读路径已改为直读 DB（无值缓存），此处的真实作用是**清理旧版本写下的遗留缓存键**
        // （键形如 `sys:config:<tenantId>:<configKey>`）。模式 `sys:config:[0-9]*` 只匹配这类键，
        // 不会误删 `sys:config:log:*`（变更日志）。
        long cleared = deleteCacheByPattern(CONFIG_KEY + "[0-9]*");
        log.info("刷新配置缓存: tenantId={}, 清理历史缓存键 {} 个", tenantId, cleared);
        return cleared;
    }

    @Override
    public long refreshCache(String configKey, Long tenantId) {
        if (!StringUtils.hasText(configKey)) {
            return refreshCache(tenantId);
        }
        long cleared = deleteCacheByPattern(CONFIG_KEY + "[0-9]*:" + configKey.trim());
        log.info("刷新配置缓存: key={}, 清理历史缓存键 {} 个", configKey, cleared);
        return cleared;
    }

    private long deleteCacheByPattern(String pattern) {
        if (cacheService == null) {
            return 0L;
        }
        try {
            Long deleted = cacheService.deleteByPattern(pattern);
            return deleted == null ? 0L : deleted;
        } catch (Exception e) {
            // Redis 不可用时不影响接口可用性（配置读写不依赖缓存）
            log.warn("按模式清理缓存失败: pattern={}", pattern, e);
            return 0L;
        }
    }

    // ────────────────────────── 「被商品引用后不可改」的锁定判定 ──────────────────────────

    /**
     * 把「商品引用情况」折算成 {@code locked} + {@code lockedReason}（读取时实时算，不落库）。
     *
     * <p>为什么在读取时算而不是在写时置位：引用是**双向**的 —— 商品勾上「保质期/批次号」要立刻
     * 锁住配置，商品取消勾选又应解锁；把状态写进 `sys_config.locked` 需要商品侧反向回调，
     * 容易漏。读时按真实引用计数折算，任何时刻都与商品档案一致（单表 COUNT，配置项总量很小）。</p>
     *
     * <p>只有 {@link #REFERENCE_LOCKED_KEYS} 登记过的键会被改写；其余行保持库里的值。</p>
     */
    private void applyReferenceLocks(List<SystemConfig> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        boolean relevant = false;
        for (SystemConfig row : rows) {
            if (row != null && REFERENCE_LOCKED_KEYS.containsKey(row.getConfigKey())) {
                relevant = true;
                break;
            }
        }
        if (!relevant) {
            return;
        }
        Map<String, Object> counts = referenceCounts();
        for (SystemConfig row : rows) {
            if (row == null) {
                continue;
            }
            String refKind = REFERENCE_LOCKED_KEYS.get(row.getConfigKey());
            if (refKind == null) {
                continue;
            }
            long used = asLong(counts.get(refKind));
            if (used > 0) {
                row.setLocked(Boolean.TRUE);
                row.setLockedReason("已被 " + used + " 个商品启用：此配置被商品启用后不能更改，请慎重选择");
            }
        }
    }

    /** 引用计数（查询失败时按「无引用」处理，不影响页面可用性） */
    private Map<String, Object> referenceCounts() {
        try {
            Map<String, Object> counts = configMapper.countProductReferences();
            return counts == null ? Map.of() : counts;
        } catch (Exception e) {
            log.warn("统计商品引用失败（locked 按未锁定处理）", e);
            return Map.of();
        }
    }

    private static long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * 写入前拦截「已被商品引用」的不可逆配置（ql361 语义：被商品启用后不能更改）。
     *
     * <p>页面已把锁定项渲染为禁用并排除在提交之外，这里是**服务端兜底**：
     * 其它入口（平台「系统配置」页、支付配置等）若试图改这 4 个键，会被明确拒绝而不是静默写入。</p>
     */
    private void ensureNotReferenceLocked(String configKey) {
        String refKind = REFERENCE_LOCKED_KEYS.get(configKey);
        if (refKind == null) {
            return;
        }
        long used = asLong(referenceCounts().get(refKind));
        if (used > 0) {
            throw new IllegalStateException("配置[" + configKey + "]已被 " + used
                    + " 个商品启用，启用后不能更改，请慎重选择");
        }
    }

    /** 按已取回的引用计数判断某个键是否被锁定（批量保存复用同一份计数） */
    private static boolean isReferenceLocked(String configKey, Map<String, Object> counts) {
        String refKind = configKey == null ? null : REFERENCE_LOCKED_KEYS.get(configKey);
        return refKind != null && asLong(counts.get(refKind)) > 0;
    }

    // ────────────────────────── 内部工具 ──────────────────────────

    /**
     * 当前生效租户：优先用调用方传入的（{@code X-Tenant-Id} 头），为空时回退会话租户。
     *
     * <p>为 null 表示「无会话租户」（平台超管等），此时 {@link SysConfigMapper} 的查询不加租户条件。</p>
     */
    private Long effectiveTenant(Long tenantId) {
        Long sessionTenantId = MyBatisPlusConfig.getCurrentTenantIdValue();

        // 平台超管（租户隔离豁免）：允许用调用方传入的租户（含 0 = 全局默认行）
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return tenantId != null ? tenantId : sessionTenantId;
        }

        // 非超管：一律强制会话租户，传入的 tenantId（来自 X-Tenant-Id 头，客户端可随意伪造）一律忽略。
        // 2026-09-20 加固：此前 tenantId 优先，任何持有 system:config:update 的租户管理员只要把
        // 请求头改成 0（或他人租户），就能读写全系统生效的全局配置 —— 属跨层越权。
        if (sessionTenantId == null) {
            throw new IllegalStateException("无法解析当前会话租户，请重新登录后再试");
        }
        if (tenantId != null && !tenantId.equals(sessionTenantId)) {
            log.warn("系统参数：忽略越权的 tenantId 参数，requested={}, session={}", tenantId, sessionTenantId);
        }
        return sessionTenantId;
    }

    /** 空串（含纯空白）归一为 null —— Mapper 的过滤口径是「null = 不限」，非 null 才拼条件 */
    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 把查询载体里的字符串条件统一归一（避免空串被当成过滤值，把列表查成 0 行） */
    private static SystemConfig normalizeQuery(SystemConfig query) {
        SystemConfig q = query == null ? new SystemConfig() : query;
        q.setConfigType(blankToNull(q.getConfigType()));
        q.setConfigGroup(blankToNull(q.getConfigGroup()));
        q.setNavGroup(blankToNull(q.getNavGroup()));
        q.setConfigKey(blankToNull(q.getConfigKey()));
        q.setConfigName(blankToNull(q.getConfigName()));
        return q;
    }

    /** 新增一行（显式从序列取号，避免依赖驱动回填自增主键） */
    private SystemConfig insertNew(SystemConfig source, String configKey, Long tenant) {
        SystemConfig row = new SystemConfig();
        row.setId(configMapper.selectNextId());
        row.setConfigKey(configKey);
        row.setConfigName(StringUtils.hasText(source.getConfigName()) ? source.getConfigName() : configKey);
        row.setConfigValue(source.getConfigValue());
        row.setConfigType(source.getConfigType());
        row.setConfigGroup(source.getConfigGroup());
        // 新配置项必须落在某个左标签视图里，否则本页看不到它
        row.setNavGroup(StringUtils.hasText(source.getNavGroup()) ? source.getNavGroup() : DEFAULT_NAV_GROUP);
        row.setParentKey(source.getParentKey());
        row.setValueType(StringUtils.hasText(source.getValueType()) ? source.getValueType() : "string");
        // 新建的一律不是内置（内置只来自迁移 seed）
        row.setSystemConfig(false);
        row.setDescription(source.getDescription());
        row.setHelpText(source.getHelpText());
        row.setTipText(source.getTipText());
        row.setSortOrder(source.getSortOrder() != null ? source.getSortOrder() : DEFAULT_SORT_ORDER);
        row.setEnabled(source.getEnabled() != null ? source.getEnabled() : Boolean.TRUE);
        row.setLocked(source.getLocked() != null ? source.getLocked() : Boolean.FALSE);
        row.setTenantId(tenant != null ? tenant : 0L);

        configMapper.insertConfig(row);
        log.info("新增配置: key={}, id={}, tenantId={}", configKey, row.getId(), row.getTenantId());
        return configMapper.selectByIdAnyTenant(row.getId());
    }

    /**
     * 把调用方显式提供的字段合并到已存在行上（null 一律视为「本次不修改」）。
     *
     * <p>注意：字符串只判 {@code != null} 而不判空白 —— 允许把某个字段显式改成空串（清空）。</p>
     */
    private void mergeProvidedFields(SystemConfig source, SystemConfig target) {
        if (source.getConfigName() != null) {
            target.setConfigName(source.getConfigName());
        }
        if (source.getConfigValue() != null) {
            target.setConfigValue(source.getConfigValue());
        }
        if (source.getConfigType() != null) {
            target.setConfigType(source.getConfigType());
        }
        if (source.getConfigGroup() != null) {
            target.setConfigGroup(source.getConfigGroup());
        }
        if (source.getNavGroup() != null) {
            target.setNavGroup(source.getNavGroup());
        }
        if (source.getParentKey() != null) {
            target.setParentKey(source.getParentKey());
        }
        if (source.getValueType() != null) {
            target.setValueType(source.getValueType());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getHelpText() != null) {
            target.setHelpText(source.getHelpText());
        }
        if (source.getTipText() != null) {
            target.setTipText(source.getTipText());
        }
        if (source.getSortOrder() != null) {
            target.setSortOrder(source.getSortOrder());
        }
        if (source.getEnabled() != null) {
            target.setEnabled(source.getEnabled());
        }
        if (source.getLocked() != null) {
            target.setLocked(source.getLocked());
        }
        // 已存在行按 id 定位更新，不接受调用方改 id / key / 内置标记 / 租户归属
    }
}
