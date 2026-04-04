package cn.aiedge.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置变更日志
 */
@Schema(description = "配置变更日志")
public class ConfigChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long logId;
    
    @Schema(description = "配置ID")
    private Long configId;
    
    @Schema(description = "配置键")
    private String configKey;
    
    @Schema(description = "旧值")
    private String oldValue;
    
    @Schema(description = "新值")
    private String newValue;
    
    @Schema(description = "变更类型: create/update/delete")
    private String changeType;
    
    @Schema(description = "变更原因")
    private String changeReason;
    
    @Schema(description = "操作人ID")
    private Long operatorId;
    
    @Schema(description = "操作人名称")
    private String operatorName;
    
    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
    
    @Schema(description = "租户ID")
    private Long tenantId;
    
    @Schema(description = "客户端IP")
    private String clientIp;

    // Getters and Setters
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
