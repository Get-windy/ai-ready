package cn.aiedge.erp.payment.enums;

public enum ReceiptStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    REJECTED(3, "已拒绝"),
    PENDING_VERIFY(4, "待核销"),
    VERIFYING(5, "核销中"),
    VERIFIED(6, "已核销"),
    COMPLETED(7, "已完成"),
    CANCELLED(8, "已取消");

    private final Integer code;
    private final String desc;

    ReceiptStatus(Integer code, String desc) {
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