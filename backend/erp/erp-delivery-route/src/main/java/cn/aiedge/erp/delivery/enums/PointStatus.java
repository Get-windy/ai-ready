package cn.aiedge.erp.delivery.enums;

public enum PointStatus {
    PENDING("PENDING", "待配送"),
    IN_ROUTE("IN_ROUTE", "在途中"),
    ARRIVED("ARRIVED", "已到达"),
    DELIVERED("DELIVERED", "已送达"),
    FAILED("FAILED", "配送失败"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String name;

    PointStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}