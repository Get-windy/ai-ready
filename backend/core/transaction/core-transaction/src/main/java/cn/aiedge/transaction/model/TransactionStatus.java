package cn.aiedge.transaction.model;

/**
 * 事务状态枚举
 * 定义分布式事务的各种状态
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public enum TransactionStatus {
    /**
     * 初始状态
     */
    INIT(0, "初始状态"),

    /**
     * 尝试中
     */
    TRYING(1, "尝试中"),

    /**
     * 已确认
     */
    CONFIRMED(2, "已确认"),

    /**
     * 已取消
     */
    CANCELLED(3, "已取消"),

    /**
     * 失败
     */
    FAILED(4, "失败"),

    /**
     * 超时
     */
    TIMEOUT(5, "超时"),

    /**
     * 成功
     */
    SUCCESS(6, "成功"),

    /**
     * 部分成功
     */
    PARTIAL_SUCCESS(7, "部分成功"),

    /**
     * 等待补偿
     */
    WAITING_COMPENSATION(8, "等待补偿"),

    /**
     * 已补偿
     */
    COMPENSATED(9, "已补偿");

    private final Integer code;
    private final String description;

    TransactionStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取事务状态
     * 
     * @param code 状态码
     * @return 事务状态
     */
    public static TransactionStatus fromCode(Integer code) {
        for (TransactionStatus status : TransactionStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid transaction status code: " + code);
    }
}