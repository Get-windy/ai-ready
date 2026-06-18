package com.qizhilian.backup.model;

import java.time.LocalDateTime;

/**
 * 恢复结果模型类
 */
public class RestoreResult {
    private String restoreId;
    private String backupId;
    private String status;
    private long durationS;
    private int tablesRestored;
    private LocalDateTime targetTime;
    private LocalDateTime timestamp;
    
    public RestoreResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public String getRestoreId() {
        return restoreId;
    }
    
    public void setRestoreId(String restoreId) {
        this.restoreId = restoreId;
    }
    
    public String getBackupId() {
        return backupId;
    }
    
    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getDurationS() {
        return durationS;
    }
    
    public void setDurationS(long durationS) {
        this.durationS = durationS;
    }
    
    public int getTablesRestored() {
        return tablesRestored;
    }
    
    public void setTablesRestored(int tablesRestored) {
        this.tablesRestored = tablesRestored;
    }
    
    public LocalDateTime getTargetTime() {
        return targetTime;
    }
    
    public void setTargetTime(LocalDateTime targetTime) {
        this.targetTime = targetTime;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}