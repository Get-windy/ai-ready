package com.aiready.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息查询请求
 */
@Data
public class MessageQueryRequest {
    
    /**
     * 房间ID
     */
    private String roomId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 阅读状态
     */
    private Integer readStatus;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 20;
}
