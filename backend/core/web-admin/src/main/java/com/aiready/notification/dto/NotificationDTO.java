package com.aiready.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息通知DTO
 */
@Data
public class NotificationDTO {
    
    private Long id;
    private String title;
    private String content;
    private Integer type;
    private Integer channel;
    private Long userId;
    private Long senderId;
    private String senderName;
    private Long templateId;
    private Map<String, Object> templateParams;
    private String bizType;
    private String bizId;
    private Integer readStatus;
    private LocalDateTime readTime;
    private Integer sendStatus;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}
