package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

/**
 * 文字版（{@code agreement_narrative.section_code}）的段落类别，DOMAIN-MODEL §13.1 / §13.2。
 *
 * <h3>⚠️ 文字版 = 只留痕举证，**系统永不自动执行**</h3>
 * 本枚举的每一项都是"文字条款"：争议解决与管辖、保密、不可抗力、特别约定、
 * 违约责任的具体文字表述。<b>它们没有消费方</b> —— 不解析、不自动执行，
 * 只做双方确认 + 哈希留痕，供举证（§13.2 第三行）。
 *
 * <h3>为什么"不会自动执行"要写进接口返回，而不是只写在注释里</h3>
 * 用户看界面时默认"写进合同的东西系统会照做"。若界面不说，他会以为写了
 * 「逾期按日万分之五」系统就会自动扣钱 —— 于是既不自己主张，也错过了时效。
 * §13.2 明确要求「<b>界面必须显式告知</b>"此类条款系统不会自动执行，需人工处理"」，
 * 所以这条口径落在 {@link #AUTO_EXECUTABLE} 与 {@link #MANUAL_NOTICE} 两个<b>可下发的常量</b>上，
 * 由接口原样带给前端（不是注释里写一句就完事）。
 */
public enum AgreementNarrativeSection {

    /** 争议解决方式与管辖法院 —— 真的出事时按它去司法解决（这是 §13.8 的出口）。 */
    DISPUTE("争议解决与管辖"),

    /** 保密义务。 */
    CONFIDENTIALITY("保密"),

    /** 不可抗力。 */
    FORCE_MAJEURE("不可抗力"),

    /** 特别约定（双方自定的、不属于其它类别的补充条款）。 */
    SPECIAL_TERMS("特别约定"),

    /** 违约责任的具体文字表述（赔什么、怎么算的叙述；真正自动扣罚要用"后果设定"的字段，见 §13.2）。 */
    BREACH_LIABILITY_TEXT("违约责任（文字表述）");

    /**
     * ⚠️ 永远是 {@code false} —— 文字条款没有消费方，系统不会自动执行（§13.2）。
     * 接口必须把这个值下发给前端，让界面能明确告诉用户。
     */
    public static final boolean AUTO_EXECUTABLE = false;

    /** 与 {@link #AUTO_EXECUTABLE} 配套的一句中文说明，前端直接展示给用户。 */
    public static final String MANUAL_NOTICE = "此类条款为文字条款：系统不会自动执行，需人工处理（仅供双方确认与留痕举证）";

    private final String label;

    AgreementNarrativeSection(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AgreementNarrativeSection of(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim();
        for (AgreementNarrativeSection s : values()) {
            if (s.name().equalsIgnoreCase(c)) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(String code) {
        AgreementNarrativeSection s = of(code);
        return s == null ? (code == null ? "" : code) : s.label;
    }

    /**
     * 解析段落类别（大小写不敏感）。
     *
     * @throws BusinessException 为空或不在枚举内。此处**不允许自由新增段落代码**：
     *                           段落代码是"这段文字属于哪一类权责"的归类口径，随手新造会让
     *                           举证时说不清它到底算什么（真要加，走平台侧维护本枚举）。
     */
    public static AgreementNarrativeSection parse(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw BusinessException.badRequest("请选择文字条款的段落类别");
        }
        AgreementNarrativeSection s = of(code);
        if (s == null) {
            throw BusinessException.badRequest("文字条款段落「" + code
                    + "」不支持；可选：DISPUTE（争议解决与管辖）/ CONFIDENTIALITY（保密）/ FORCE_MAJEURE（不可抗力）"
                    + " / SPECIAL_TERMS（特别约定）/ BREACH_LIABILITY_TEXT（违约责任文字表述）");
        }
        return s;
    }
}
