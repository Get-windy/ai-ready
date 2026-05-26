package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 供应商通知实体
 * 管理供应商相关的通知消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_notification")
public class SupplierNotificationEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("notification_no")
    private String notificationNo;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("notification_title")
    private String notificationTitle;

    @TableField("notification_content")
    private String notificationContent;

    @TableField("notification_type")
    private Integer notificationType;

    @TableField("notification_subtype")
    private String notificationSubtype;

    @TableField("business_id")
    private String businessId;

    @TableField("business_no")
    private String businessNo;

    @TableField("notification_priority")
    private Integer notificationPriority;

    @TableField("notification_channel")
    private Integer notificationChannel;

    @TableField("notification_status")
    private Integer notificationStatus;

    @TableField("sender_id")
    private String senderId;

    @TableField("sender_name")
    private String senderName;

    @TableField("sender_department")
    private String senderDepartment;

    @TableField("receiver_id")
    private String receiverId;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_email")
    private String receiverEmail;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("planned_send_time")
    private LocalDateTime plannedSendTime;

    @TableField("actual_send_time")
    private LocalDateTime actualSendTime;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("process_time")
    private LocalDateTime processTime;

    @TableField("process_result")
    private Integer processResult;

    @TableField("process_comment")
    private String processComment;

    @TableField("validity_period")
    private Integer validityPeriod;

    @TableField("require_receipt")
    private Boolean requireReceipt;

    @TableField("has_receipt")
    private Boolean hasReceipt;

    @TableField("receipt_time")
    private LocalDateTime receiptTime;

    @TableField("receipt_content")
    private String receiptContent;

    @TableField("template_id")
    private String templateId;

    @TableField("template_params")
    private String templateParams;

    @TableField("attachment_info")
    private String attachmentInfo;

    @TableField("send_log")
    private String sendLog;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("extend_info")
    private String extendInfo;
}