package cn.aiedge.agreement.enums;

/**
 * 终止记录状态 —— 对应 {@code agreement_termination.status}。
 *
 * <pre>
 *   MUTUAL_AGREEMENT（协商一致）：PENDING --对方确认--> CONFIRMED
 *                                        --对方异议--> OBJECTED（**协议不终止**）
 *                                        --发起方撤回--> WITHDRAWN
 *   其余四种来源（单方终止 / 因对方违约 / 平台清退 / 自然到期）：**发起即 CONFIRMED**
 *        —— 因为"停止履行"这个事实已经发生，系统不假装它没发生；
 *        对方的异议只作为留痕（counterparty_objection），**不回滚终止事实**。
 * </pre>
 */
public enum AgreementTerminationStatus {

    /** 0 待对方确认（只有"协商一致"会停在这里） */
    PENDING(0, "待对方确认"),

    /** 1 已终止：**停止履行**已生效（⚠️ 不等于责任已了结） */
    CONFIRMED(1, "已终止（停止履行已生效）"),

    /** 2 对方有异议：协商一致的终止被对方否掉，协议**继续有效** */
    OBJECTED(2, "对方有异议"),

    /** 3 已撤回：发起方在对方表态前收回 */
    WITHDRAWN(3, "已撤回");

    private final int code;
    private final String label;

    AgreementTerminationStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AgreementTerminationStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgreementTerminationStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        AgreementTerminationStatus s = of(code);
        return s == null ? "未知" : s.label;
    }
}
