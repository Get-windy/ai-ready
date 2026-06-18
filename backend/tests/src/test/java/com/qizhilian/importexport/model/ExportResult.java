package com.qizhilian.importexport.model;

/**
 * 导出结果模型
 */
public class ExportResult {
    private String status;
    private String filePath;
    private long fileSize;
    private int recordCount;
    private int exportedColumnCount;
    private String taskId;
    private long durationMs;
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    
    public int getExportedColumnCount() { return exportedColumnCount; }
    public void setExportedColumnCount(int exportedColumnCount) { this.exportedColumnCount = exportedColumnCount; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
