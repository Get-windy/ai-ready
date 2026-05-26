package cn.aiedge.erp.purchase.inbound.enums;

public enum InboundStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    PENDING_RECEIVE(3, "待收货"),
    RECEIVED(4, "已收货"),
    PENDING_QUALITY_CHECK(5, "待质检"),
    QUALITY_CHECKED(6, "已质检"),
    PENDING_WAREHOUSE(7, "待入库"),
    WAREHOUSE_CONFIRMED(8, "已入库"),
    COMPLETED(9, "已完成"),
    CANCELLED(10, "已取消");

    private final Integer code;
    private final String desc;

    InboundStatus(Integer code, String desc) {
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