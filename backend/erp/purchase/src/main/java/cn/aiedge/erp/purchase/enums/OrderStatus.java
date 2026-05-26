package cn.aiedge.erp.purchase.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    ISSUED(3, "已下达"),
    IN_PROGRESS(4, "执行中"),
    PARTIAL_RECEIVED(5, "部分入库"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消");

    private final int value;
    private final String description;

    OrderStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromValue(int value) {
        for (OrderStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态: " + value);
    }
}