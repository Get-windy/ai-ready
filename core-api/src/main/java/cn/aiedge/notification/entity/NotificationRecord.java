package cn.aiedge.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知记录实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_notification_record")
public class NotificationRecord {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 通知类型
     */
    private String notifyType;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 接收者类型：user-用户，role-角色，all-全员
     */
    private String receiverType;

    /**
     * 接收地址（邮箱/手机号等）
     */
    private String receiverAddress;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 发送状态：0-待发送，1-发送中，2-发送成功，3-发送失败
     */
    private Integer status;

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
     * 阅读状态：0-未读，1-已读
     */
    private Integer readStatus;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private String bizId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== 常量定义 ====================

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENDING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;

    public static final int READ_UNREAD = 0;
    public static final int READ_READ = 1;

    public static final String RECEIVER_USER = "user";
    public static final String RECEIVER_ROLE = "role";
    public static final String RECEIVER_ALL = "all";
}
