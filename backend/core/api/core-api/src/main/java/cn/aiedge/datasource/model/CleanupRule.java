package cn.aiedge.datasource.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据清理规则
 */
@Schema(description = "数据清理规则")
@TableName("sys_data_cleanup_rule")
public class CleanupRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "规则ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "目标表")
    private String targetTable;

    @Schema(description = "条件列")
    private String conditionColumn;

    @Schema(description = "保留天数")
    private Integer retentionDays;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "启停状态: running/paused/stopped（不是执行状态，执行结果见 lastRunStatus）")
    private String status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "最近一次执行时间")
    private LocalDateTime lastRunTime;

    @Schema(description = "最近一次执行结论: success/failed/rejected")
    private String lastRunStatus;

    @Schema(description = "最近一次实际删除行数")
    private Long lastDeletedCount;

    @Schema(description = "最近一次执行明细或失败原因")
    private String lastRunResult;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "是否删除")
    private Integer deleted;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public String getConditionColumn() { return conditionColumn; }
    public void setConditionColumn(String conditionColumn) { this.conditionColumn = conditionColumn; }
    public Integer getRetentionDays() { return retentionDays; }
    public void setRetentionDays(Integer retentionDays) { this.retentionDays = retentionDays; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(LocalDateTime lastRunTime) { this.lastRunTime = lastRunTime; }
    public String getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }
    public Long getLastDeletedCount() { return lastDeletedCount; }
    public void setLastDeletedCount(Long lastDeletedCount) { this.lastDeletedCount = lastDeletedCount; }
    public String getLastRunResult() { return lastRunResult; }
    public void setLastRunResult(String lastRunResult) { this.lastRunResult = lastRunResult; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
