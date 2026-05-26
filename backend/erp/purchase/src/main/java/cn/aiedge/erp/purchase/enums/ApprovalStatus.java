package cn.aiedge.erp.purchase.enums;

/**
 * 审批状态枚举
 */
public enum ApprovalStatus {
    PENDING("待审批"),
    APPROVED("已批准"),
    REJECTED("已拒绝");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}