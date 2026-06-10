package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum PaperSize {
    A4("A4", "A4"),
    A5("A5", "A5"),
    CUSTOM("CUSTOM", "自定义");

    private final String code;
    private final String name;

    PaperSize(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
