package cn.aiedge.erp.purchase.inbound.enums;

public enum InboundType {
    PURCHASE_ORDER(1, "采购订单入库"),
    PURCHASE_RETURN(2, "采购退货入库"),
    SALE_RETURN(3, "销售退货入库"),
    TRANSFER_IN(4, "调拨入库"),
    OTHER(5, "其他入库");

    private final Integer code;
    private final String desc;

    InboundType(Integer code, String desc) {
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