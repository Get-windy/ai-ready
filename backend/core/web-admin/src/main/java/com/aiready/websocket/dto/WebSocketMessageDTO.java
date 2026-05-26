package com.aiready.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket消息DTO
 */
@Data
public class WebSocketMessageDTO {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型（1：系统通知 2：业务通知 3：私聊消息 4：群聊消息 5：广播消息）
     */
    private Integer messageType;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 发送者头像
     */
    private String senderAvatar;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 房间ID
     */
    private String roomId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息内容类型（1：文本 2：图片 3：文件 4：富文本）
     */
    private Integer contentType;
    
    /**
     * 附加数据
     */
    private String extraData;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private String bizId;
    
    /**
     * 阅读状态
     */
    private Integer readStatus;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
