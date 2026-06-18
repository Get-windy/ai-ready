package cn.aiedge.tenant.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户配额
 */
@Schema(description = "租户配额")
@TableName("sys_tenant_quota")
public class SysTenantQuota implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "配额ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "最大用户数")
    private Integer maxUsers;

    @Schema(description = "存储配额(GB)")
    private String maxStorage;

    @Schema(description = "API调用限制/月")
    private Integer maxApiCalls;

    @Schema(description = "已用用户数")
    private Integer usedUsers;

    @Schema(description = "已用存储(GB)")
    private String usedStorage;

    @Schema(description = "已用API调用数")
    private Integer usedApiCalls;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    public String getMaxStorage() { return maxStorage; }
    public void setMaxStorage(String maxStorage) { this.maxStorage = maxStorage; }
    public Integer getMaxApiCalls() { return maxApiCalls; }
    public void setMaxApiCalls(Integer maxApiCalls) { this.maxApiCalls = maxApiCalls; }
    public Integer getUsedUsers() { return usedUsers; }
    public void setUsedUsers(Integer usedUsers) { this.usedUsers = usedUsers; }
    public String getUsedStorage() { return usedStorage; }
    public void setUsedStorage(String usedStorage) { this.usedStorage = usedStorage; }
    public Integer getUsedApiCalls() { return usedApiCalls; }
    public void setUsedApiCalls(Integer usedApiCalls) { this.usedApiCalls = usedApiCalls; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
