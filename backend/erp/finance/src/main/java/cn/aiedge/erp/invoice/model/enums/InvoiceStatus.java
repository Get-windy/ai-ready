package cn.aiedge.erp.invoice.model.enums;

/**
 * 发票状态枚举
 */
public enum InvoiceStatus {
    /**
     * 草稿状态 - 发票申请已创建但未提交
     */
    DRAFT("草稿", "Invoice application created but not submitted"),
    
    /**
     * 已提交 - 发票申请已提交等待审批
     */
    SUBMITTED("已提交", "Invoice application submitted for approval"),
    
    /**
     * 审批中 - 正在审批过程中
     */
    IN_APPROVAL("审批中", "Invoice application is under approval"),
    
    /**
     * 已批准 - 发票申请已批准
     */
    APPROVED("已批准", "Invoice application approved"),
    
    /**
     * 已拒绝 - 发票申请被拒绝
     */
    REJECTED("已拒绝", "Invoice application rejected"),
    
    /**
     * 已生成 - 发票已生成
     */
    GENERATED("已生成", "Invoice generated"),
    
    /**
     * 已发送 - 发票已发送给客户
     */
    SENT("已发送", "Invoice sent to customer"),
    
    /**
     * 部分支付 - 发票部分支付
     */
    PARTIALLY_PAID("部分支付", "Invoice partially paid"),
    
    /**
     * 已支付 - 发票已全额支付
     */
    PAID("已支付", "Invoice fully paid"),
    
    /**
     * 逾期 - 发票已逾期未支付
     */
    OVERDUE("逾期", "Invoice overdue"),
    
    /**
     * 已取消 - 发票已取消
     */
    CANCELLED("已取消", "Invoice cancelled"),
    
    /**
     * 已冲红 - 发票已冲红
     */
    CREDITED("已冲红", "Invoice credited"),
    
    /**
     * 已作废 - 发票已作废
     */
    VOIDED("已作废", "Invoice voided");
    
    private final String chineseName;
    private final String description;
    
    InvoiceStatus(String chineseName, String description) {
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
     * 判断状态是否可以提交审批
     */
    public boolean canSubmit() {
        return this == DRAFT;
    }
    
    /**
     * 判断状态是否可以审批
     */
    public boolean canApprove() {
        return this == SUBMITTED || this == IN_APPROVAL;
    }
    
    /**
     * 判断状态是否可以生成发票
     */
    public boolean canGenerateInvoice() {
        return this == APPROVED;
    }
    
    /**
     * 判断状态是否可以支付
     */
    public boolean canPay() {
        return this == GENERATED || this == SENT || this == PARTIALLY_PAID;
    }
    
    /**
     * 判断状态是否可以取消
     */
    public boolean canCancel() {
        return this == DRAFT || this == SUBMITTED || this == IN_APPROVAL || this == APPROVED;
    }
    
    /**
     * 获取工作流状态集合
     */
    public static InvoiceStatus[] getWorkflowStatuses() {
        return new InvoiceStatus[] { DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, REJECTED };
    }
    
    /**
     * 获取发票生命周期状态集合
     */
    public static InvoiceStatus[] getLifecycleStatuses() {
        return new InvoiceStatus[] { GENERATED, SENT, PARTIALLY_PAID, PAID, OVERDUE };
    }
    
    /**
     * 获取终止状态集合
     */
    public static InvoiceStatus[] getTerminalStatuses() {
        return new InvoiceStatus[] { CANCELLED, CREDITED, VOIDED };
    }
    
    @Override
    public String toString() {
        return this.name() + " (" + chineseName + ")";
    }
}