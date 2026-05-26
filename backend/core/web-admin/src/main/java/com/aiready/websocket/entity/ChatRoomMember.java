package com.aiready.websocket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 聊天房间成员实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_chat_room_member")
public class ChatRoomMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 房间ID
     */
    private String roomId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户在房间中的昵称
     */
    private String nickname;
    
    /**
     * 用户角色（1：普通成员 2：管理员 3：群主）
     */
    private Integer role;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
    
    /**
     * 最后阅读消息ID
     */
    private Long lastReadMessageId;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 是否静音（0：否 1：是）
     */
    private Integer isMuted;
    
    /**
     * 未读消息数
     */
    private Integer unreadCount;
    
    /**
     * 成员状态（0：已退出 1：正常）
     */
    private Integer status;
    
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
