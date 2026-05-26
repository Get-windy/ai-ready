package cn.aiedge.erp.supplier.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商通知记录实体
 * 记录所有发送给供应商的通知信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("supplier_notification_record")
@Schema(description = "供应商通知记录")
public class SupplierNotificationRecord {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "供应商ID")
    @TableField("supplier_id")
    private Long supplierId;
    
    @Schema(description = "供应商名称")
    @TableField("supplier_name")
    private String supplierName;
    
    @Schema(description = "模板编码")
    @TableField("template_code")
    private String templateCode;
    
    @Schema(description = "通知标题")
    @TableField("title")
    private String title;
    
    @Schema(description = "通知内容")
    @TableField("content")
    private String content;
    
    @Schema(description = "通知渠道列表，逗号分隔")
    @TableField("channels")
    private String channels;
    
    @Schema(description = "通知状态: PENDING-待发送, SENDING-发送中, SUCCESS-成功, FAILED-失败")
    @TableField("status")
    private String status;
    
    @Schema(description = "失败原因")
    @TableField("failure_reason")
    private String failureReason;
    
    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("send_time")
    private LocalDateTime sendTime;
    
    @Schema(description = "送达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("delivered_time")
    private LocalDateTime deliveredTime;
    
    @Schema(description = "阅读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("read_time")
    private LocalDateTime readTime;
    
    @Schema(description = "业务类型")
    @TableField("business_type")
    private String businessType;
    
    @Schema(description = "业务ID")
    @TableField("business_id")
    private String businessId;
    
    @Schema(description = "外部系统ID（如短信服务商返回的ID）")
    @TableField("external_id")
    private String externalId;
    
    @Schema(description = "通知优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急")
    @TableField("priority")
    private String priority;
    
    @Schema(description = "重试次数")
    @TableField("retry_count")
    private Integer retryCount = 0;
    
    @Schema(description = "最大重试次数")
    @TableField("max_retry_count")
    private Integer maxRetryCount = 3;
    
    @Schema(description = "下次重试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;
    
    @Schema(description = "渠道发送详情（JSON格式，记录各渠道发送状态）")
    @TableField("channel_details")
    private String channelDetails;
    
    @Schema(description = "发送IP地址")
    @TableField("sender_ip")
    private String senderIp;
    
    @Schema(description = "发送人ID")
    @TableField("sender_id")
    private Long senderId;
    
    @Schema(description = "发送人姓名")
    @TableField("sender_name")
    private String senderName;
    
    @Schema(description = "通知费用（分）")
    @TableField("cost")
    private Integer cost;
    
    @Schema(description = "通知费用货币")
    @TableField("cost_currency")
    private String costCurrency = "CNY";
    
    @Schema(description = "语言")
    @TableField("language")
    private String language = "zh-CN";
    
    @Schema(description = "通知标签，逗号分隔")
    @TableField("tags")
    private String tags;
    
    @Schema(description = "是否需要回执")
    @TableField("require_receipt")
    private Boolean requireReceipt = false;
    
    @Schema(description = "是否已回执")
    @TableField("has_receipt")
    private Boolean hasReceipt = false;
    
    @Schema(description = "回执时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("receipt_time")
    private LocalDateTime receiptTime;
    
    @Schema(description = "是否归档")
    @TableField("archived")
    private Boolean archived = false;
    
    @Schema(description = "归档时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("archive_time")
    private LocalDateTime archiveTime;
    
    @Schema(description = "创建时间")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // ==================== 业务方法 ====================
    
    /**
     * 检查是否可重试
     */
    public boolean canRetry() {
        if (retryCount == null) {
            retryCount = 0;
        }
        
        if (maxRetryCount == null) {
            maxRetryCount = 3;
        }
        
        // 检查是否超过最大重试次数
        if (retryCount >= maxRetryCount) {
            return false;
        }
        
        // 检查是否有下次重试时间
        if (nextRetryTime != null) {
            return LocalDateTime.now().isAfter(nextRetryTime);
        }
        
        return true;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 1;
        } else {
            retryCount++;
        }
        
        // 设置下次重试时间（指数退避）
        if (retryCount == 1) {
            nextRetryTime = LocalDateTime.now().plusMinutes(1);
        } else if (retryCount == 2) {
            nextRetryTime = LocalDateTime.now().plusMinutes(5);
        } else {
            nextRetryTime = LocalDateTime.now().plusMinutes(15);
        }
    }
    
    /**
     * 标记为发送成功
     */
    public void markAsSuccess(String externalId) {
        this.status = "SUCCESS";
        this.sendTime = LocalDateTime.now();
        this.externalId = externalId;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为发送失败
     */
    public void markAsFailed(String failureReason) {
        this.status = "FAILED";
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为送达
     */
    public void markAsDelivered() {
        this.deliveredTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.readTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否是重要通知
     */
    public boolean isImportant() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }
    
    /**
     * 检查是否已过期（超过30天未发送成功）
     */
    public boolean isExpired() {
        if (createdAt == null) {
            return false;
        }
        
        LocalDateTime expiryDate = createdAt.plusDays(30);
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    /**
     * 获取通知摘要（前50个字符）
     */
    public String getSummary() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        if (content.length() <= 50) {
            return content;
        }
        
        return content.substring(0, 50) + "...";
    }
    
    /**
     * 检查是否是特定业务类型
     */
    public boolean isBusinessType(String type) {
        if (businessType == null || type == null) {
            return false;
        }
        return businessType.equalsIgnoreCase(type);
    }
    
    /**
     * 检查是否使用特定渠道
     */
    public boolean usesChannel(String channel) {
        if (channels == null || channel == null) {
            return false;
        }
        return channels.toLowerCase().contains(channel.toLowerCase());
    }
    
    /**
     * 获取渠道列表数组
     */
    public String[] getChannelArray() {
        if (channels == null || channels.isEmpty()) {
            return new String[0];
        }
        return channels.split(",");
    }
    
    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (tags == null || tags.isEmpty()) {
            tags = tag;
        } else {
            tags += "," + tag;
        }
    }
    
    /**
     * 检查是否有特定标签
     */
    public boolean hasTag(String tag) {
        if (tags == null || tag == null) {
            return false;
        }
        return tags.contains(tag);
    }
    
    /**
     * 归档通知
     */
    public void archive() {
        this.archived = true;
        this.archiveTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 计算通知年龄（天）
     */
    public long getAgeInDays() {
        if (createdAt == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(createdAt, now).toDays();
    }
    
    /**
     * 检查是否需要清理（归档超过90天）
     */
    public boolean shouldBeCleaned() {
        if (!Boolean.TRUE.equals(archived) || archiveTime == null) {
            return false;
        }
        
        LocalDateTime cleanupDate = archiveTime.plusDays(90);
        return LocalDateTime.now().isAfter(cleanupDate);
    }
}