package cn.aiedge.erp.sale.return.enums;

public enum ReturnType {
    QUALITY(1, "质量问题"),
    WRONG_PRODUCT(2, "错发商品"),
    DAMAGE(3, "商品损坏"),
    NOT_MATCH(4, "与描述不符"),
    CUSTOMER_CHANGE(5, "客户变更"),
    OTHER(6, "其他原因");

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