package cn.aiedge.erp.stock.enums;

public enum AlertType {
    LOW_STOCK(1, "低库存预警"),
    OVER_STOCK(2, "超库存预警"),
    EXPIRY(3, "效期预警"),
    ALL(4, "全部预警");

    private final Integer code;
    private final String desc;

    AlertType(Integer code, String desc) {
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