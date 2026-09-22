package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.enums.FulfillmentMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code AgreementRuntime.resolve(...)} 的返回：**某一业务时点生效那一版**的完整设定集。
 *
 * <p>里面同时带上"是哪一版"（版本号与有效期区间）与"每种取值的三态"。
 * 带版本信息不是装饰：司法举证与故障排查要回答的第一个问题都是
 * 「<b>这笔交易当时按的是哪一版</b>」。</p>
 *
 * <p><b>⚠️ 没有生效版本 ≠ 未约定</b>：前者是"这份协议此刻根本不适用"（例如尚未生效、
 * 或业务时点落在协议有效期之外），后者是"适用，但这一项没约定"。
 * 因此本类用 {@link #isVersionFound()} 单独表达，{@code get/require} 在无版本时
 * 抛出明确的中文业务异常，而不是悄悄返回一个空值聚合。</p>
 */
public final class AgreementResolvedSettings {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 该业务时点有没有找到生效版本。 */
    private final boolean versionFound;

    /** 没找到时的中文原因（直接可展示，排查时不用猜）。 */
    private final String absenceReason;

    private final Long agreementId;

    private final Long versionId;

    private final Integer versionNo;

    private final LocalDateTime effectiveFrom;

    private final LocalDateTime effectiveTo;

    /** 查询使用的业务时点（下单/发货/结算发生的那个时刻）。 */
    private final LocalDateTime businessTime;

    /** 字段编码 → 三态取值（含"未约定/未定义"，不是只放已约定的项）。 */
    private final Map<String, AgreementSettingValue> values;

    /** 履约方式**集合**（可为空集 = 这一版没约定履约方式，不等于"随便用哪种"）。 */
    private final Set<FulfillmentMode> fulfillmentModes;

    /** 平台字段元数据（界面据此显示"这一项是什么、影响什么"）。 */
    private final List<AgreementSettingDef> definitions;

    AgreementResolvedSettings(boolean versionFound, String absenceReason,
                              Long agreementId, Long versionId, Integer versionNo,
                              LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                              LocalDateTime businessTime,
                              Map<String, AgreementSettingValue> values,
                              Set<FulfillmentMode> fulfillmentModes,
                              List<AgreementSettingDef> definitions) {
        this.versionFound = versionFound;
        this.absenceReason = absenceReason;
        this.agreementId = agreementId;
        this.versionId = versionId;
        this.versionNo = versionNo;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.businessTime = businessTime;
        this.values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.fulfillmentModes = fulfillmentModes == null
                ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(fulfillmentModes));
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    /** 「该业务时点没有生效版本」的明确结果（**不是** {@code null}，也**不是**异常）。 */
    public static AgreementResolvedSettings noEffectiveVersion(String reason, LocalDateTime businessTime,
                                                              List<AgreementSettingDef> definitions) {
        return new AgreementResolvedSettings(false, reason, null, null, null, null, null,
                businessTime, Map.of(), Set.of(), definitions);
    }

    // ══════════════════════ 读 ══════════════════════

    public boolean isVersionFound() {
        return versionFound;
    }

    public String getAbsenceReason() {
        return absenceReason;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public LocalDateTime getBusinessTime() {
        return businessTime;
    }

    public Map<String, AgreementSettingValue> getValues() {
        return values;
    }

    public Set<FulfillmentMode> getFulfillmentModes() {
        return fulfillmentModes;
    }

    public List<AgreementSettingDef> getDefinitions() {
        return definitions;
    }

    /** 本版允许的履约方式里有没有这一种（订单路由问的第一个问题）。 */
    public boolean allows(FulfillmentMode mode) {
        return mode != null && fulfillmentModes.contains(mode);
    }

    /**
     * 取某个设定项的**三态**结果（不抛错，供"先看看约定了没有"的调用方）。
     *
     * @throws cn.aiedge.common.exception.BusinessException 该业务时点没有生效版本
     *         （此时"值是多少"这个问题本身不成立，必须让调用方知道）
     */
    public AgreementSettingValue get(String settingKey) {
        requireVersion();
        AgreementSettingValue v = values.get(settingKey);
        // 元数据里没有、且本版也没存过 ⇒ 未定义（不是"未约定"：字段本身就不存在）
        return v == null ? AgreementSettingValue.undefined(settingKey) : v;
    }

    /**
     * **严格取值**：要求该字段"已约定"，否则抛中文业务异常。
     *
     * <p>用法示例（下游链路的写法）：
     * <pre>{@code
     * int days = settings.require("AR_CREDIT_DAYS", "生成应收到期日").requireDurationDays();
     * }</pre>
     * 未约定时的报错形如「本协议未约定「账期天数」，无法生成应收到期日。未约定的条款系统不会自动执行…」，
     * 正是 §13.3 要求的"拦下并明确报错"。</p>
     *
     * @param settingKey 字段编码
     * @param usage      这个值要拿去做什么（会拼进报错文案，让人一眼知道该怎么办）
     */
    public AgreementSettingValue require(String settingKey, String usage) {
        requireVersion();
        AgreementSettingValue v = get(settingKey);
        v.assertAgreed(usage);
        return v;
    }

    private void requireVersion() {
        if (!versionFound) {
            throw cn.aiedge.common.exception.BusinessException.badRequest(
                    "本协议在 " + (businessTime == null ? "该" : businessTime.format(TS))
                            + " 这个时点没有生效版本，无法取得协议设定。"
                            + (absenceReason == null ? "" : absenceReason));
        }
    }

    @Override
    public String toString() {
        return versionFound
                ? "AgreementResolvedSettings{协议=" + agreementId + ", 版本=" + versionNo
                + ", 业务时点=" + (businessTime == null ? "" : businessTime.format(TS))
                + ", 字段数=" + values.size() + ", 履约方式=" + fulfillmentModes + "}"
                : "AgreementResolvedSettings{该时点无生效版本: " + absenceReason + "}";
    }
}
