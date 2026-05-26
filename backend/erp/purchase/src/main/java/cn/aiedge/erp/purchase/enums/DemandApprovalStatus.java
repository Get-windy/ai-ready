package cn.aiedge.erp.purchase.enums;

import lombok.Getter;

/**
 * 采购需求审批状态枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum DemandApprovalStatus {
    
    /**
     * 待审批
     */
    PENDING(1, "待审批"),
    
    /**
     * 审批中
     */
    REVIEWING(2, "审批中"),
    
    /**
     * 已批准
     */
    APPROVED(3, "已批准"),
    
    /**
     * 已拒绝
     */
    REJECTED(4, "已拒绝"),
    
    /**
     * 已撤回
     */
    WITHDRAWN(5, "已撤回");
    
    private final int value;
    private final String description;
    
    DemandApprovalStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    /**
     * 根据值获取枚举
     */
    public static DemandApprovalStatus fromValue(int value) {
        for (DemandApprovalStatus status : DemandApprovalStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的审批状态值: " + value);
    }
    
    /**
     * 检查是否为待审批状态
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * 检查是否为审批中状态
     */
    public boolean isReviewing() {
        return this == REVIEWING;
    }
    
    /**
     * 检查是否为已批准状态
     */
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    /**
     * 检查是否为已拒绝状态
     */
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    /**
     * 检查是否可以提交审批
     */
    public boolean canSubmitForApproval() {
        return this == PENDING || this == WITHDRAWN;
    }
    
    /**
     * 检查是否可以撤回
     */
    public boolean canWithdraw() {
        return this == PENDING || this == REVIEWING;
    }
}