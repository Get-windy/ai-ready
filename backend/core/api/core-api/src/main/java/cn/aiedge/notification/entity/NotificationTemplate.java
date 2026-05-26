package cn.aiedge.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知模板实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_notification_template")
public class NotificationTemplate {

    /**
     * 模板ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码（唯一标识）
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 通知类型：site-站内信，email-邮件，sms-短信
     */
    private String notifyType;

    /**
     * 模板标题
     */
    private String title;

    /**
     * 模板内容（支持变量占位符 ${var}）
     */
    private String content;

    /**
     * 变量定义（JSON格式）
     */
    private String variables;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

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

    // ==================== 常量定义 ====================

    public static final String TYPE_SITE = "site";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_SMS = "sms";

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
}
