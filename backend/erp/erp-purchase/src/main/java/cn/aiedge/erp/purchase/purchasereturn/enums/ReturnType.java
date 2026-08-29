package cn.aiedge.erp.purchase.purchasereturn.enums;

/**
 * 采购退货类型
 */
public enum ReturnType {
    QUALITY(1, "质量退货"),
    QUANTITY(2, "数量退货"),
    OTHER(3, "其他退货");

    private final Integer code;
    private final String desc;

    ReturnType(Integer code, String desc) {
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
