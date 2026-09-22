package cn.aiedge.agreement.enums;

/**
 * 协议主档状态（对应 {@code agreement.status}，DB 注释同口径）。
 */
public enum AgreementStatus {

    /** 0 洽谈中 */
    DRAFT(0, "洽谈中"),

    /** 1 生效 */
    ACTIVE(1, "生效中"),

    /** 2 暂停 */
    SUSPENDED(2, "已暂停"),

    /** 3 终止 */
    TERMINATED(3, "已终止");

    private final int code;
    private final String label;

    AgreementStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AgreementStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgreementStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        AgreementStatus s = of(code);
        return s == null ? "未知" : s.label;
    }
}
