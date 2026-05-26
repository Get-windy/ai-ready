package com.aiready.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天房间DTO
 */
@Data
public class ChatRoomDTO {
    
    /**
     * 房间ID
     */
    private String roomId;
    
    /**
     * 房间名称
     */
    private String roomName;
    
    /**
     * 房间描述
     */
    private String description;
    
    /**
     * 房间类型（1：私聊 2：群聊 3：系统广播）
     */
    private Integer roomType;
    
    /**
     * 房间头像
     */
    private String roomAvatar;
    
    /**
     * 创建者ID
     */
    private Long creatorId;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
    
    /**
     * 当前成员数
     */
    private Integer currentMembers;
    
    /**
     * 房间状态
     */
    private Integer status;
    
    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 最后消息内容预览
     */
    private String lastMessagePreview;
    
    /**
     * 未读消息数（当前用户的）
     */
    private Integer unreadCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
