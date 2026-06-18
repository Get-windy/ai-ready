package com.qizhilian.backup.model;

/**
 * 数据完整性验证结果模型类
 */
public class IntegrityResult {
    private String name;
    private boolean passed;
    private String message;
    private long sourceRows;
    private long restoredRows;
    private int tablesCount;
    private int foreignKeysChecked;
    private int violations;
    private String checksumAlgorithm;
    private boolean checksumMatch;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getSourceRows() {
        return sourceRows;
    }
    
    public void setSourceRows(long sourceRows) {
        this.sourceRows = sourceRows;
    }
    
    public long getRestoredRows() {
        return restoredRows;
    }
    
    public void setRestoredRows(long restoredRows) {
        this.restoredRows = restoredRows;
    }
    
    public int getTablesCount() {
        return tablesCount;
    }
    
    public void setTablesCount(int tablesCount) {
        this.tablesCount = tablesCount;
    }
    
    public int getForeignKeysChecked() {
        return foreignKeysChecked;
    }
    
    public void setForeignKeysChecked(int foreignKeysChecked) {
        this.foreignKeysChecked = foreignKeysChecked;
    }
    
    public int getViolations() {
        return violations;
    }
    
    public void setViolations(int violations) {
        this.violations = violations;
    }
    
    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }
    
    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }
    
    public boolean isChecksumMatch() {
        return checksumMatch;
    }
    
    public void setChecksumMatch(boolean checksumMatch) {
        this.checksumMatch = checksumMatch;
    }
}