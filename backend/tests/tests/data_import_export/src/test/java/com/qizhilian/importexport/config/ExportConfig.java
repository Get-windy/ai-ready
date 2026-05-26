package com.qizhilian.importexport.config;

import com.qizhilian.importexport.model.ExportColumn;
import java.util.List;

/**
 * 导出配置类
 */
public class ExportConfig {
    private String fileName;
    private String sheetName = "Sheet1";
    private String format = "xlsx";
    private String encoding = "UTF-8";
    private List<String> columns;
    private List<ExportColumn> customColumns;
    private ExportFilter filter;
    private boolean enablePagination = false;
    private int pageSize = 1000;
    private boolean async = false;
    private String callbackUrl;
    private String templateType;
    private String rootElement;
    private String itemElement;
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ExportConfig config = new ExportConfig();
        
        public Builder fileName(String fileName) {
            config.fileName = fileName;
            return this;
        }
        
        public Builder sheetName(String sheetName) {
            config.sheetName = sheetName;
            return this;
        }
        
        public Builder format(String format) {
            config.format = format;
            return this;
        }
        
        public Builder encoding(String encoding) {
            config.encoding = encoding;
            return this;
        }
        
        public Builder columns(List<String> columns) {
            config.columns = columns;
            return this;
        }
        
        public Builder customColumns(List<ExportColumn> columns) {
            config.customColumns = columns;
            return this;
        }
        
        public Builder filter(ExportFilter filter) {
            config.filter = filter;
            return this;
        }
        
        public Builder enablePagination(boolean enable) {
            config.enablePagination = enable;
            return this;
        }
        
        public Builder pageSize(int size) {
            config.pageSize = size;
            return this;
        }
        
        public Builder async(boolean async) {
            config.async = async;
            return this;
        }
        
        public Builder callbackUrl(String url) {
            config.callbackUrl = url;
            return this;
        }
        
        public Builder templateType(String type) {
            config.templateType = type;
            return this;
        }
        
        public Builder rootElement(String element) {
            config.rootElement = element;
            return this;
        }
        
        public Builder itemElement(String element) {
            config.itemElement = element;
            return this;
        }
        
        public ExportConfig build() {
            return config;
        }
    }
    
    // Getters
    public String getFileName() { return fileName; }
    public String getSheetName() { return sheetName; }
    public String getFormat() { return format; }
    public String getEncoding() { return encoding; }
    public List<String> getColumns() { return columns; }
    public List<ExportColumn> getCustomColumns() { return customColumns; }
    public ExportFilter getFilter() { return filter; }
    public boolean isEnablePagination() { return enablePagination; }
    public int getPageSize() { return pageSize; }
    public boolean isAsync() { return async; }
    public String getCallbackUrl() { return callbackUrl; }
    public String getTemplateType() { return templateType; }
    public String getRootElement() { return rootElement; }
    public String getItemElement() { return itemElement; }
}
