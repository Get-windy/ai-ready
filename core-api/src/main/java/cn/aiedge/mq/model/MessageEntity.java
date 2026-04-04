package cn.aiedge.mq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通用消息实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息内容
     */
    private Object payload;

    /**
     * 创建时间
     */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 过期时间（时间戳）
     */
    private Long expireTime;

    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetry = 3;

    /**
     * 来源服务
     */
    private String sourceService;

    /**
     * 目标服务
     */
    private String targetService;

    /**
     * 扩展属性
     */
    private java.util.Map<String, Object> extra;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建简单消息
     */
    public static MessageEntity of(String topic, Object payload) {
        return MessageEntity.builder()
                .topic(topic)
                .payload(payload)
                .build();
    }

    /**
     * 创建带类型的消息
     */
    public static MessageEntity of(String topic, String messageType, Object payload) {
        return MessageEntity.builder()
                .topic(topic)
                .messageType(messageType)
                .payload(payload)
                .build();
    }

    /**
     * 创建邮件消息
     */
    public static MessageEntity emailMessage(String to, String subject, String content) {
        return MessageEntity.builder()
                .topic("email")
                .messageType("EMAIL_SEND")
                .payload(new EmailPayload(to, subject, content))
                .build();
    }

    /**
     * 创建短信消息
     */
    public static MessageEntity smsMessage(String phone, String content) {
        return MessageEntity.builder()
                .topic("sms")
                .messageType("SMS_SEND")
                .payload(new SmsPayload(phone, content))
                .build();
    }

    /**
     * 创建通知消息
     */
    public static MessageEntity notificationMessage(Long userId, String title, String content) {
        return MessageEntity.builder()
                .topic("notification")
                .messageType("NOTIFICATION_SEND")
                .payload(new NotificationPayload(userId, title, content))
                .build();
    }

    /**
     * 增加重试次数
     */
    public MessageEntity incrementRetry() {
        this.retryCount++;
        return this;
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetry;
    }

    // ==================== 内部载荷类 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailPayload {
        private String to;
        private String subject;
        private String content;
        private String[] cc;
        private String[] bcc;
        private Map<String, String> attachments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsPayload {
        private String phone;
        private String content;
        private String templateCode;
        private Map<String, String> params;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPayload {
        private Long userId;
        private String title;
        private String content;
        private String type;
        private String link;
    }
}
