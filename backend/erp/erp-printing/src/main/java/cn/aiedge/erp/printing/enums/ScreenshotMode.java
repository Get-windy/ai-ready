package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum ScreenshotMode {
    DISABLED("DISABLED", "关闭截图"),
    MANUAL_CONFIRM("MANUAL_CONFIRM", "手动确认"),
    AUTO_CONFIRM("AUTO_CONFIRM", "超时自动确认");

    private final String code;
    private final String name;

    ScreenshotMode(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
