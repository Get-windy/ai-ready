package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

/**
 * 终止来源（§13.7 / ㉝）—— 五种，一个不多一个不少。
 *
 * <p><b>⚠️ 终止 ≠ 免责</b>：不论哪一种来源，系统只记录「停止履行」这个**事实**，
 * <b>不自动结清、不自动免责</b>。是否违约、赔多少走 §13.8（平台不裁判）。</p>
 */
public enum AgreementTerminationSource {

    /** 协商一致：必须**双方确认**才生效（status = PENDING → CONFIRMED） */
    MUTUAL_AGREEMENT("协商一致", true),

    /** 自然到期：按有效期到期终止（到期提醒 / 续签另说）。无需对方确认，但对方仍可提异议 */
    NATURAL_EXPIRY("自然到期", false),

    /** 单方终止：一方即可停止履行。⚠️ **不等于不违约** —— 只记事实，违约与否另论（用户原话强调） */
    UNILATERAL("单方终止", false),

    /** 因对方违约：一方以对方违约为由终止。⚠️ "违约"是**当事人主张**，不是平台认定（§13.8） */
    COUNTERPARTY_BREACH("因对方违约", false),

    /** 平台清退（㉝ 平台清退权）：留痕 + 可申诉（异议即申诉留痕） */
    PLATFORM_EXPULSION("平台清退", false);

    private final String label;

    /** 是否需要对方确认后才生效（只有"协商一致"需要）。 */
    private final boolean requiresCounterpartyConfirm;

    AgreementTerminationSource(String label, boolean requiresCounterpartyConfirm) {
        this.label = label;
        this.requiresCounterpartyConfirm = requiresCounterpartyConfirm;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRequiresCounterpartyConfirm() {
        return requiresCounterpartyConfirm;
    }

    /** 解析来源名（不认中文，只认枚举名 —— 与其它枚举的接口口径一致）。 */
    public static AgreementTerminationSource parse(String name) {
        String n = name == null ? null : name.trim();
        if (n == null || n.isEmpty()) {
            throw BusinessException.badRequest("请选择终止来源");
        }
        for (AgreementTerminationSource s : values()) {
            if (s.name().equalsIgnoreCase(n)) {
                return s;
            }
        }
        throw BusinessException.badRequest("终止来源「" + name + "」不支持；可选："
                + "MUTUAL_AGREEMENT 协商一致 / NATURAL_EXPIRY 自然到期 / UNILATERAL 单方终止 / "
                + "COUNTERPARTY_BREACH 因对方违约 / PLATFORM_EXPULSION 平台清退");
    }

    public static AgreementTerminationSource of(String name) {
        if (name == null) {
            return null;
        }
        for (AgreementTerminationSource s : values()) {
            if (s.name().equalsIgnoreCase(name.trim())) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(String name) {
        AgreementTerminationSource s = of(name);
        return s == null ? (name == null ? "" : name) : s.label;
    }
}
