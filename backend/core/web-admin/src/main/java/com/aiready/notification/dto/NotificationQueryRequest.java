package com.aiready.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知查询请求DTO
 */
@Data
public class NotificationQueryRequest {
    
    private Long userId;
    private Integer type;
    private Integer channel;
    private Integer readStatus;
    private Integer sendStatus;
    private String bizType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String keyword;
    private Integer page = 1;
    private Integer size = 10;
}
