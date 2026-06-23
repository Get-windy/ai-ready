package cn.aiedge.erp.expense.model.enumeration;

/**
 * 费用单状态枚举
 */
public enum ExpenseStatus {
    /**
     * 草稿状态
     */
    DRAFT("草稿", 0),
    
    /**
     * 已提交，等待审批
     */
    SUBMITTED("已提交", 1),
    
    /**
     * 部门经理审批中
     */
    DEPARTMENT_APPROVING("部门审批中", 2),
    
    /**
     * 财务审批中
     */
    FINANCE_APPROVING("财务审批中", 3),
    
    /**
     * 总经理审批中
     */
    GENERAL_MANAGER_APPROVING("总经理审批中", 4),
    
    /**
     * 审批通过
     */
    APPROVED("审批通过", 5),
    
    /**
     * 审批拒绝
     */
    REJECTED("审批拒绝", 6),
    
    /**
     * 已支付
     */
    PAID("已支付", 7),
    
    /**
     * 已报销
     */
    REIMBURSED("已报销", 8),
    
    /**
     * 已取消
     */
    CANCELLED("已取消", 9);
    
    private final String description;
    private final int code;
    
    ExpenseStatus(String description, int code) {
        this.description = description;
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getCode() {
        return code;
    }
    
    /**
     * 根据code获取枚举
     */
    public static ExpenseStatus fromCode(int code) {
        for (ExpenseStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return DRAFT;
    }
    
    /**
     * 是否可以提交审批
     */
    public boolean canSubmit() {
        return this == DRAFT;
    }
    
    /**
     * 是否可以审批
     */
    public boolean canApprove() {
        return this == SUBMITTED || this == DEPARTMENT_APPROVING || 
               this == FINANCE_APPROVING || this == GENERAL_MANAGER_APPROVING;
    }
    
    /**
     * 是否可以支付
     */
    public boolean canPay() {
        return this == APPROVED;
    }
    
    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == DRAFT || this == SUBMITTED || 
               this == DEPARTMENT_APPROVING || this == FINANCE_APPROVING || 
               this == GENERAL_MANAGER_APPROVING;
    }
    
    /**
     * 是否是终态
     */
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || 
               this == PAID || this == REIMBURSED || this == CANCELLED;
    }
}