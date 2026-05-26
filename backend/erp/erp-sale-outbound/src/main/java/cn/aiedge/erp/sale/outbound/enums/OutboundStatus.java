package cn.aiedge.erp.sale.outbound.enums;

public enum OutboundStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    PENDING_PICKING(3, "待拣货"),
    PICKING(4, "拣货中"),
    PICKED(5, "已拣货"),
    PENDING_PACKING(6, "待打包"),
    PACKING(7, "打包中"),
    PACKED(8, "已打包"),
    PENDING_SHIP(9, "待发货"),
    SHIPPED(10, "已发货"),
    COMPLETED(11, "已完成"),
    CANCELLED(12, "已取消");

    private final Integer code;
    private final String desc;

    OutboundStatus(Integer code, String desc) {
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