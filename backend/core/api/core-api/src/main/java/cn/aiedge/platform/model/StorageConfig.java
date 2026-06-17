package cn.aiedge.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 存储配置
 */
@Schema(description = "存储配置")
@TableName("sys_storage_config")
public class StorageConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "存储类型: local/oss/cos/s3")
    private String storageType;

    @Schema(description = "本地存储路径")
    private String localPath;

    @Schema(description = "本地存储URL前缀")
    private String localUrlPrefix;

    @Schema(description = "OSS/S3端点")
    private String endpoint;

    @Schema(description = "存储桶名称")
    private String bucket;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "AccessSecret")
    private String accessSecret;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public String getLocalUrlPrefix() { return localUrlPrefix; }
    public void setLocalUrlPrefix(String localUrlPrefix) { this.localUrlPrefix = localUrlPrefix; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getAccessSecret() { return accessSecret; }
    public void setAccessSecret(String accessSecret) { this.accessSecret = accessSecret; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
