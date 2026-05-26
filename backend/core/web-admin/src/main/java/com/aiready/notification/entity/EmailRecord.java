package com.aiready.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮件发送记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_email_record")
public class EmailRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联通知ID
     */
    private Long notificationId;
    
    /**
     * 收件人邮箱
     */
    private String toEmail;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 邮件内容
     */
    private String content;
    
    /**
     * 抄送地址
     */
    private String cc;
    
    /**
     * 密送地址
     */
    private String bcc;
    
    /**
     * 附件数量
     */
    private Integer attachmentCount;
    
    /**
     * 发送状态（0：待发送 1：发送中 2：发送成功 3：发送失败）
     */
    private Integer status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 失败原因
     */
    private String failReason;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
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
