package cn.aiedge.tenant.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户套餐
 */
@Schema(description = "租户套餐")
@TableName("sys_tenant_package")
public class SysTenantPackage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "套餐编码")
    private String packageCode;

    @Schema(description = "购买类型: monthly/yearly/perpetual")
    private String purchaseType;

    @Schema(description = "价格（分）")
    private Long price;

    @Schema(description = "最大用户数")
    private Integer maxUsers;

    @Schema(description = "存储配额(GB)")
    private Integer storageQuota;

    @Schema(description = "API调用限制/月")
    private Integer apiCallLimit;

    @Schema(description = "状态: 1=启用 0=停用")
    private Integer status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sortOrder;

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

    @Schema(description = "逻辑删除: 0=未删 1=已删")
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getPurchaseType() { return purchaseType; }
    public void setPurchaseType(String purchaseType) { this.purchaseType = purchaseType; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    public Integer getStorageQuota() { return storageQuota; }
    public void setStorageQuota(Integer storageQuota) { this.storageQuota = storageQuota; }
    public Integer getApiCallLimit() { return apiCallLimit; }
    public void setApiCallLimit(Integer apiCallLimit) { this.apiCallLimit = apiCallLimit; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
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
