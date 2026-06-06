package cn.aiedge.notification.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知模板
 */
@Schema(description = "通知模板")
public class NotificationTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;
    
    @Schema(description = "模板编码")
    private String templateCode;
    
    @Schema(description = "模板名称")
    private String templateName;
    
    @Schema(description = "通知类型")
    private String notificationType;
    
    @Schema(description = "渠道")
    private String channel;
    
    @Schema(description = "标题模板")
    private String titleTemplate;
    
    @Schema(description = "内容模板")
    private String contentTemplate;
    
    @Schema(description = "变量列表(逗号分隔)")
    private String variables;
    
    @Schema(description = "是否启用")
    private boolean enabled;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getters and Setters
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }
    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
