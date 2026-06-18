package cn.aiedge.erp.product.kit.enums;

public enum KitType {
    COMBO(1, "组合套装"),
    BUNDLE(2, "捆绑套装"),
    GIFT(3, "礼品套装"),
    ASSEMBLY(4, "组装产品"),
    SPLIT(5, "拆分产品");

    private final Integer code;
    private final String desc;

    KitType(Integer code, String desc) {
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