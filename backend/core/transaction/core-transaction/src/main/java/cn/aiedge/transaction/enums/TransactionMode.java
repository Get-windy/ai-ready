package cn.aiedge.transaction.enums;

/**
 * 事务模式枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public enum TransactionMode {
    /**
     * TCC模式：Try-Confirm-Cancel
     */
    TCC("TCC", "Try-Confirm-Cancel模式"),

    /**
     * Saga模式：长事务拆分成多个短事务
     */
    SAGA("SAGA", "Saga模式"),

    /**
     * 最大努力通知模式
     */
    BEST_EFFORT("BEST_EFFORT", "最大努力通知模式");

    private final String code;
    private final String description;

    TransactionMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取事务模式
     * 
     * @param code 代码
     * @return 事务模式
     */
    public static TransactionMode fromCode(String code) {
        for (TransactionMode mode : TransactionMode.values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid transaction mode code: " + code);
    }
}