package cn.aiedge.erp.printing.enums;

public enum TaskStatus {
    PENDING("PENDING", "待打印"),
    QUEUED("QUEUED", "已入队"),
    PRINTING("PRINTING", "打印中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    RETRYING("RETRYING", "重试中");

    private final String code;
    private final String name;

    TaskStatus(String code, String name) {
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