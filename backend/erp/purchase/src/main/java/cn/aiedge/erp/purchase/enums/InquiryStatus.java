package cn.aiedge.erp.purchase.enums;

/**
 * 询价单状态枚举
 */
public enum InquiryStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    QUOTING("报价中"),
    DECISION_MADE("已决策"),
    CONTRACT_CREATED("已生成合同"),
    CLOSED("已关闭"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}