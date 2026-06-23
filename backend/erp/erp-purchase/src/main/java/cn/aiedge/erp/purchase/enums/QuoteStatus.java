package cn.aiedge.erp.purchase.enums;

/**
 * 报价单状态枚举
 */
public enum QuoteStatus {
    SUBMITTED("已提交"),
    REVIEWED("已审查"),
    ACCEPTED("已接受"),
    REJECTED("已拒绝"),
    WITHDRAWN("已撤回");

    private final String description;

    QuoteStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}