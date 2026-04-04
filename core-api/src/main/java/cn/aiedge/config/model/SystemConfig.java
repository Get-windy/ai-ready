package cn.aiedge.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置项
 */
@Schema(description = "系统配置项")
public class SystemConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;
    
    @Schema(description = "配置键")
    private String configKey;
    
    @Schema(description = "配置值")
    private String configValue;
    
    @Schema(description = "配置类型: system/business/security/integration")
    private String configType;
    
    @Schema(description = "配置分组")
    private String configGroup;
    
    @Schema(description = "配置名称")
    private String configName;
    
    @Schema(description = "配置描述")
    private String description;
    
    @Schema(description = "值类型: string/number/boolean/json/list")
    private String valueType;
    
    @Schema(description = "默认值")
    private String defaultValue;
    
    @Schema(description = "是否启用")
    private boolean enabled;
    
    @Schema(description = "是否系统配置")
    private boolean systemConfig;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @Schema(description = "租户ID")
    private Long tenantId;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getConfigType() { return configType; }
    public void setConfigType(String configType) { this.configType = configType; }
    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSystemConfig() { return systemConfig; }
    public void setSystemConfig(boolean systemConfig) { this.systemConfig = systemConfig; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
