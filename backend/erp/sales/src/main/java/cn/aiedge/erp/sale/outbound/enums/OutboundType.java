package cn.aiedge.erp.sale.outbound.enums;

public enum OutboundType {
    SALE_ORDER(1, "销售订单出库"),
    SALE_EXCHANGE(2, "销售换货出库"),
    TRANSFER_OUT(3, "调拨出库"),
    OTHER(4, "其他出库");

    private final Integer code;
    private final String desc;

    OutboundType(Integer code, String desc) {
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