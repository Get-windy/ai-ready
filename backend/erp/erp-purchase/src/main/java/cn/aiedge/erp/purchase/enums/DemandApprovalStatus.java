package cn.aiedge.erp.purchase.enums;

public enum DemandApprovalStatus {
    
    PENDING(1, "待审批"),
    REVIEWING(2, "审批中"),
    APPROVED(3, "已批准"),
    REJECTED(4, "已拒绝"),
    WITHDRAWN(5, "已撤回");
    
    private final int value;
    private final String description;
    
    DemandApprovalStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static DemandApprovalStatus fromValue(int value) {
        for (DemandApprovalStatus status : DemandApprovalStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的审批状态值: " + value);
    }
    
    public boolean isPending() {
        return this == PENDING;
    }
    
    public boolean isReviewing() {
        return this == REVIEWING;
    }
    
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    public boolean canSubmitForApproval() {
        return this == PENDING || this == WITHDRAWN;
    }
    
    public boolean canWithdraw() {
        return this == PENDING || this == REVIEWING;
    }
}
