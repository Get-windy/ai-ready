package cn.aiedge.base.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import cn.aiedge.base.service.SysConfigService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务实现。
 *
 * <p><b>2026-09-21 租户专项（消灭硬编码租户）</b>：原实现顶部写着</p>
 * <pre>
 *   // 当前租户ID（简化实现）
 *   private static final Long CURRENT_TENANT_ID = 1L;
 * </pre>
 * <p>这个「简化实现」把 <b>租户 1</b> 当成了所有租户的配置来源：{@code getValue / getConfig /
 * getConfigsByGroup / getAllConfigs} 一律读租户 1，写也一律写进租户 1。后果是**跨租户串数据**：</p>
 * <ul>
 *   <li>{@code inventory.mode}（库存管理模式，注释自称「支持租户级配置」）——租户 2 读到的是租户 1 的模式；</li>
 *   <li>{@code expense.approval.level1~3.approverId}（费用审批默认审批人）——**租户 2 的单据会派给租户 1 的人**，
 *       而租户 2 管理员一保存，又把租户 1 的配置覆盖掉；</li>
 *   <li>{@code mall.product.default.sort}（商城商品默认排序）、{@code stock.alert.comparison}
 *       （库存预警口径）、{@code marketing.autoCampaign.globalFreq*}（全域频控）同理。</li>
 * </ul>
 *
 * <p><b>现在的口径</b>（三句话，别记混）：</p>
 * <ol>
 *   <li><b>会话租户</b>（{@code MyBatisPlusConfig.getCurrentTenantIdValue()}：临时租户 → Sa-Token 会话）
 *       是「本租户自己的配置」，读写都以它为准；</li>
 *   <li><b>平台行</b>（{@link #PLATFORM_TENANT_ID} = 0）是全站默认值，**只在读路径且本租户没配过时**回落；</li>
 *   <li>两者都拿不到「某个具体租户」—— <b>任何情况下都不猜租户</b>。原实现猜的是 1，这就是本专项要根除的东西。</li>
 * </ol>
 *
 * <p><b>为什么写路径坚决不回落</b>：若用「本租户没有就取平台行」的查询去找"已存在行"，
 * 那么本租户没配过时会把**平台那一行**当成自己的并更新它 ⇒ 租户保存自己的配置 = 覆盖全站默认值。
 * 所以写路径（{@link #setValue} / {@link #deleteConfig}）一律用 {@code selectRowByTenant}（严格本租户），
 * 而展示给管理员的 {@link #getConfig(String)} 也沿用严格语义（"没配过"就该是空的，不该显示别人的行）。</p>
 *
 * <p><b>缓存</b>：{@code configCache} 的键从「配置键」改为「租户ID + '\0' + 配置键」。
 * 原缓存只有配置键一个维度，一旦多租户真的用起来，租户 A 写入的值会直接被租户 B 读到。</p>
 *
 * <p><b>已知取舍（不是本专项引入的，但要知道）</b>：读路径回落平台行后，缓存里存的是
 * 「(本租户, 键) → 平台值」这个快照；此后平台行被改动，已缓存的租户不会自动感知，
 * 需 {@link #refreshCache()} / {@link #refreshConfig(String)}（与改动前的刷新语义一致）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysProjectConfigMapper configMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 配置缓存，键 = {@code 租户ID + '\0' + 配置键}。
     *
     * <p>用 '\0' 作分隔符是因为配置键里本身就会出现 {@code .} 和 {@code :}
     * （{@code set:menu-config:hidden}、{@code user_page_config:col-config:xxx:1}），
     * 用这两个字符做分隔会撞键。'\0' 不可能出现在配置键里。</p>
     */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /** 配置历史表名（Redis List 前缀，后面还会拼租户与键） */
    private static final String CONFIG_HISTORY_KEY = "sys:config:history:";
    private static final String CONFIG_CHANGE_CHANNEL = "sys:config:change";

    /**
     * 平台默认行的租户 ID（全站默认值），与 {@code PaymentConfigServiceImpl.PLATFORM_TENANT_ID} 同一约定。
     *
     * <p>注意它**不是** {@code sys_tenant} 里那个 id=1 的「系统租户」：系统租户是「平台自己作为
     * 一家租户」，而这里是「不属于任何租户的全局默认行」。两者混用正是原实现 bug 的来源。</p>
     */
    private static final Long PLATFORM_TENANT_ID = 0L;

    // ═══════════════════════════════════════════════════════════════════
    // 租户解析
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 读写作用域租户：会话租户 → 拿不到时用平台行（0）。
     *
     * <p>为什么拿不到时不返回 null / 不抛异常：本服务会被无 HTTP 会话的路径调用
     * （启动期、定时任务、登录链路）。返回 null 会让每个调用方都要各自处理空值，
     * 而「返回平台行」是一个**确定且不会泄漏任何租户私有数据**的答案。
     * 唯一不能做的是猜一个具体租户（如常量 1）—— 那正是原实现的 bug。</p>
     */
    private static Long scopedTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return tenantId != null ? tenantId : PLATFORM_TENANT_ID;
    }

    private static String cacheKey(Long tenantId, String key) {
        return tenantId + "\u0000" + key;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 配置查询
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public Optional<String> getValue(String key) {
        Long tenantId = scopedTenantId();
        String cacheK = cacheKey(tenantId, key);

        String cachedValue = configCache.get(cacheK);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }

        // 本租户优先；本租户没配过才回落平台默认行（**不是**回落到别的租户）
        String value = configMapper.getConfigValue(tenantId, key);
        if (value == null && !PLATFORM_TENANT_ID.equals(tenantId)) {
            value = configMapper.getConfigValue(PLATFORM_TENANT_ID, key);
        }
        if (value != null) {
            configCache.put(cacheK, value);
            return Optional.of(value);
        }

        return Optional.empty();
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    @Override
    public Optional<SysProjectConfig> getConfig(String key) {
        // 严格本租户（不回落平台行）：本方法服务于配置管理页与写路径的"行已存在吗"判断，
        // 回落会让租户看到一个不属于自己的行、进而更新到它。
        return Optional.ofNullable(configMapper.selectRowByTenant(scopedTenantId(), key));
    }

    @Override
    public List<SysProjectConfig> getConfigsByGroup(String group) {
        return configMapper.selectByGroup(scopedTenantId(), group);
    }

    @Override
    public List<SysProjectConfig> getAllConfigs() {
        return configMapper.selectAllConfigs(scopedTenantId());
    }

    @Override
    public Map<String, String> getConfigMap() {
        Map<String, String> map = new HashMap<>();
        List<SysProjectConfig> configs = getAllConfigs();
        for (SysProjectConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 配置更新
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setValue(String key, String value) {
        setValue(key, value, "string", "default", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setValue(String key, String value, String type, String group, String description) {
        Long tenantId = scopedTenantId();
        // ⚠️ 严格本租户取"已存在行"（不能用 getConfig 的回落语义）：否则本租户没配过时会去更新平台行
        SysProjectConfig existing = configMapper.selectRowByTenant(tenantId, key);

        if (existing != null) {
            // 保存历史记录（历史键含租户，否则租户 A 的回滚会把租户 B 的旧值写回来）
            saveHistory(tenantId, key, existing.getConfigValue(), value);

            // 更新配置；命中行原本可能被停用（status != 0）而读不到，保存时一并恢复为生效
            LambdaUpdateWrapper<SysProjectConfig> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SysProjectConfig::getId, existing.getId())
                   .set(SysProjectConfig::getConfigValue, value)
                   .set(SysProjectConfig::getStatus, 0)
                   .set(SysProjectConfig::getUpdateTime, LocalDateTime.now());
            configMapper.update(null, wrapper);
        } else {
            // 新增配置（租户 = 当前作用域租户，绝不写进"某个固定租户"）
            SysProjectConfig config = new SysProjectConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType(type);
            config.setConfigGroup(group);
            config.setDescription(description);
            config.setStatus(0);
            // deleted 列 NOT NULL：显式给 0，不依赖逻辑删除插件在 insert 时补默认值
            config.setDeleted(0);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        }

        // 更新缓存
        configCache.put(cacheKey(tenantId, key), value);

        // 发布变更事件
        publishConfigChange(key, value);

        log.info("配置已更新: tenant={}, {} = {}", tenantId, key, value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setValues(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String key) {
        Long tenantId = scopedTenantId();
        // 严格本租户：否则租户会话会把平台默认行（或别的行）逻辑删除掉
        SysProjectConfig config = configMapper.selectRowByTenant(tenantId, key);
        if (config != null) {
            // 保存历史记录
            saveHistory(tenantId, key, config.getConfigValue(), null);

            // 删除配置
            LambdaUpdateWrapper<SysProjectConfig> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SysProjectConfig::getId, config.getId())
                   .set(SysProjectConfig::getDeleted, 1)
                   .set(SysProjectConfig::getUpdateTime, LocalDateTime.now());
            configMapper.update(null, wrapper);

            // 清除缓存（含租户维度：删本租户的键，不能顺手清掉别的租户的缓存）
            configCache.remove(cacheKey(tenantId, key));

            log.info("配置已删除: tenant={}, {}", tenantId, key);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 配置热更新
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void refreshCache() {
        Long tenantId = scopedTenantId();
        String prefix = tenantId + "\u0000";

        // 只清本租户的缓存条目：原实现 clear() 会把别的租户的缓存一起清掉，
        // 而紧接着又只按租户 1 重新装载 —— 清得比装得多的那种"刷新"。
        configCache.keySet().removeIf(k -> k.startsWith(prefix));

        List<SysProjectConfig> configs = configMapper.selectAllConfigs(tenantId);
        for (SysProjectConfig config : configs) {
            if (config.getConfigValue() != null) {
                configCache.put(cacheKey(tenantId, config.getConfigKey()), config.getConfigValue());
            }
        }
        log.info("配置缓存已刷新: tenant={}, 本租户{}项", tenantId, configs.size());
    }

    @Override
    public void refreshConfig(String key) {
        Long tenantId = scopedTenantId();
        String cacheK = cacheKey(tenantId, key);

        // 与 getValue 同一套解析口径（本租户 → 平台行），否则"刷新"会把回落值刷没了
        String value = configMapper.getConfigValue(tenantId, key);
        if (value == null && !PLATFORM_TENANT_ID.equals(tenantId)) {
            value = configMapper.getConfigValue(PLATFORM_TENANT_ID, key);
        }
        if (value != null) {
            configCache.put(cacheK, value);
        } else {
            configCache.remove(cacheK);
        }
        log.info("配置已刷新: tenant={}, {}", tenantId, key);
    }

    @Override
    public void publishConfigChange(String key, String value) {
        try {
            Map<String, String> message = new HashMap<>();
            // 带上租户：订阅方（ConfigChangeListener 等）才能按租户落缓存，
            // 否则与本次修复前一样，只是个"键 → 值"、没有归属的广播
            message.put("tenantId", String.valueOf(scopedTenantId()));
            message.put("key", key);
            message.put("value", value);
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));

            redisTemplate.convertAndSend(CONFIG_CHANGE_CHANNEL, JSONUtil.toJsonStr(message));
            log.debug("配置变更已发布: {}", key);
        } catch (Exception e) {
            log.warn("发布配置变更失败: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 配置版本管理
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public List<ConfigHistory> getConfigHistory(String key) {
        String historyKey = historyKey(scopedTenantId(), key);
        List<String> historyList = redisTemplate.opsForList().range(historyKey, 0, 99);

        List<ConfigHistory> result = new ArrayList<>();
        if (historyList != null) {
            for (String json : historyList) {
                Map<String, Object> map = JSONUtil.toBean(json, Map.class);
                result.add(new ConfigHistory(
                    Long.valueOf(map.get("id").toString()),
                    key,
                    (String) map.get("oldValue"),
                    (String) map.get("newValue"),
                    (String) map.get("changedBy"),
                    LocalDateTime.parse((String) map.get("changedAt")),
                    (String) map.get("changeReason")
                ));
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackConfig(String key, Long version) {
        List<ConfigHistory> history = getConfigHistory(key);

        for (ConfigHistory h : history) {
            if (h.id().equals(version)) {
                // setValue 写的是**当前会话租户**，而 getConfigHistory 读的也是当前会话租户，
                // 两侧同源 ⇒ 不会把别的租户的历史版本写进本租户。
                setValue(key, h.oldValue());
                log.info("配置已回滚: {}", key);
                return;
            }
        }

        throw new RuntimeException("未找到配置历史版本: " + version);
    }

    @Override
    public ConfigDiff compareVersions(String key, Long version1, Long version2) {
        List<ConfigHistory> history = getConfigHistory(key);

        String value1 = null;
        String value2 = null;

        for (ConfigHistory h : history) {
            if (h.id().equals(version1)) {
                value1 = h.newValue();
            }
            if (h.id().equals(version2)) {
                value2 = h.newValue();
            }
        }

        return new ConfigDiff(key, value1, value2,
            Objects.equals(value1, value2) ? "SAME" : "DIFFERENT");
    }

    @Override
    public <T> T getJsonValue(String key, Class<T> clazz) {
        String value = getValue(key, null);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return JSONUtil.toBean(value, clazz);
        } catch (Exception e) {
            log.warn("解析JSON配置失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 内部
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 配置历史在 Redis 里的键：{@code sys:config:history:<租户ID>:<配置键>}。
     *
     * <p>原实现是 {@code ...:<配置键>}，没有租户维度 —— 同一个键在租户 1 与租户 2 下
     * 共用一条历史：租户 2 在历史里能翻到租户 1 的旧值，而"回滚"会把那个旧值写回**自己**的配置。</p>
     */
    private static String historyKey(Long tenantId, String key) {
        return CONFIG_HISTORY_KEY + tenantId + ":" + key;
    }

    /**
     * 保存配置历史记录
     */
    private void saveHistory(Long tenantId, String key, String oldValue, String newValue) {
        try {
            String historyKey = historyKey(tenantId, key);

            Map<String, Object> history = new HashMap<>();
            history.put("id", System.currentTimeMillis());
            history.put("tenantId", tenantId);
            history.put("configKey", key);
            history.put("oldValue", oldValue);
            history.put("newValue", newValue);
            history.put("changedBy", "system");
            history.put("changedAt", LocalDateTime.now().toString());
            history.put("changeReason", "配置更新");

            redisTemplate.opsForList().leftPush(historyKey, JSONUtil.toJsonStr(history));
            redisTemplate.opsForList().trim(historyKey, 0, 99); // 保留最近100条

            log.debug("配置历史已保存: {}", key);
        } catch (Exception e) {
            log.warn("保存配置历史失败: {}", e.getMessage());
        }
    }
}
