package com.aiready.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息模板DTO
 */
@Data
public class NotificationTemplateDTO {
    
    private Long id;
    private String templateCode;
    private String templateName;
    private Integer type;
    private String title;
    private String content;
    private List<String> params;
    private String emailSubject;
    private String emailContent;
    private String smsSign;
    private String smsTemplateCode;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
