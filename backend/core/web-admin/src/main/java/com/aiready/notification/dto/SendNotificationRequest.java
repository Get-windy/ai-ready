package com.aiready.notification.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 发送消息通知请求DTO
 */
@Data
public class SendNotificationRequest {
    
    /**
     * 接收用户ID列表
     */
    private List<Long> userIds;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型（1：系统通知 2：业务通知 3：提醒通知）
     */
    private Integer type;
    
    /**
     * 发送渠道（1：站内信 2：邮件 3：短信 4：多通道）
     */
    private Integer channel;
    
    /**
     * 模板编码
     */
    private String templateCode;
    
    /**
     * 模板参数
     */
    private Map<String, Object> templateParams;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private String bizId;
    
    /**
     * 邮件主题（邮件通知专用）
     */
    private String emailSubject;
    
    /**
     * 邮件内容HTML（邮件通知专用）
     */
    private String emailContent;
    
    /**
     * 手机号列表（短信通知专用）
     */
    private List<String> phoneNumbers;
}
