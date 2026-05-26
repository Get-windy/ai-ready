package cn.aiedge.erp.delivery.enums;

public enum RouteStatus {
    PLANNING("PLANNING", "规划中"),
    READY("READY", "待出发"),
    IN_PROGRESS("IN_PROGRESS", "配送中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String name;

    RouteStatus(String code, String name) {
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