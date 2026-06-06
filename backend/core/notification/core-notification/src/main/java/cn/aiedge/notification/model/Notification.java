package cn.aiedge.notification.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息
 */
@Schema(description = "通知消息")
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    private Long notificationId;
    
    @Schema(description = "通知类型: system/business/approval/alert")
    private String notificationType;
    
    @Schema(description = "通知标题")
    private String title;
    
    @Schema(description = "通知内容")
    private String content;
    
    @Schema(description = "接收人ID")
    private Long receiverId;
    
    @Schema(description = "发送人ID")
    private Long senderId;
    
    @Schema(description = "发送人名称")
    private String senderName;
    
    @Schema(description = "渠道: in_app/email/sms/wechat/websocket")
    private String channel;
    
    @Schema(description = "优先级: low/normal/high/urgent")
    private String priority;
    
    @Schema(description = "状态: pending/sent/delivered/read/failed")
    private String status;
    
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
    
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
    
    @Schema(description = "关联业务类型")
    private String businessType;
    
    @Schema(description = "关联业务ID")
    private String businessId;
    
    @Schema(description = "跳转链接")
    private String linkUrl;
    
    @Schema(description = "扩展数据")
    private Map<String, Object> extraData;
    
    @Schema(description = "租户ID")
    private Long tenantId;

    // Getters and Setters
    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSendTime() { return sendTime; }
    public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
    public LocalDateTime getReadTime() { return readTime; }
    public void setReadTime(LocalDateTime readTime) { this.readTime = readTime; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public Map<String, Object> getExtraData() { return extraData; }
    public void setExtraData(Map<String, Object> extraData) { this.extraData = extraData; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
