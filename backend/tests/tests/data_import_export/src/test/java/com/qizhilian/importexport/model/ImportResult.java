package com.qizhilian.importexport.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果模型
 */
public class ImportResult {
    private String status;
    private int importedCount;
    private int errorCount;
    private int totalCount;
    private int sheetCount;
    private int batchCount;
    private List<String> errors = new ArrayList<>();
    private long durationMs;
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
    
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    
    public int getSheetCount() { return sheetCount; }
    public void setSheetCount(int sheetCount) { this.sheetCount = sheetCount; }
    
    public int getBatchCount() { return batchCount; }
    public void setBatchCount(int batchCount) { this.batchCount = batchCount; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
