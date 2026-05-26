package com.aiready.websocket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * WebSocket消息实体类
 * 用于存储消息历史记录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_websocket_message")
public class WebSocketMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息ID（UUID，用于客户端去重）
     */
    private String messageId;
    
    /**
     * 消息类型（1：系统通知 2：业务通知 3：私聊消息 4：群聊消息 5：广播消息）
     */
    private Integer messageType;
    
    /**
     * 发送者ID（0表示系统发送）
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
     * 接收者ID（私聊使用，群聊为null）
     */
    private Long receiverId;
    
    /**
     * 房间ID（群组/房间标识）
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
     * 附加数据（JSON格式）
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
     * 阅读状态（0：未读 1：已读）
     */
    private Integer readStatus;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
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
