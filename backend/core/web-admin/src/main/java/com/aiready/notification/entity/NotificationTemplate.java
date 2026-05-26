package com.aiready.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息通知模板实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_notification_template")
public class NotificationTemplate {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模板编码
     */
    private String templateCode;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板类型（1：站内信 2：邮件 3：短信）
     */
    private Integer type;
    
    /**
     * 模板标题
     */
    private String title;
    
    /**
     * 模板内容
     */
    private String content;
    
    /**
     * 模板参数（JSON格式，如：["username", "orderNo"]）
     */
    private String params;
    
    /**
     * 邮件主题（邮件模板专用）
     */
    private String emailSubject;
    
    /**
     * 邮件内容HTML（邮件模板专用）
     */
    private String emailContent;
    
    /**
     * 短信签名（短信模板专用）
     */
    private String smsSign;
    
    /**
     * 短信模板CODE（短信模板专用）
     */
    private String smsTemplateCode;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
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
