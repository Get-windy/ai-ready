package cn.aiedge.notification.service;

import cn.aiedge.notification.entity.NotificationRecord;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知发送请求DTO
 * 支持定时发送和优先级
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
public class NotificationSendRequest {

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 接收者类型
     */
    private String receiverType;

    /**
     * 接收地址
     */
    private String receiverAddress;

    /**
     * 模板变量
     */
    private Map<String, Object> variables;

    /**
     * 优先级（1-10，数字越大优先级越高，默认5）
     */
    private Integer priority = 5;

    /**
     * 定时发送时间（null表示立即发送）
     */
    private LocalDateTime scheduledTime;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private String bizId;

    /**
     * 是否异步发送
     */
    private boolean async = true;

    // ==================== 构建方法 ====================

    public static NotificationSendRequest of(String templateCode, Long receiverId, 
                                              String receiverType, Map<String, Object> variables) {
        NotificationSendRequest request = new NotificationSendRequest();
        request.setTemplateCode(templateCode);
        request.setReceiverId(receiverId);
        request.setReceiverType(receiverType);
        request.setVariables(variables);
        return request;
    }

    public static NotificationSendRequest ofEmail(String email, String title, String content) {
        NotificationSendRequest request = new NotificationSendRequest();
        request.setTemplateCode("__direct_email__");
        request.setReceiverAddress(email);
        request.getVariables().put("__title__", title);
        request.getVariables().put("__content__", content);
        return request;
    }

    public static NotificationSendRequest ofSiteMessage(Long receiverId, String title, String content) {
        NotificationSendRequest request = new NotificationSendRequest();
        request.setTemplateCode("__direct_site__");
        request.setReceiverId(receiverId);
        request.setReceiverType(NotificationRecord.RECEIVER_USER);
        request.getVariables().put("__title__", title);
        request.getVariables().put("__content__", content);
        return request;
    }

    // ==================== 链式设置 ====================

    public NotificationSendRequest priority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public NotificationSendRequest scheduledAt(LocalDateTime time) {
        this.scheduledTime = time;
        return this;
    }

    public NotificationSendRequest bizInfo(String bizType, String bizId) {
        this.bizType = bizType;
        this.bizId = bizId;
        return this;
    }

    public NotificationSendRequest async(boolean async) {
        this.async = async;
        return this;
    }

    public NotificationSendRequest receiverAddress(String address) {
        this.receiverAddress = address;
        return this;
    }

    // ==================== Getters & Setters ====================

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
