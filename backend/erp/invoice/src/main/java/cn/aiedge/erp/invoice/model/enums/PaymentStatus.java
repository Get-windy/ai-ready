package cn.aiedge.erp.invoice.model.enums;

/**
 * 付款状态枚举
 */
public enum PaymentStatus {
    /**
     * 待付款 - 发票已生成，等待付款
     */
    PENDING("待付款", "Waiting for payment"),
    
    /**
     * 部分付款 - 已支付部分金额
     */
    PARTIALLY_PAID("部分付款", "Partially paid"),
    
    /**
     * 已付款 - 已全额支付
     */
    PAID("已付款", "Fully paid"),
    
    /**
     * 逾期 - 超过付款期限未支付
     */
    OVERDUE("逾期", "Overdue payment"),
    
    /**
     * 已取消 - 付款已取消
     */
    CANCELLED("已取消", "Payment cancelled"),
    
    /**
     * 退款中 - 正在处理退款
     */
    REFUNDING("退款中", "Refunding"),
    
    /**
     * 已退款 - 已全额退款
     */
    REFUNDED("已退款", "Fully refunded"),
    
    /**
     * 部分退款 - 已部分退款
     */
    PARTIALLY_REFUNDED("部分退款", "Partially refunded"),
    
    /**
     * 付款失败 - 付款处理失败
     */
    FAILED("付款失败", "Payment failed"),
    
    /**
     * 付款处理中 - 付款正在处理
     */
    PROCESSING("付款处理中", "Payment processing");
    
    private final String chineseName;
    private final String description;
    
    PaymentStatus(String chineseName, String description) {
        this.chineseName = chineseName;
        this.description = description;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否已支付
     */
    public boolean isPaid() {
        return this == PAID;
    }
    
    /**
     * 判断是否部分支付
     */
    public boolean isPartiallyPaid() {
        return this == PARTIALLY_PAID;
    }
    
    /**
     * 判断是否未支付
     */
    public boolean isUnpaid() {
        return this == PENDING || this == OVERDUE;
    }
    
    /**
     * 判断是否逾期
     */
    public boolean isOverdue() {
        return this == OVERDUE;
    }
    
    /**
     * 判断是否可以接受付款
     */
    public boolean canAcceptPayment() {
        return this == PENDING || this == PARTIALLY_PAID || this == OVERDUE;
    }
    
    /**
     * 判断是否可以退款
     */
    public boolean canRefund() {
        return this == PAID || this == PARTIALLY_PAID;
    }
    
    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == PROCESSING;
    }
    
    /**
     * 获取未支付状态集合
     */
    public static PaymentStatus[] getUnpaidStatuses() {
        return new PaymentStatus[] { PENDING, PARTIALLY_PAID, OVERDUE };
    }
    
    /**
     * 获取已支付状态集合
     */
    public static PaymentStatus[] getPaidStatuses() {
        return new PaymentStatus[] { PAID };
    }
    
    /**
     * 获取逾期状态集合
     */
    public static PaymentStatus[] getOverdueStatuses() {
        return new PaymentStatus[] { OVERDUE };
    }
    
    @Override
    public String toString() {
        return this.name() + " (" + chineseName + ")";
    }
}