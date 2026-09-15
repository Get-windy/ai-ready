package cn.aiedge.dms.config.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.common.util.SecretMasker;
import cn.aiedge.dms.config.dto.ConfigExportVO;
import cn.aiedge.dms.config.dto.ConfigImportRequest;
import cn.aiedge.dms.config.dto.ConfigImportResultVO;
import cn.aiedge.dms.config.dto.ConfigItemDTO;
import cn.aiedge.dms.config.dto.ConfigItemVO;
import cn.aiedge.dms.config.dto.ConfigQueryDTO;
import cn.aiedge.dms.config.dto.DmsConfigMeta;
import cn.aiedge.dms.config.dto.DmsConfigMetaRegistry;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.entity.DmsConfigHistory;
import cn.aiedge.dms.config.event.ConfigChangedEvent;
import cn.aiedge.dms.config.mapper.DmsConfigHistoryMapper;
import cn.aiedge.dms.config.mapper.DmsConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DMS 配置服务
 *
 * 配置口径：**全局默认（tenant_id = 0）+ 租户覆盖（tenant_id = 当前租户）**；
 * 读取时租户优先、全局兜底，列表返回二者合并结果（同名键以租户值为准）。
 * 敏感键（Key/密钥/令牌/密码）**脱敏返回**，留空保存=不修改，清空走 clearConfig。
 * 保存成功后：① 进程内发布 {@link ConfigChangedEvent}；② Redis 广播 {@link DmsConstants#CONFIG_CHANGE_CHANNEL}，
 * 让**所有实例**立即刷新缓存（地图 Key 等保存即生效，无需重启、不必等 TTL）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final DmsConfigMapper dmsConfigMapper;
    private final DmsConfigHistoryMapper historyMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    /** 单键历史返回上限 */
    private static final int HISTORY_LIMIT = 100;

    /**
     * 获取配置（租户覆盖优先，全局默认兜底）
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return DmsConfig
     */
    public DmsConfig getConfig(Long tenantId, String key) {
        if (tenantId != null && tenantId != 0L) {
            DmsConfig tenantConfig = dmsConfigMapper.selectOne(
                    new LambdaQueryWrapper<DmsConfig>()
                            .eq(DmsConfig::getTenantId, tenantId)
                            .eq(DmsConfig::getConfigKey, key)
            );
            if (tenantConfig != null) {
                return maskIfSecret(tenantConfig);
            }
        }
        return maskIfSecret(dmsConfigMapper.selectGlobalConfigs().stream()
                .filter(c -> key.equals(c.getConfigKey()))
                .findFirst()
                .orElseThrow(() -> new DmsBusinessException("配置不存在: key=" + key + ", tenantId=" + tenantId)));
    }

    /**
     * 更新配置
     *
     * 租户无该键时：全局存在则**新增租户覆盖行**（只影响本租户），否则报错。
     * 敏感键（Key/密钥/令牌/密码）**留空视为「不修改」** —— 页面拿到的是掩码，留空即保持原值；
     * 需要清空请调用 {@link #clearConfig(Long, String)}（避免把掩码写回库）。
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @param value    配置值
     * @return DmsConfig
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsConfig updateConfig(Long tenantId, String key, String value) {
        return applyValue(tenantId, key, value, false);
    }

    /**
     * 清空配置值（敏感键也可安全清空）
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsConfig clearConfig(Long tenantId, String key) {
        return applyValue(tenantId, key, "", true);
    }

    private DmsConfig applyValue(Long tenantId, String key, String value, boolean clear) {
        return applyValue(tenantId, key, value, clear, null);
    }

    /**
     * @param changeTypeOverride 审计变更类型覆盖（null=按 CREATE/UPDATE/CLEAR 推导；导入用 IMPORT）
     */
    private DmsConfig applyValue(Long tenantId, String key, String value, boolean clear, String changeTypeOverride) {
        if (!StringUtils.hasText(key)) {
            throw new DmsBusinessException("配置键不能为空");
        }
        Long tid = effectiveTenant(tenantId);
        // 敏感键留空且非显式清除 → 视为「不修改」，直接返回现值
        if (!clear && SecretMasker.isSecret(key) && !StringUtils.hasText(value)) {
            DmsConfig current = getConfig(tid, key);
            log.info("敏感配置留空保存，保持原值: key={}, tenantId={}", key, tid);
            return current;
        }
        try {
            // 目标行的租户上下文必须与 tenantId 一致：否则多租户插件会把「全局默认行(tenant_id=0)」
            // 过滤掉（登录租户=1 时），导致全局配置读不到 / 更新影响 0 行
            MyBatisPlusConfig.setTempTenantId(tid);

            DmsConfig config = dmsConfigMapper.selectOne(
                    new LambdaQueryWrapper<DmsConfig>()
                            .eq(DmsConfig::getTenantId, tid)
                            .eq(DmsConfig::getConfigKey, key)
            );
            String oldValue = null;
            String changeType;
            if (config != null) {
                oldValue = config.getConfigValue();
                config.setConfigValue(value);
                dmsConfigMapper.updateById(config);
                changeType = clear ? "CLEAR" : "UPDATE";
            } else {
                DmsConfig global = dmsConfigMapper.selectGlobalConfigs().stream()
                        .filter(c -> key.equals(c.getConfigKey()))
                        .findFirst()
                        .orElseThrow(() -> new DmsBusinessException("配置不存在: key=" + key + ", tenantId=" + tid));
                config = new DmsConfig();
                config.setTenantId(tid);
                config.setConfigKey(key);
                config.setConfigDesc(global.getConfigDesc());
                config.setScope(global.getScope());
                config.setConfigValue(value);
                dmsConfigMapper.insert(config);
                changeType = "CREATE";
            }
            if (changeTypeOverride != null) {
                changeType = changeTypeOverride;
            }
            recordHistory(tid, key, oldValue, value, changeType);
            log.info("配置已更新: key={}, tenantId={}, cleared={}, type={}", key, tid, clear, changeType);
            // ① 进程内事件：本实例消费方（MapKeyResolver 等）立即刷新
            eventPublisher.publishEvent(new ConfigChangedEvent(key, value));
            // ② Redis 广播：多实例部署时其它实例同步失效缓存（不等 TTL）
            publishToCluster(key, value);
            return maskIfSecret(config);
        } finally {
            MyBatisPlusConfig.clearTempTenantId();
        }
    }

    // ==================== 变更审计与回滚 ====================

    /**
     * 查询某配置键的变更历史（敏感键只返回掩码，明文列不参与查询）
     */
    public List<DmsConfigHistory> history(Long tenantId, String key) {
        if (!StringUtils.hasText(key)) {
            throw new DmsBusinessException("配置键不能为空");
        }
        Long tid = effectiveTenant(tenantId);
        return historyMapper.selectByKey(key, tid, HISTORY_LIMIT);
    }

    /**
     * 回滚到指定历史版本的前值
     *
     * 语义：把该历史记录的「变更前值」写回；若该历史是 CREATE（此前不存在），回滚等于清除。
     * 敏感键的回滚用服务端保留的明文列（接口从不返回明文），并再记一条 ROLLBACK 审计。
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsConfig rollback(Long tenantId, String key, Long historyId) {
        if (historyId == null) {
            throw new DmsBusinessException("历史记录ID不能为空");
        }
        DmsConfigHistory history = historyMapper.selectByIdWithCipher(historyId);
        if (history == null) {
            throw new DmsBusinessException("历史记录不存在: " + historyId);
        }
        if (StringUtils.hasText(key) && !key.equals(history.getConfigKey())) {
            throw new DmsBusinessException("历史记录与配置键不匹配");
        }
        boolean secret = Integer.valueOf(1).equals(history.getSecret());
        String target = secret ? history.getOldValueCipher() : history.getOldValue();
        boolean toEmpty = !StringUtils.hasText(target) || "CREATE".equals(history.getChangeType());
        DmsConfig result = applyValue(tenantId, history.getConfigKey(), toEmpty ? "" : target, true);
        // applyValue 已记 CLEAR/UPDATE 审计；追加一条 ROLLBACK 说明（便于页面对账）
        recordHistory(effectiveTenant(tenantId), history.getConfigKey(),
                result == null ? null : result.getConfigValue(), toEmpty ? "" : target, "ROLLBACK");
        return result;
    }

    /** 记录一条变更审计（敏感键的展示值掩码 + 明文列仅服务端回滚用） */
    private void recordHistory(Long tenantId, String key, String oldValue, String newValue, String changeType) {
        try {
            boolean secret = SecretMasker.isSecret(key);
            DmsConfigHistory history = new DmsConfigHistory();
            history.setTenantId(tenantId == null ? 0L : tenantId);
            history.setConfigKey(key);
            history.setSecret(secret ? 1 : 0);
            history.setChangeType(changeType);
            history.setOldValue(secret ? SecretMasker.mask(oldValue) : oldValue);
            history.setNewValue(secret ? SecretMasker.mask(newValue) : newValue);
            history.setOldValueCipher(secret ? oldValue : null);
            history.setNewValueCipher(secret ? newValue : null);
            history.setOperatorId(SecurityUtils.getCurrentUserId());
            history.setOperatorName(SecurityUtils.getCurrentUsername());
            history.setClientIp(currentClientIp());
            history.setChangeTime(LocalDateTime.now());
            historyMapper.insert(history);
        } catch (Exception e) {
            // 审计失败不影响配置变更本身
            log.warn("[ConfigService] 记录配置变更审计失败: key={}, err={}", key, e.getMessage());
        }
    }

    private String currentClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    /** 通过 Redis Pub/Sub 广播配置变更；Redis 不可用时静默降级（消费方靠 30s TTL 兜底） */
    private void publishToCluster(String key, String value) {
        try {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            if (redis == null) {
                return;
            }
            String body = "{\"key\":\"" + key + "\",\"value\":\"" + (value == null ? "" : value) + "\"}";
            redis.convertAndSend(DmsConstants.CONFIG_CHANGE_CHANNEL, body);
        } catch (Exception e) {
            log.warn("[ConfigService] 配置变更广播失败（不影响保存）: {}", e.getMessage());
        }
    }

    /** 敏感键脱敏：configValue 返回掩码 + 标记 secret/configured（明文不出服务端） */
    private DmsConfig maskIfSecret(DmsConfig config) {
        if (config == null) {
            return null;
        }
        boolean secret = SecretMasker.isSecret(config.getConfigKey());
        config.setSecret(secret);
        config.setConfigured(StringUtils.hasText(config.getConfigValue()));
        if (secret) {
            config.setConfigValue(SecretMasker.mask(config.getConfigValue()));
        }
        return config;
    }

    /**
     * 获取租户可见的全部配置（全局默认 + 本租户覆盖，同名键以租户值为准）
     *
     * @param tenantId 租户ID（0 表示只看全局默认）
     * @return 配置列表（按配置键升序）
     */
    public List<DmsConfig> listAll(Long tenantId) {
        Map<String, DmsConfig> merged = new LinkedHashMap<>();
        for (DmsConfig global : dmsConfigMapper.selectGlobalConfigs()) {
            merged.put(global.getConfigKey(), global);
        }
        if (tenantId != null && tenantId != 0L) {
            for (DmsConfig tenantConfig : dmsConfigMapper.selectByTenantId(tenantId)) {
                merged.put(tenantConfig.getConfigKey(), tenantConfig);
            }
        }
        return merged.values().stream()
                .sorted((a, b) -> a.getConfigKey().compareTo(b.getConfigKey()))
                .map(this::maskIfSecret)
                .toList();
    }

    /**
     * 获取字符串类型配置值
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return 字符串值
     */
    public String getString(Long tenantId, String key) {
        return getConfig(tenantId, key).getConfigValue();
    }

    /**
     * 获取整数类型配置值
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return 整数值
     */
    public Integer getInteger(Long tenantId, String key) {
        String value = getString(tenantId, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("配置值不是有效的整数: key=" + key + ", value=" + value);
        }
    }

    /**
     * 获取布尔类型配置值
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return 布尔值
     */
    public Boolean getBoolean(Long tenantId, String key) {
        String value = getString(tenantId, key);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 生效租户：显式传入优先；**未传则取当前登录租户**（无租户上下文时回落全局 0）
     *
     * <p>参数中心按「全局默认 + 租户覆盖」口径读写：页面不传 tenantId 时必须落在**当前租户覆盖行**，
     * 否则会误写到全局默认行（表现为「保存/恢复默认后页面值不变」）。</p>
     */
    private Long effectiveTenant(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        Long current = MyBatisPlusConfig.getCurrentTenantIdValue();
        return current == null ? 0L : current;
    }

    // ==================== 参数中心（《配送参数开发文档》§3：元数据驱动 / 分页 / 批量 / 恢复默认） ====================

    /**
     * 参数中心分页（多条件）
     *
     * <p>数据源为「全局默认 + 租户覆盖」两级合并结果，键量小（数十条），故在内存中过滤与分页；
     * 返回行 = 合并值 + 元数据（类型/默认值/范围/单位/生效方式/是否敏感/是否租户覆盖）。</p>
     */
    public Page<ConfigItemVO> page(ConfigQueryDTO query) {
        ConfigQueryDTO q = query != null ? query : new ConfigQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        Set<String> overridden = tenantOverrideKeys(tenantId);
        List<ConfigItemVO> all = listAll(tenantId).stream()
                .map(config -> toVO(config, overridden.contains(config.getConfigKey())))
                .filter(vo -> matches(vo, q))
                .toList();

        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        Page<ConfigItemVO> result = new Page<>(pageNum, pageSize, all.size());
        result.setRecords(new ArrayList<>(all.subList(from, to)));
        return result;
    }

    /**
     * 参数元数据 + 分组树计数（驱动前端渲染与左侧分组）
     *
     * @return {@code {groups: [{key,text,count}], items: [DmsConfigMeta…]}}
     */
    public Map<String, Object> meta() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        List<ConfigItemVO> rows = listAll(tenantId).stream()
                .map(config -> toVO(config, false))
                .toList();
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(ConfigItemVO::getGroup, Collectors.counting()));

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, String> entry : DmsConfigMetaRegistry.GROUPS.entrySet()) {
            long count = counts.getOrDefault(entry.getKey(), 0L);
            if (count == 0 && !"OTHER".equals(entry.getKey())) {
                continue; // 该分组当前无配置项则不展示，保持分组树与数据一致
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("key", entry.getKey());
            group.put("text", entry.getValue());
            group.put("count", count);
            groups.add(group);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        result.put("items", DmsConfigMetaRegistry.all());
        return result;
    }

    /**
     * 批量保存（事务）：逐项做**类型/范围/枚举/JSON/时间范围**校验，再复用单键保存口径
     * （租户覆盖行写入、审计留痕、事件热生效一并复用）
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdate(List<ConfigItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new DmsBusinessException("没有需要保存的参数");
        }
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        int done = 0;
        for (ConfigItemDTO item : items) {
            if (!StringUtils.hasText(item.getConfigKey())) {
                throw new DmsBusinessException("参数键不能为空");
            }
            DmsConfigMeta meta = DmsConfigMetaRegistry.find(item.getConfigKey())
                    .orElseGet(() -> DmsConfigMetaRegistry.fallback(item.getConfigKey()));
            if (Boolean.FALSE.equals(meta.getEditable())) {
                throw new DmsBusinessException("参数不可修改: " + item.getConfigKey());
            }
            // 敏感键留空 = 不修改（页面拿到掩码，直接回写会把掩码写库）
            if (SecretMasker.isSecret(item.getConfigKey()) && !StringUtils.hasText(item.getConfigValue())) {
                continue;
            }
            validateValue(meta, item.getConfigValue());
            updateConfig(tenantId, item.getConfigKey(), item.getConfigValue());
            done++;
        }
        log.info("参数批量保存完成: {} 项", done);
        return done;
    }

    /** 恢复默认：用注册表中的默认值写回（未登记元数据的键拒绝） */
    @Transactional(rollbackFor = Exception.class)
    public DmsConfig reset(Long tenantId, String key) {
        DmsConfigMeta meta = DmsConfigMetaRegistry.find(key)
                .orElseThrow(() -> new DmsBusinessException("该参数未登记元数据，无法恢复默认: " + key));
        if (!StringUtils.hasText(meta.getDefaultValue())
                && !DmsConfigMeta.ValueType.TEXT.equals(meta.getValueType())) {
            throw new DmsBusinessException("该参数未定义默认值: " + key);
        }
        return updateConfig(tenantId, key, meta.getDefaultValue() == null ? "" : meta.getDefaultValue());
    }

    /** 类型/范围/枚举/JSON/时间范围校验（后端前置，杜绝「字符串万能值」） */
    private void validateValue(DmsConfigMeta meta, String value) {
        String v = value == null ? "" : value.trim();
        switch (meta.getValueType()) {
            case NUMBER -> {
                BigDecimal number;
                try {
                    number = new BigDecimal(v);
                } catch (Exception e) {
                    throw new DmsBusinessException(meta.getName() + " 必须是数字");
                }
                if (meta.getMin() != null && number.compareTo(meta.getMin()) < 0) {
                    throw new DmsBusinessException(meta.getName() + " 不能小于 "
                            + meta.getMin().stripTrailingZeros().toPlainString());
                }
                if (meta.getMax() != null && number.compareTo(meta.getMax()) > 0) {
                    throw new DmsBusinessException(meta.getName() + " 不能大于 "
                            + meta.getMax().stripTrailingZeros().toPlainString());
                }
            }
            case BOOLEAN -> {
                if (!List.of("true", "false", "1", "0").contains(v.toLowerCase())) {
                    throw new DmsBusinessException(meta.getName() + " 只能是 true/false");
                }
            }
            case ENUM -> {
                boolean hit = meta.getOptions() != null && meta.getOptions().stream()
                        .anyMatch(o -> o.getValue() != null && o.getValue().equals(v));
                if (!hit) {
                    throw new DmsBusinessException(meta.getName() + " 取值不合法: " + v);
                }
            }
            case JSON -> {
                if (StringUtils.hasText(v)) {
                    try {
                        new ObjectMapper().readTree(v);
                    } catch (Exception e) {
                        throw new DmsBusinessException(meta.getName() + " 必须是合法 JSON");
                    }
                }
            }
            case TIME_RANGE -> {
                if (!v.matches("^([01]\\d|2[0-3]):[0-5]\\d(-([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?)?$")) {
                    throw new DmsBusinessException(meta.getName() + " 格式应为 HH:mm-HH:mm（可带 :ss）");
                }
            }
            default -> {
                if (v.length() > 2000) {
                    throw new DmsBusinessException(meta.getName() + " 长度超限（≤2000）");
                }
            }
        }
    }

    // ==================== 参数集导入 / 导出（《配送参数开发文档》§3.4 / §3.5） ====================

    /**
     * 导出参数集（JSON 参数清单）
     *
     * <p>口径：按查询条件（分组/类型/关键词/作用域…）导出「全局默认 + 租户覆盖」**合并后**的参数集，
     * 可直接作为 {@link #importConfigs} 的请求体回灌到另一环境/租户。</p>
     *
     * <p>⚠️ 敏感键**不导出明文**：值置空并标 `secret/configured`，回灌时按「留空=不修改」跳过。</p>
     */
    public ConfigExportVO exportConfigs(ConfigQueryDTO query) {
        ConfigQueryDTO q = query != null ? query : new ConfigQueryDTO();
        Long tenantId = effectiveReadTenant();
        Set<String> overridden = tenantOverrideKeys(tenantId);
        List<ConfigExportVO.Item> items = listAll(tenantId).stream()
                .map(config -> toVO(config, overridden.contains(config.getConfigKey())))
                .filter(vo -> matches(vo, q))
                .map(this::toExportItem)
                .toList();
        ConfigExportVO vo = new ConfigExportVO();
        vo.setExportedAt(LocalDateTime.now());
        vo.setTenantId(tenantId == null ? 0L : tenantId);
        vo.setCount(items.size());
        vo.setItems(items);
        log.info("参数集导出: tenantId={}, count={}", vo.getTenantId(), items.size());
        return vo;
    }

    private ConfigExportVO.Item toExportItem(ConfigItemVO row) {
        ConfigExportVO.Item item = new ConfigExportVO.Item();
        item.setConfigKey(row.getConfigKey());
        item.setName(row.getName());
        item.setGroup(row.getGroup());
        item.setValueType(row.getValueType());
        item.setUnit(row.getUnit());
        item.setSecret(Boolean.TRUE.equals(row.getSecret()));
        item.setConfigured(row.getConfigured());
        item.setDefaultValue(row.getDefaultValue());
        item.setConfigValue(Boolean.TRUE.equals(row.getSecret()) ? "" : row.getConfigValue());
        return item;
    }

    /**
     * 导入参数集（PREVIEW 预览差异 / APPLY 落库）
     *
     * <p>规则：</p>
     * <ol>
     *   <li>仅接受**已存在的参数键**（未登记/不存在 → 失败项）；不可改参数 → 失败项；</li>
     *   <li>逐项类型校验（NUMBER 范围 / ENUM 枚举 / BOOLEAN / JSON / TIME_RANGE）；</li>
     *   <li>敏感键：值空或为掩码（含 `*`）→ 跳过（不修改），杜绝掩码写库；</li>
     *   <li>值相同 → SAME（不写库）；已有租户覆盖且 `overwrite=false` → 跳过；</li>
     *   <li>**存在失败项则 APPLY 整体拒绝**（与批量保存事务语义一致），PREVIEW 不写库、不记审计、不发事件。</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfigImportResultVO importConfigs(ConfigImportRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new DmsBusinessException("没有可导入的参数");
        }
        Long tenantId = effectiveReadTenant();
        Set<String> overridden = tenantOverrideKeys(tenantId);
        Map<String, ConfigItemVO> current = listAll(tenantId).stream()
                .map(config -> toVO(config, overridden.contains(config.getConfigKey())))
                .collect(Collectors.toMap(ConfigItemVO::getConfigKey, v -> v, (a, b) -> a, LinkedHashMap::new));

        ConfigImportResultVO result = new ConfigImportResultVO();
        result.setMode(request.isPreview() ? "PREVIEW" : "APPLY");
        result.setTotal(request.getItems().size());
        List<ConfigItemDTO> toApply = new ArrayList<>();

        for (ConfigExportVO.Item item : request.getItems()) {
            String key = item.getConfigKey();
            if (!StringUtils.hasText(key)) {
                result.getFailed().add(new ConfigImportResultVO.Failure("(空)", "参数键不能为空"));
                continue;
            }
            ConfigItemVO exist = current.get(key);
            if (exist == null) {
                result.getFailed().add(new ConfigImportResultVO.Failure(key, "参数不存在或未登记元数据，暂不支持导入新增参数"));
                continue;
            }
            if (Boolean.FALSE.equals(exist.getEditable())) {
                result.getFailed().add(new ConfigImportResultVO.Failure(key, "参数不可修改"));
                continue;
            }
            boolean secret = Boolean.TRUE.equals(exist.getSecret());
            String newValue = item.getConfigValue() == null ? "" : item.getConfigValue().trim();
            // 敏感键：空值 / 掩码 → 不修改（导出物中敏感键本就为空）
            if (secret && (!StringUtils.hasText(newValue) || newValue.contains("*"))) {
                result.setSkipped(nz(result.getSkipped()) + 1);
                result.getPreview().add(preview(exist, "", "SKIP", "敏感键留空=不修改", true));
                continue;
            }
            DmsConfigMeta meta = DmsConfigMetaRegistry.find(key)
                    .orElseGet(() -> DmsConfigMetaRegistry.fallback(key));
            try {
                validateValue(meta, newValue);
            } catch (DmsBusinessException e) {
                result.getFailed().add(new ConfigImportResultVO.Failure(key, e.getMessage()));
                continue;
            }
            String oldValue = exist.getConfigValue() == null ? "" : exist.getConfigValue();
            if (oldValue.equals(newValue)) {
                result.setUnchanged(nz(result.getUnchanged()) + 1);
                result.getPreview().add(preview(exist, newValue, "SAME", null, secret));
                continue;
            }
            boolean hasOverride = Boolean.TRUE.equals(exist.getTenantOverride());
            if (hasOverride && !request.isOverwrite()) {
                result.setSkipped(nz(result.getSkipped()) + 1);
                result.getPreview().add(preview(exist, newValue, "SKIP", "已存在租户覆盖值，按「不覆盖」跳过", secret));
                continue;
            }
            result.setChanged(nz(result.getChanged()) + 1);
            result.getPreview().add(preview(exist, newValue, hasOverride ? "UPDATE" : "CREATE", null, secret));
            ConfigItemDTO dto = new ConfigItemDTO();
            dto.setConfigKey(key);
            dto.setConfigValue(newValue);
            toApply.add(dto);
        }

        result.setChanged(nz(result.getChanged()));
        result.setUnchanged(nz(result.getUnchanged()));
        result.setSkipped(nz(result.getSkipped()));

        // 任一项非法 → 整体拒绝（不落库）
        if (!result.getFailed().isEmpty()) {
            log.warn("参数集导入被拒绝: {} 项非法", result.getFailed().size());
            return result;
        }
        if (request.isPreview()) {
            return result;
        }
        for (ConfigItemDTO dto : toApply) {
            applyValue(tenantId, dto.getConfigKey(), dto.getConfigValue(), false, "IMPORT");
        }
        log.info("参数集导入完成: tenantId={}, changed={}, skipped={}", tenantId, result.getChanged(), result.getSkipped());
        return result;
    }

    private ConfigImportResultVO.Preview preview(ConfigItemVO exist, String newValue, String action,
                                                 String reason, boolean secret) {
        ConfigImportResultVO.Preview p = new ConfigImportResultVO.Preview();
        p.setConfigKey(exist.getConfigKey());
        p.setName(exist.getName());
        p.setGroup(exist.getGroup());
        p.setValueType(String.valueOf(exist.getValueType()));
        p.setOldValue(secret ? SecretMasker.mask(exist.getConfigValue()) : exist.getConfigValue());
        p.setNewValue(newValue);
        p.setAction(action);
        p.setReason(reason);
        p.setSecret(secret);
        return p;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    /** 读取口径的租户：分页/导出/导入统一取当前登录租户（无上下文回落 0） */
    private Long effectiveReadTenant() {
        return effectiveTenant(null);
    }

    /** 当前租户实际覆盖的键集合（用于页面标注「租户覆盖 / 继承全局」） */
    private Set<String> tenantOverrideKeys(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return Set.of();
        }
        return dmsConfigMapper.selectByTenantId(tenantId).stream()
                .map(DmsConfig::getConfigKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean matches(ConfigItemVO vo, ConfigQueryDTO q) {
        if (StringUtils.hasText(q.getGroup()) && !q.getGroup().equals(vo.getGroup())) {
            return false;
        }
        if (StringUtils.hasText(q.getValueType())
                && !q.getValueType().equalsIgnoreCase(String.valueOf(vo.getValueType()))) {
            return false;
        }
        if (q.getEditable() != null && !q.getEditable().equals(vo.getEditable())) {
            return false;
        }
        if (StringUtils.hasText(q.getEffect()) && !q.getEffect().equalsIgnoreCase(vo.getEffect())) {
            return false;
        }
        if (Boolean.TRUE.equals(q.getConfiguredOnly()) && !StringUtils.hasText(vo.getConfigValue())) {
            return false;
        }
        if (StringUtils.hasText(q.getScope())
                && !q.getScope().equalsIgnoreCase(String.valueOf(vo.getScope()))) {
            return false;
        }
        if (q.getTenantOverride() != null
                && !q.getTenantOverride().equals(Boolean.TRUE.equals(vo.getTenantOverride()))) {
            return false;
        }
        if (!inTimeRange(vo.getUpdateTime(), q.getUpdateTimeStart(), q.getUpdateTimeEnd())) {
            return false;
        }
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim().toLowerCase();
            return containsIgnoreCase(vo.getConfigKey(), kw)
                    || containsIgnoreCase(vo.getName(), kw)
                    || containsIgnoreCase(vo.getDesc(), kw);
        }
        return true;
    }

    /** 更新时间区间过滤（起止均可空；止为纯日期时含当天 23:59:59） */
    private boolean inTimeRange(LocalDateTime updateTime, String start, String end) {
        if (!StringUtils.hasText(start) && !StringUtils.hasText(end)) {
            return true;
        }
        if (updateTime == null) {
            return false;
        }
        LocalDateTime from = parseBound(start, false);
        LocalDateTime to = parseBound(end, true);
        if (from != null && updateTime.isBefore(from)) {
            return false;
        }
        return to == null || !updateTime.isAfter(to);
    }

    private LocalDateTime parseBound(String text, boolean endOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String v = text.trim().replace('T', ' ');
        try {
            if (v.length() == 10) {
                return LocalDateTime.parse(v + (endOfDay ? "T23:59:59" : "T00:00:00"));
            }
            if (v.length() == 16) {
                return LocalDateTime.parse(v.replace(' ', 'T') + (endOfDay ? ":59" : ":00"));
            }
            return LocalDateTime.parse(v.replace(' ', 'T'));
        } catch (Exception e) {
            throw new DmsBusinessException("更新时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss: " + text);
        }
    }

    private boolean containsIgnoreCase(String text, String lowerKeyword) {
        return text != null && text.toLowerCase().contains(lowerKeyword);
    }

    /** 合并行 → 参数中心行（未登记元数据的键归「其他」分组、按 TEXT 处理） */
    private ConfigItemVO toVO(DmsConfig config, boolean tenantOverride) {
        DmsConfigMeta meta = DmsConfigMetaRegistry.find(config.getConfigKey())
                .orElseGet(() -> DmsConfigMetaRegistry.fallback(config.getConfigKey()));
        ConfigItemVO vo = new ConfigItemVO();
        vo.setConfigKey(config.getConfigKey());
        vo.setName(meta.getName());
        vo.setGroup(meta.getGroup());
        vo.setGroupText(meta.getGroupText());
        vo.setConfigValue(config.getConfigValue());
        vo.setDefaultValue(meta.getDefaultValue());
        vo.setValueType(meta.getValueType());
        vo.setMin(meta.getMin());
        vo.setMax(meta.getMax());
        vo.setUnit(meta.getUnit());
        vo.setOptions(meta.getOptions());
        vo.setEditable(meta.getEditable());
        vo.setEffect(meta.getEffect());
        vo.setSecret(Boolean.TRUE.equals(config.getSecret()) || Boolean.TRUE.equals(meta.getSecret()));
        vo.setConfigured(config.getConfigured());
        vo.setDesc(StringUtils.hasText(config.getConfigDesc()) ? config.getConfigDesc() : meta.getDesc());
        vo.setScope(config.getScope());
        vo.setTenantOverride(tenantOverride);
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
