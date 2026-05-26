package com.qizhilian.backup.model;

import java.time.LocalDateTime;

/**
 * 备份结果模型类
 */
public class BackupResult {
    private String backupId;
    private String type;
    private String status;
    private long sizeMb;
    private long durationS;
    private int tablesBackedUp;
    private int changesCount;
    private LocalDateTime timestamp;
    
    public BackupResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public String getBackupId() {
        return backupId;
    }
    
    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getSizeMb() {
        return sizeMb;
    }
    
    public void setSizeMb(long sizeMb) {
        this.sizeMb = sizeMb;
    }
    
    public long getDurationS() {
        return durationS;
    }
    
    public void setDurationS(long durationS) {
        this.durationS = durationS;
    }
    
    public int getTablesBackedUp() {
        return tablesBackedUp;
    }
    
    public void setTablesBackedUp(int tablesBackedUp) {
        this.tablesBackedUp = tablesBackedUp;
    }
    
    public int getChangesCount() {
        return changesCount;
    }
    
    public void setChangesCount(int changesCount) {
        this.changesCount = changesCount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}