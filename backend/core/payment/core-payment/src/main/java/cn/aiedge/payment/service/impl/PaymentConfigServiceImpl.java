package cn.aiedge.payment.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import cn.aiedge.base.payment.PaymentCallbackVerifier;
import cn.aiedge.payment.channel.PaymentChannel;
import cn.aiedge.payment.config.PaymentConfigCatalog;
import cn.aiedge.payment.dto.PaymentChannelConfigVO;
import cn.aiedge.payment.dto.PaymentChannelParam;
import cn.aiedge.payment.dto.PaymentConfigItemVO;
import cn.aiedge.payment.dto.PaymentSceneVO;
import cn.aiedge.payment.service.PaymentConfigService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 支付配置服务实现（真实落库版）。
 *
 * <p><b>修的是什么（P0，开发文档 §12-①/②）</b>：原实现的渠道参数写路径走
 * {@code POST /api/config/save-value}（只写缓存不落库），读路径走 {@code GET /api/config/list}
 * （只返回内置 12 条、无任何 {@code payment.} 键）→ 保存后重新打开抽屉永远为空，
 * 却提示「保存成功」。本实现改为**真实读写 {@code sys_project_config}**，保存后再次 GET 必然读到刚写进去的值。</p>
 *
 * <p><b>为什么用 {@code sys_project_config} 而不是 {@code sys_config}</b>（裁定说明）：</p>
 * <ul>
 *   <li>开发文档 §11 / §8.4 列出三个候选载体并注明「需先裁定」：{@code sys_config.param_value}、
 *       {@code sys_project_config.config_value}、{@code md_payment_channel}；</li>
 *   <li>{@code sys_config} 正被「系统参数」页（菜单 80621）改造为它自己的载体（迁移 V11.393.0），
 *       本页若共用会与该页的 {@code nav_group} 视图维度互相污染；且 {@code sys_config.param_key}
 *       是**全表唯一**（不含 tenant_id），多租户下第二家租户写同名键会唯一冲突；</li>
 *   <li>{@code md_payment_channel} 是「资料 → 支付渠道」页的**档案表**，开发文档 §1.1 已明确
 *       本页「❌ 不读 {@code md_payment_channel}」；</li>
 *   <li>{@code sys_project_config} 是本系统**在用的配置中心 KV**（{@code SysConfigServiceImpl} 的载体，
 *       已在存 {@code user_page_config:*} / {@code expense.approval.*} 等），有独立 {@code config_group}
 *       维度可隔离，且本页统一使用 {@code payment.*} 键前缀 —— 2026-09-18 实测该表
 *       {@code config_key LIKE 'payment%'} 为 0 行，**不撞键**。</li>
 * </ul>
 *
 * <p><b>租户口径</b>：{@code sys_project_config} 登记在
 * {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 中 → 多租户拦截器**不会**为该表注入
 * {@code tenant_id} 条件，因此本类所有查询都**显式**带上租户条件，否则会跨租户串数据。
 * 读：优先取当前租户的行，取不到再回落到 {@code tenant_id = 0}（平台默认行）；
 * 写：一律写到当前租户（解析不到租户时才落到 0）。这避免「更新 0 行却提示成功」的坑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfigServiceImpl implements PaymentConfigService {

    /** 平台默认行（全局默认）的租户 ID，与全站约定一致 */
    private static final Long PLATFORM_TENANT_ID = 0L;

    private final SysProjectConfigMapper configMapper;
    private final List<PaymentChannel> channels;

    /**
     * 平台提供的回调验签实现。
     *
     * <p>用来判定「本租户下该渠道是否真的能生效」：**没配凭据 / 凭据格式无效 ⇒ 不生效**。
     * 渠道 Bean 的 {@code isAvailable()} 只表示「代码层面支持该渠道」（实测所有实现都无条件返回
     * true），不能用来代表「这个租户配好了」。没有验签器的渠道（现金/银行）不走回调，视为就绪。</p>
     */
    private final List<PaymentCallbackVerifier> callbackVerifiers;

    // ═══════════════════════════════════════════════════════════════════
    // Tab② 支付方式：渠道参数
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 本租户下该渠道的回调验签凭据是否就绪。
     *
     * <p>判定交给对应的 {@link PaymentCallbackVerifier#isConfigured}（内部做**格式解析**，
     * 不是看字段非空）—— 这样「填了半截公钥」等同于「没填」。</p>
     *
     * <p>没有任何验签器认领该渠道时返回 {@code true}：现金、银行转账这类**不走回调**的渠道
     * 没有验签凭据可配，不应因此被判为不可用。</p>
     */
    private boolean credentialReady(String channelCode) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return callbackVerifiers.stream()
                .filter(v -> v.channelCode() != null && v.channelCode().equalsIgnoreCase(channelCode))
                .findFirst()
                .map(v -> v.isConfigured(tenantId))
                .orElse(true);
    }

    @Override
    public List<PaymentChannelConfigVO> listChannelConfigs(String keyword, Boolean enabled) {
        String kw = StrUtil.trimToNull(keyword);
        List<PaymentChannelConfigVO> rows = new ArrayList<>();
        for (PaymentChannel channel : sortedChannels()) {
            String code = channel.getChannelCode();
            String name = channel.getChannelName();
            if (kw != null && !containsIgnoreCase(code, kw) && !containsIgnoreCase(name, kw)) {
                continue;
            }
            PaymentChannelParam param = readChannelParam(code);

            PaymentChannelConfigVO vo = new PaymentChannelConfigVO();
            vo.setChannelCode(code);
            vo.setChannelName(name);
            vo.setMinAmount(channel.getMinAmount());
            vo.setMaxAmount(channel.getMaxAmount());
            // 生效判定 = 渠道自身可用 且 本租户凭据就绪（未配 / 格式无效 都算未就绪）
            boolean credentialReady = credentialReady(code);
            vo.setCredentialReady(credentialReady);
            vo.setAvailable(channel.isAvailable() && credentialReady);
            if (param != null) {
                vo.setAppId(param.getAppId());
                vo.setMerchantNo(param.getMerchantNo());
                vo.setNotifyUrl(param.getNotifyUrl());
                vo.setSecretConfigured(StrUtil.isNotBlank(param.getAppSecret()));
                vo.setEnabled(param.getEnabled() == null || param.getEnabled());
            } else {
                // 未配置过的渠道按「启用」展示：与原抽屉的初始值 enabled=true 保持一致，
                // 不改动渠道 Bean 自身的 isAvailable（渠道可用性由代码决定，本页只存参数）
                vo.setSecretConfigured(false);
                vo.setEnabled(true);
            }
            if (enabled != null && !enabled.equals(vo.getEnabled())) {
                continue;
            }
            rows.add(vo);
        }
        return rows;
    }

    @Override
    public PaymentChannelParam getChannelParam(String channelCode) {
        PaymentChannelParam param = readChannelParam(channelCode);
        return param != null ? param : new PaymentChannelParam();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChannelParam(String channelCode, PaymentChannelParam param) {
        String code = normalizeChannelCode(channelCode);
        if (code == null) {
            throw new IllegalArgumentException("不支持的支付渠道: " + channelCode);
        }
        PaymentChannelParam toSave = param != null ? param : new PaymentChannelParam();
        upsert(PaymentConfigCatalog.channelKey(code), JSONUtil.toJsonStr(toSave),
                "json", "渠道参数（" + code + "）");
        log.info("支付渠道参数已落库: channel={}, tenant={}", code, currentTenantId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tab① 微信公众号配置 / Tab④ 在线退款：配置项
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public List<PaymentConfigItemVO> listItems(String tab, String keyword) {
        List<PaymentConfigCatalog.ItemDef> defs = PaymentConfigCatalog.itemsOf(tab);
        if (defs.isEmpty()) {
            throw new IllegalArgumentException("不支持的配置分组: " + tab);
        }
        String kw = StrUtil.trimToNull(keyword);
        List<PaymentConfigItemVO> rows = new ArrayList<>();
        for (PaymentConfigCatalog.ItemDef def : defs) {
            if (kw != null && !containsIgnoreCase(def.key(), kw) && !containsIgnoreCase(def.name(), kw)) {
                continue;
            }
            SysProjectConfig row = findRow(def.key());
            PaymentConfigItemVO vo = new PaymentConfigItemVO();
            vo.setItemKey(def.key());
            vo.setItemName(def.name());
            vo.setValueType(def.valueType());
            vo.setDescription(def.description());
            vo.setItemValue(row != null && row.getConfigValue() != null
                    ? row.getConfigValue() : def.defaultValue());
            vo.setUpdateTime(row != null ? row.getUpdateTime() : null);
            rows.add(vo);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveItem(String itemKey, String itemValue) {
        PaymentConfigCatalog.ItemDef def = PaymentConfigCatalog.itemOf(itemKey);
        if (def == null) {
            throw new IllegalArgumentException("配置键不在支付配置白名单内: " + itemKey);
        }
        String value = itemValue == null ? "" : itemValue;
        if ("boolean".equals(def.valueType())) {
            value = Boolean.toString("true".equalsIgnoreCase(value) || "1".equals(value));
        }
        upsert(itemKey, value, "boolean".equals(def.valueType()) ? "boolean" : "string", def.name());
        log.info("支付配置项已落库: key={}, value={}, tenant={}", itemKey, value, currentTenantId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tab③ 场景配置
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public List<PaymentSceneVO> listScenes(String keyword) {
        String kw = StrUtil.trimToNull(keyword);
        List<PaymentSceneVO> rows = new ArrayList<>();
        for (PaymentConfigCatalog.SceneDef def : PaymentConfigCatalog.SCENES) {
            if (kw != null && !containsIgnoreCase(def.code(), kw) && !containsIgnoreCase(def.name(), kw)) {
                continue;
            }
            SysProjectConfig row = findRow(PaymentConfigCatalog.sceneKey(def.code()));
            PaymentSceneVO vo = new PaymentSceneVO();
            vo.setSceneCode(def.code());
            vo.setSceneName(def.name());
            vo.setChannels(row != null ? parseChannels(row.getConfigValue()) : List.of());
            vo.setUpdateTime(row != null ? row.getUpdateTime() : null);
            rows.add(vo);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScene(String sceneCode, List<String> channels) {
        if (sceneCode == null || !PaymentConfigCatalog.isScene(sceneCode)) {
            throw new IllegalArgumentException("不支持的支付场景: " + sceneCode);
        }
        Set<String> known = sortedChannels().stream()
                .map(c -> c.getChannelCode().toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 只保留已知渠道码并去重（未知码直接丢弃，避免脏配置写进 KV）
        String value = (channels == null ? List.<String>of() : channels).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .filter(known::contains)
                .distinct()
                .collect(Collectors.joining(","));

        upsert(PaymentConfigCatalog.sceneKey(sceneCode), value, "string", "支付场景配置（" + sceneCode + "）");
        log.info("支付场景配置已落库: scene={}, channels={}, tenant={}", sceneCode, value, currentTenantId());
    }

    @Override
    public List<String> refundNotes() {
        return PaymentConfigCatalog.REFUND_NOTES;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 内部：KV 读写
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 真实 upsert 一条配置行。
     *
     * <p>⚠️ 用 {@link LambdaUpdateWrapper} 而非 {@code updateById}：MyBatis-Plus 的
     * {@code updateById} 会**忽略 null 字段**，而本页需要能把密钥等字段**清空**，
     * 用 {@code set(..., null)} 才能落成 NULL。</p>
     */
    private void upsert(String configKey, String configValue, String configType, String description) {
        Long tenantId = writeTenantId();
        LocalDateTime now = LocalDateTime.now();
        // ⚠️ 必须用 findOwnRow：原实现用 findRow（含「回落平台行」），
        // 于是**本租户没配过时，会把平台那一行当成"已存在"并更新它** ——
        // 租户 A 保存自己的渠道参数 = 覆盖平台配置（并可能波及所有未配置的租户）。
        SysProjectConfig existing = findOwnRow(configKey, tenantId);

        if (existing != null) {
            LambdaUpdateWrapper<SysProjectConfig> update = new LambdaUpdateWrapper<>();
            update.eq(SysProjectConfig::getId, existing.getId())
                    .set(SysProjectConfig::getConfigValue, configValue)
                    .set(SysProjectConfig::getConfigType, configType)
                    // 命中行原本可能被停用（status != 0）而读不到，保存时一并恢复为生效
                    .set(SysProjectConfig::getStatus, 0)
                    .set(SysProjectConfig::getUpdateTime, now);
            configMapper.update(null, update);
            return;
        }

        SysProjectConfig row = new SysProjectConfig();
        row.setTenantId(tenantId);
        row.setConfigKey(configKey);
        row.setConfigValue(configValue);
        row.setConfigType(configType);
        row.setConfigGroup(PaymentConfigCatalog.CONFIG_GROUP);
        row.setDescription(description);
        row.setStatus(0);
        // deleted 列 NOT NULL：显式给 0，不依赖逻辑删除插件在 insert 时补默认值
        row.setDeleted(0);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        configMapper.insert(row);
    }

    /** 按当前租户读取配置行（取不到时回落平台默认行 tenant_id = 0） */
    /**
     * 只取**本租户**的行，**不回落平台行**。
     *
     * <p>用于承载凭据的配置（支付渠道参数）：凭据是身份，不是可继承的默认值 ——
     * 回落等于把平台商户号借给所有租户用，违背「凭证只能本租户自用」。</p>
     */
    private SysProjectConfig findOwnRow(String configKey, Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return selectByKeyAndTenant(configKey, tenantId);
    }

    private SysProjectConfig findRow(String configKey) {
        return findRow(configKey, currentTenantId());
    }

    private SysProjectConfig findRow(String configKey, Long tenantId) {
        if (tenantId != null && !PLATFORM_TENANT_ID.equals(tenantId)) {
            // 先精确取本租户的行；没有才回落平台默认行。
            // sys_project_config 被登记为「忽略租户表」，此处必须自己带 tenant_id 条件。
            SysProjectConfig own = selectByKeyAndTenant(configKey, tenantId);
            if (own != null) {
                return own;
            }
        }
        return selectByKeyAndTenant(configKey, PLATFORM_TENANT_ID);
    }

    private SysProjectConfig selectByKeyAndTenant(String configKey, Long tenantId) {
        LambdaQueryWrapper<SysProjectConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysProjectConfig::getConfigKey, configKey)
                .eq(SysProjectConfig::getTenantId, tenantId)
                .orderByDesc(SysProjectConfig::getId);
        wrapper.last("LIMIT 1");
        return configMapper.selectOne(wrapper);
    }

    /** 读取渠道参数（未配置返回 null，由调用方决定默认值） */
    private PaymentChannelParam readChannelParam(String channelCode) {
        String code = normalizeChannelCode(channelCode);
        if (code == null) {
            return null;
        }
        // ⚠️ 用 findOwnRow 而非 findRow：渠道参数里有商户号与密钥，
        // **不得回落平台行**，否则租户没配时会读到平台凭据并显示为「已配置」。
        SysProjectConfig row = findOwnRow(PaymentConfigCatalog.channelKey(code), currentTenantId());
        if (row == null || StrUtil.isBlank(row.getConfigValue())) {
            return null;
        }
        try {
            return JSONUtil.toBean(row.getConfigValue(), PaymentChannelParam.class);
        } catch (Exception e) {
            log.warn("支付渠道参数解析失败: channel={}, raw={}", code, row.getConfigValue());
            return null;
        }
    }

    /** 解析场景值（逗号分隔渠道码） */
    private List<String> parseChannels(String raw) {
        if (StrUtil.isBlank(raw)) {
            return List.of();
        }
        Set<String> known = sortedChannels().stream()
                .map(c -> c.getChannelCode().toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .filter(known::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<PaymentChannel> sortedChannels() {
        return channels.stream()
                .sorted(Comparator.comparing(PaymentChannel::getChannelCode))
                .collect(Collectors.toList());
    }

    /** 渠道码归一：必须在渠道 Bean 清单内（防止把任意字符串写成配置键） */
    private String normalizeChannelCode(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return null;
        }
        String upper = channelCode.trim().toUpperCase();
        return sortedChannels().stream()
                .map(PaymentChannel::getChannelCode)
                .filter(c -> c.equalsIgnoreCase(upper))
                .findFirst()
                .orElse(null);
    }

    private static boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private static Long currentTenantId() {
        return MyBatisPlusConfig.getCurrentTenantIdValue();
    }

    private static Long writeTenantId() {
        Long tenantId = currentTenantId();
        return tenantId != null ? tenantId : PLATFORM_TENANT_ID;
    }
}
