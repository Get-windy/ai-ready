package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统消息实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息类型 1-邮件 2-站内信 3-SMS 4-企业微信
     */
    private Integer msgType;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 接收人ID
     */
    private Long receiverId;

    /**
     * 接收人姓名
     */
    private String receiverName;

    /**
     * 接收邮箱/手机号
     */
    private String receiverContact;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板参数JSON
     */
    private String templateParams;

    /**
     * 发送状态 0-待发送 1-发送中 2-发送成功 3-发送失败
     */
    private Integer sendStatus;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 租户ID
     */
    private Long tenantId;

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
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
