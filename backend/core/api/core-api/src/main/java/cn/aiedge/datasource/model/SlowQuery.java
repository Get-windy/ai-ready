package cn.aiedge.datasource.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 慢查询记录
 */
@Schema(description = "慢查询记录")
@TableName("sys_slow_query")
public class SlowQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "数据源ID")
    private Long dataSourceId;

    @Schema(description = "查询语句")
    private String queryText;

    @Schema(description = "查询耗时(毫秒)")
    private Long queryTimeMs;

    @Schema(description = "锁等待时间(毫秒)")
    private Long lockTimeMs;

    @Schema(description = "扫描行数")
    private Long rowsExamined;

    @Schema(description = "返回行数")
    private Long rowsSent;

    @Schema(description = "查询时间")
    private LocalDateTime queryTime;

    @Schema(description = "数据库名称")
    private String databaseName;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "主机信息")
    private String hostInfo;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public Long getQueryTimeMs() { return queryTimeMs; }
    public void setQueryTimeMs(Long queryTimeMs) { this.queryTimeMs = queryTimeMs; }
    public Long getLockTimeMs() { return lockTimeMs; }
    public void setLockTimeMs(Long lockTimeMs) { this.lockTimeMs = lockTimeMs; }
    public Long getRowsExamined() { return rowsExamined; }
    public void setRowsExamined(Long rowsExamined) { this.rowsExamined = rowsExamined; }
    public Long getRowsSent() { return rowsSent; }
    public void setRowsSent(Long rowsSent) { this.rowsSent = rowsSent; }
    public LocalDateTime getQueryTime() { return queryTime; }
    public void setQueryTime(LocalDateTime queryTime) { this.queryTime = queryTime; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getHostInfo() { return hostInfo; }
    public void setHostInfo(String hostInfo) { this.hostInfo = hostInfo; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
