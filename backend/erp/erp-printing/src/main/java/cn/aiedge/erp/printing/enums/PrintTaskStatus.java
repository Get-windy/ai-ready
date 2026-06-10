package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum PrintTaskStatus {
    PENDING("PENDING", "待打印"),
    QUEUED("QUEUED", "已入队"),
    PRINTING("PRINTING", "打印中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),

    private final String code;
    private final String name;

    PrintTaskStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
