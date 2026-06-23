package cn.aiedge.erp.purchase.enums;

/**
 * 合同状态枚举
 */
public enum ContractStatus {
    
    DRAFT("草稿"),
    PENDING_APPROVAL("待审批"),
    APPROVED("已审批"),
    ACTIVE("生效中"),
    COMPLETED("已完成"),
    REJECTED("已驳回"),
    TERMINATED("已终止"),
    ARCHIVED("已归档");

    private final String description;

    ContractStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}