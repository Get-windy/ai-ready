package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum ScreenshotStatus {
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String name;

    ScreenshotStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
