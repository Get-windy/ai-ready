package com.aiready.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 短信发送记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_sms_record")
public class SmsRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联通知ID
     */
    private Long notificationId;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 短信签名
     */
    private String signName;
    
    /**
     * 短信模板CODE
     */
    private String templateCode;
    
    /**
     * 模板参数（JSON格式）
     */
    private String templateParams;
    
    /**
     * 短信内容
     */
    private String content;
    
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
     * 短信平台返回的ID
     */
    private String platformMsgId;
    
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
