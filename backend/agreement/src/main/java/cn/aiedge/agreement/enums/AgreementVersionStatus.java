package cn.aiedge.agreement.enums;

/**
 * 协议版本状态（对应 {@code agreement_version.status}，DB 注释同口径）。
 *
 * <p>状态迁移只允许这三条（服务层硬校验，见 {@code AgreementServiceImpl}）：</p>
 * <pre>
 *   DRAFT --双方确认齐 + 必填条款齐--> ACTIVE（同事务把旧 ACTIVE 置 SUPERSEDED）
 *   DRAFT --一方拒绝--------------> REJECTED（**现行版本继续有效，交易照常**）
 *   ACTIVE --新版生效------------> SUPERSEDED
 * </pre>
 */
public enum AgreementVersionStatus {

    /** 0 待对方确认 */
    DRAFT(0, "待双方确认"),

    /** 1 生效（一旦进入本状态，snapshot_json 永久不可改） */
    ACTIVE(1, "生效中"),

    /** 2 已被新版取代（快照留存，可举证） */
    SUPERSEDED(2, "已被新版取代"),

    /** 3 被否决（现行版本继续有效） */
    REJECTED(3, "已否决");

    private final int code;
    private final String label;

    AgreementVersionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AgreementVersionStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgreementVersionStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        AgreementVersionStatus s = of(code);
        return s == null ? "未知" : s.label;
    }
}
