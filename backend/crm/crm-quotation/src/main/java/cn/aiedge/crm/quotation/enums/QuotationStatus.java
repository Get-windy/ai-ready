package cn.aiedge.crm.quotation.enums;

public enum QuotationStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    SENT(3, "已发送"),
    ACCEPTED(4, "已接受"),
    REJECTED(5, "已拒绝"),
    EXPIRED(6, "已过期"),
    CONVERTED(7, "已转订单"),
    CANCELLED(8, "已取消");

    private final Integer code;
    private final String desc;

    QuotationStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}