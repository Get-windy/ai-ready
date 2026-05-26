package cn.aiedge.erp.sale.return.enums;

public enum ReturnStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    REJECTED(3, "已拒绝"),
    PENDING_RECEIVE(4, "待收货"),
    RECEIVED(5, "已收货"),
    PENDING_WAREHOUSE(6, "待入库"),
    WAREHOUSE_CONFIRMED(7, "已入库"),
    PENDING_REFUND(8, "待退款"),
    REFUND_PROCESSING(9, "退款中"),
    REFUNDED(10, "已退款"),
    COMPLETED(11, "已完成"),
    CANCELLED(12, "已取消");

    private final Integer code;
    private final String desc;

    ReturnStatus(Integer code, String desc) {
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