package cn.aiedge.erp.printing.enums;

public enum PrinterStatus {
    ONLINE("ONLINE", "在线"),
    OFFLINE("OFFLINE", "离线"),
    BUSY("BUSY", "忙碌"),
    ERROR("ERROR", "故障"),
    MAINTENANCE("MAINTENANCE", "维护中");

    private final String code;
    private final String name;

    PrinterStatus(String code, String name) {
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