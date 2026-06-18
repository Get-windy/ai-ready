package cn.aiedge.erp.sales.pricing.enums;

/**
 * 价格审批状态枚举
 */
public enum PriceApprovalStatus {
    
    /**
     * 草稿 - 正在创建，还未提交审批
     */
    DRAFT("draft", "草稿"),
    
    /**
     * 待审批 - 已提交，等待审批
     */
    PENDING_APPROVAL("pending_approval", "待审批"),
    
    /**
     * 审批中 - 审批流程正在进行中
     */
    UNDER_REVIEW("under_review", "审批中"),
    
    /**
     * 一级审批通过 - 第一级审批已通过
     */
    LEVEL_1_APPROVED("level_1_approved", "一级审批通过"),
    
    /**
     * 二级审批通过 - 第二级审批已通过
     */
    LEVEL_2_APPROVED("level_2_approved", "二级审批通过"),
    
    /**
     * 三级审批通过 - 第三级审批已通过
     */
    LEVEL_3_APPROVED("level_3_approved", "三级审批通过"),
    
    /**
     * 审批通过 - 所有级别审批已通过
     */
    APPROVED("approved", "审批通过"),
    
    /**
     * 一级审批拒绝 - 第一级审批被拒绝
     */
    LEVEL_1_REJECTED("level_1_rejected", "一级审批拒绝"),
    
    /**
     * 二级审批拒绝 - 第二级审批被拒绝
     */
    LEVEL_2_REJECTED("level_2_rejected", "二级审批拒绝"),
    
    /**
     * 三级审批拒绝 - 第三级审批被拒绝
     */
    LEVEL_3_REJECTED("level_3_rejected", "三级审批拒绝"),
    
    /**
     * 审批拒绝 - 最终审批被拒绝
     */
    REJECTED("rejected", "审批拒绝"),
    
    /**
     * 审批撤回 - 申请人撤回了审批请求
     */
    WITHDRAWN("withdrawn", "审批撤回"),
    
    /**
     * 审批过期 - 审批请求已过期
     */
    EXPIRED("expired", "审批过期"),
    
    /**
     * 已取消 - 审批被取消
     */
    CANCELLED("cancelled", "已取消");
    
    private final String code;
    private final String description;
    
    PriceApprovalStatus(String code, String description) {
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
     * 根据编码获取枚举值
     */
    public static PriceApprovalStatus fromCode(String code) {
        for (PriceApprovalStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的价格审批状态编码: " + code);
    }
    
    /**
     * 是否为待处理状态（可继续审批流程）
     */
    public boolean isPending() {
        return this == PENDING_APPROVAL || this == UNDER_REVIEW;
    }
    
    /**
     * 是否为进行中状态（审批流程未结束）
     */
    public boolean isInProgress() {
        return this == DRAFT || this == PENDING_APPROVAL || this == UNDER_REVIEW ||
               this == LEVEL_1_APPROVED || this == LEVEL_2_APPROVED;
    }
    
    /**
     * 是否为已完成状态（审批流程已结束）
     */
    public boolean isCompleted() {
        return this == APPROVED || this == REJECTED || this == WITHDRAWN ||
               this == EXPIRED || this == CANCELLED;
    }
    
    /**
     * 是否为审批通过状态
     */
    public boolean isApproved() {
        return this == LEVEL_1_APPROVED || this == LEVEL_2_APPROVED || 
               this == LEVEL_3_APPROVED || this == APPROVED;
    }
    
    /**
     * 是否为审批拒绝状态
     */
    public boolean isRejected() {
        return this == LEVEL_1_REJECTED || this == LEVEL_2_REJECTED || 
               this == LEVEL_3_REJECTED || this == REJECTED;
    }
    
    /**
     * 是否为可提交审批的状态
     */
    public boolean canSubmit() {
        return this == DRAFT;
    }
    
    /**
     * 是否为可撤销的状态
     */
    public boolean canWithdraw() {
        return this == PENDING_APPROVAL || this == UNDER_REVIEW || 
               this == LEVEL_1_APPROVED || this == LEVEL_2_APPROVED;
    }
    
    /**
     * 是否为可取消的状态
     */
    public boolean canCancel() {
        return this == DRAFT || this == PENDING_APPROVAL || 
               this == UNDER_REVIEW || this == EXPIRED;
    }
    
    /**
     * 获取下一级审批状态（如果审批通过）
     */
    public PriceApprovalStatus getNextApprovalLevel() {
        switch (this) {
            case PENDING_APPROVAL:
                return LEVEL_1_APPROVED;
            case LEVEL_1_APPROVED:
                return LEVEL_2_APPROVED;
            case LEVEL_2_APPROVED:
                return LEVEL_3_APPROVED;
            case LEVEL_3_APPROVED:
                return APPROVED;
            default:
                return this;
        }
    }
    
    /**
     * 获取当前审批级别
     */
    public int getApprovalLevel() {
        switch (this) {
            case LEVEL_1_APPROVED:
            case LEVEL_1_REJECTED:
                return 1;
            case LEVEL_2_APPROVED:
            case LEVEL_2_REJECTED:
                return 2;
            case LEVEL_3_APPROVED:
            case LEVEL_3_REJECTED:
                return 3;
            case APPROVED:
            case REJECTED:
                return 4; // 最终级别
            default:
                return 0;
        }
    }
    
    /**
     * 获取审批失败的状态
     */
    public PriceApprovalStatus getRejectionStatus(int level) {
        switch (level) {
            case 1:
                return LEVEL_1_REJECTED;
            case 2:
                return LEVEL_2_REJECTED;
            case 3:
                return LEVEL_3_REJECTED;
            default:
                return REJECTED;
        }
    }
    
    /**
     * 验证是否为有效的审批状态转换
     */
    public static boolean isValidTransition(PriceApprovalStatus from, PriceApprovalStatus to) {
        // 状态转换规则
        switch (from) {
            case DRAFT:
                return to == PENDING_APPROVAL || to == CANCELLED;
            case PENDING_APPROVAL:
                return to == UNDER_REVIEW || to == WITHDRAWN || to == EXPIRED;
            case UNDER_REVIEW:
                return to == LEVEL_1_APPROVED || to == LEVEL_1_REJECTED || to == WITHDRAWN;
            case LEVEL_1_APPROVED:
                return to == LEVEL_2_APPROVED || to == LEVEL_2_REJECTED || to == WITHDRAWN;
            case LEVEL_2_APPROVED:
                return to == LEVEL_3_APPROVED || to == LEVEL_3_REJECTED || to == WITHDRAWN;
            case LEVEL_3_APPROVED:
                return to == APPROVED || to == WITHDRAWN;
            case LEVEL_1_REJECTED:
            case LEVEL_2_REJECTED:
            case LEVEL_3_REJECTED:
                return to == REJECTED || to == WITHDRAWN;
            default:
                return false;
        }
    }
}