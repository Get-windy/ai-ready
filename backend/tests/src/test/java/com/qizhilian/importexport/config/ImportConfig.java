package com.qizhilian.importexport.config;

import java.util.List;

/**
 * 导入配置类
 */
public class ImportConfig {
    private boolean multiSheet = false;
    private boolean enableValidation = false;
    private boolean enableFormatValidation = false;
    private boolean checkDuplicates = false;
    private boolean enableBatchProcessing = false;
    private int batchSize = 100;
    private boolean supportNested = false;
    private boolean isApiImport = false;
    private List<String> requiredFields;
    private List<String> uniqueFields;
    
    // Builder pattern
    public static ImportConfig singleSheet() {
        ImportConfig config = new ImportConfig();
        config.multiSheet = false;
        return config;
    }
    
    public static ImportConfig multiSheet() {
        ImportConfig config = new ImportConfig();
        config.multiSheet = true;
        return config;
    }
    
    public static ImportConfig largeData() {
        ImportConfig config = new ImportConfig();
        config.enableBatchProcessing = true;
        config.batchSize = 1000;
        return config;
    }
    
    public static ImportConfig withValidation() {
        ImportConfig config = new ImportConfig();
        config.enableValidation = true;
        return config;
    }
    
    public static ImportConfig withFormatValidation() {
        ImportConfig config = new ImportConfig();
        config.enableValidation = true;
        config.enableFormatValidation = true;
        return config;
    }
    
    public static ImportConfig withDuplicateCheck() {
        ImportConfig config = new ImportConfig();
        config.checkDuplicates = true;
        return config;
    }
    
    public static ImportConfig withNestedSupport() {
        ImportConfig config = new ImportConfig();
        config.supportNested = true;
        return config;
    }
    
    public static ImportConfig apiImport() {
        ImportConfig config = new ImportConfig();
        config.isApiImport = true;
        return config;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Builder class
    public static class Builder {
        private ImportConfig config = new ImportConfig();
        
        public Builder batchSize(int size) {
            config.batchSize = size;
            return this;
        }
        
        public Builder enableBatchProcessing(boolean enable) {
            config.enableBatchProcessing = enable;
            return this;
        }
        
        public Builder requiredFields(List<String> fields) {
            config.requiredFields = fields;
            return this;
        }
        
        public ImportConfig build() {
            return config;
        }
    }
    
    // Getters
    public boolean isMultiSheet() { return multiSheet; }
    public boolean isEnableValidation() { return enableValidation; }
    public boolean isEnableFormatValidation() { return enableFormatValidation; }
    public boolean isCheckDuplicates() { return checkDuplicates; }
    public boolean isEnableBatchProcessing() { return enableBatchProcessing; }
    public int getBatchSize() { return batchSize; }
    public boolean isSupportNested() { return supportNested; }
    public boolean isApiImport() { return isApiImport; }
    public List<String> getRequiredFields() { return requiredFields; }
    public List<String> getUniqueFields() { return uniqueFields; }
}
