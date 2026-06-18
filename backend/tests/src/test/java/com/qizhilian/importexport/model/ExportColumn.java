package com.qizhilian.importexport.model;

/**
 * 导出列定义
 */
public class ExportColumn {
    private String field;
    private String header;
    private int width;
    private String format;
    
    public ExportColumn(String field, String header, int width) {
        this.field = field;
        this.header = header;
        this.width = width;
    }
    
    public ExportColumn(String field, String header, int width, String format) {
        this.field = field;
        this.header = header;
        this.width = width;
        this.format = format;
    }
    
    // Getters
    public String getField() { return field; }
    public String getHeader() { return header; }
    public int getWidth() { return width; }
    public String getFormat() { return format; }
}
