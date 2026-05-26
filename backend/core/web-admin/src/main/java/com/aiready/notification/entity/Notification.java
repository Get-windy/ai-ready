package com.aiready.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息通知实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_notification")
public class Notification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型（1：系统通知 2：业务通知 3：提醒通知）
     */
    private Integer type;
    
    /**
     * 发送渠道（1：站内信 2：邮件 3：短信 4：多通道）
     */
    private Integer channel;
    
    /**
     * 接收用户ID
     */
    private Long userId;
    
    /**
     * 发送者ID（0表示系统发送）
     */
    private Long senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 模板参数（JSON格式）
     */
    private String templateParams;
    
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
     * 发送状态（0：待发送 1：发送中 2：发送成功 3：发送失败）
     */
    private Integer sendStatus;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 失败原因
     */
    private String failReason;
    
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
