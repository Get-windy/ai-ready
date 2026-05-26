package com.aiready.websocket.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建房间请求
 */
@Data
public class CreateRoomRequest {
    
    /**
     * 房间名称
     */
    private String roomName;
    
    /**
     * 房间描述
     */
    private String description;
    
    /**
     * 房间类型（1：私聊 2：群聊）
     */
    private Integer roomType;
    
    /**
     * 房间头像
     */
    private String roomAvatar;
    
    /**
     * 最大成员数（0表示无限制，群聊使用）
     */
    private Integer maxMembers;
    
    /**
     * 初始成员ID列表（群聊使用）
     */
    private List<Long> memberIds;
    
    /**
     * 私聊对方用户ID（私聊使用）
     */
    private Long privateChatUserId;
}
