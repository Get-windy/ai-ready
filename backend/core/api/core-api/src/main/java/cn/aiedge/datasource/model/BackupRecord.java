package cn.aiedge.datasource.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备份记录
 */
@Schema(description = "备份记录")
@TableName("sys_backup_record")
public class BackupRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "备份ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "数据源ID")
    private Long dataSourceId;

    @Schema(description = "备份名称")
    private String backupName;

    @Schema(description = "备份类型: full/incremental")
    private String backupType;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "状态: running/success/failed")
    private String status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "最近一次恢复状态: none/dispatched/success/failed")
    private String restoreStatus;

    @Schema(description = "最近一次恢复时间")
    private LocalDateTime restoreTime;

    @Schema(description = "最近一次恢复结果摘要或失败原因")
    private String restoreMessage;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String createBy;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getBackupName() { return backupName; }
    public void setBackupName(String backupName) { this.backupName = backupName; }
    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRestoreStatus() { return restoreStatus; }
    public void setRestoreStatus(String restoreStatus) { this.restoreStatus = restoreStatus; }
    public LocalDateTime getRestoreTime() { return restoreTime; }
    public void setRestoreTime(LocalDateTime restoreTime) { this.restoreTime = restoreTime; }
    public String getRestoreMessage() { return restoreMessage; }
    public void setRestoreMessage(String restoreMessage) { this.restoreMessage = restoreMessage; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
}
