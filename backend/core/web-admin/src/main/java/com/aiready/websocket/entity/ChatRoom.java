package com.aiready.websocket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 聊天房间/群组实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_chat_room")
public class ChatRoom {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 房间ID（唯一标识）
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
     * 最大成员数（0表示无限制）
     */
    private Integer maxMembers;
    
    /**
     * 当前成员数
     */
    private Integer currentMembers;
    
    /**
     * 房间状态（0：禁用 1：正常 2：归档）
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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
