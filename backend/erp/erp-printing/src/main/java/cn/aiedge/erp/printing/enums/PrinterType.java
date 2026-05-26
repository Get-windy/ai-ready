package cn.aiedge.erp.printing.enums;

public enum PrinterType {
    THERMAL("THERMAL", "热敏打印机"),
    LASER("LASER", "激光打印机"),
    INKJET("INKJET", "喷墨打印机"),
    DOT_MATRIX("DOT_MATRIX", "针式打印机"),
    LABEL("LABEL", "标签打印机"),
    POS("POS", "POS打印机");

    private final String code;
    private final String name;

    PrinterType(String code, String name) {
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