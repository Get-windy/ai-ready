package cn.aiedge.erp.supplier.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商通知配置实体
 * 存储每个供应商的通知偏好和渠道配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("supplier_notification_config")
@Schema(description = "供应商通知配置")
public class SupplierNotificationConfig {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "供应商ID")
    @TableField("supplier_id")
    private Long supplierId;
    
    @Schema(description = "供应商名称")
    @TableField("supplier_name")
    private String supplierName;
    
    @Schema(description = "配置JSON，存储详细的渠道偏好、时间限制等")
    @TableField("config_json")
    @JsonRawValue
    private String configJson;
    
    @Schema(description = "默认语言")
    @TableField("default_language")
    private String defaultLanguage = "zh-CN";
    
    @Schema(description = "静默时段开始时间（格式：HH:mm）")
    @TableField("quiet_hours_start")
    private String quietHoursStart = "22:00";
    
    @Schema(description = "静默时段结束时间（格式：HH:mm）")
    @TableField("quiet_hours_end")
    private String quietHoursEnd = "08:00";
    
    @Schema(description = "是否接收采购订单通知")
    @TableField("receive_purchase_order")
    private Boolean receivePurchaseOrder = true;
    
    @Schema(description = "是否接收付款通知")
    @TableField("receive_payment")
    private Boolean receivePayment = true;
    
    @Schema(description = "是否接收质量通知")
    @TableField("receive_quality")
    private Boolean receiveQuality = true;
    
    @Schema(description = "是否接收绩效通知")
    @TableField("receive_performance")
    private Boolean receivePerformance = true;
    
    @Schema(description = "是否接收交付提醒")
    @TableField("receive_delivery_reminder")
    private Boolean receiveDeliveryReminder = true;
    
    @Schema(description = "紧急联系人手机号")
    @TableField("emergency_phone")
    private String emergencyPhone;
    
    @Schema(description = "钉钉用户ID")
    @TableField("dingtalk_userid")
    private String dingtalkUserid;
    
    @Schema(description = "企业微信用户ID")
    @TableField("wecom_userid")
    private String wecomUserid;
    
    @Schema(description = "飞书用户ID")
    @TableField("feishu_userid")
    private String feishuUserid;
    
    @Schema(description = "App推送设备ID")
    @TableField("app_push_token")
    private String appPushToken;
    
    @Schema(description = "是否启用短信通知")
    @TableField("enable_sms")
    private Boolean enableSms = true;
    
    @Schema(description = "是否启用邮件通知")
    @TableField("enable_email")
    private Boolean enableEmail = true;
    
    @Schema(description = "是否启用站内信")
    @TableField("enable_in_app")
    private Boolean enableInApp = true;
    
    @Schema(description = "是否启用即时通讯通知")
    @TableField("enable_im")
    private Boolean enableIm = false;
    
    @Schema(description = "是否启用App推送")
    @TableField("enable_app_push")
    private Boolean enableAppPush = false;
    
    @Schema(description = "是否启用语音通知")
    @TableField("enable_voice")
    private Boolean enableVoice = false;
    
    @Schema(description = "每日最大通知数量")
    @TableField("daily_max_notifications")
    private Integer dailyMaxNotifications = 100;
    
    @Schema(description = "最后通知时间")
    @TableField("last_notification_time")
    private LocalDateTime lastNotificationTime;
    
    @Schema(description = "今日已发送通知数量")
    @TableField("today_notification_count")
    private Integer todayNotificationCount = 0;
    
    @Schema(description = "配置版本")
    @TableField("config_version")
    private Integer configVersion = 1;
    
    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
    
    @Schema(description = "创建时间")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @Schema(description = "创建人ID")
    @TableField("created_by")
    private String createdBy;
    
    @Schema(description = "更新人ID")
    @TableField("updated_by")
    private String updatedBy;
    
    // ==================== 业务方法 ====================
    
    /**
     * 检查是否在静默时段
     */
    public boolean isInQuietHours() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        
        int startHour = Integer.parseInt(quietHoursStart.substring(0, 2));
        int startMinute = Integer.parseInt(quietHoursStart.substring(3, 5));
        int endHour = Integer.parseInt(quietHoursEnd.substring(0, 2));
        int endMinute = Integer.parseInt(quietHoursEnd.substring(3, 5));
        
        int currentMinutes = hour * 60 + minute;
        int startMinutes = startHour * 60 + startMinute;
        int endMinutes = endHour * 60 + endMinute;
        
        // 处理跨天情况
        if (startMinutes > endMinutes) {
            return currentMinutes >= startMinutes || currentMinutes < endMinutes;
        } else {
            return currentMinutes >= startMinutes && currentMinutes < endMinutes;
        }
    }
    
    /**
     * 检查是否可以发送通知
     */
    public boolean canSendNotification() {
        // 检查今日发送数量
        if (todayNotificationCount >= dailyMaxNotifications) {
            return false;
        }
        
        // 检查静默时段
        if (isInQuietHours()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 增加今日发送计数
     */
    public void incrementTodayCount() {
        if (todayNotificationCount == null) {
            todayNotificationCount = 1;
        } else {
            todayNotificationCount++;
        }
        lastNotificationTime = LocalDateTime.now();
    }
    
    /**
     * 重置今日计数（通常在凌晨执行）
     */
    public void resetTodayCount() {
        todayNotificationCount = 0;
    }
    
    /**
     * 检查是否接收特定类型的通知
     */
    public boolean shouldReceive(String notificationType) {
        if (notificationType == null) {
            return true;
        }
        
        switch (notificationType.toUpperCase()) {
            case "PURCHASE_ORDER":
                return Boolean.TRUE.equals(receivePurchaseOrder);
            case "PAYMENT":
                return Boolean.TRUE.equals(receivePayment);
            case "QUALITY":
                return Boolean.TRUE.equals(receiveQuality);
            case "PERFORMANCE":
                return Boolean.TRUE.equals(receivePerformance);
            case "DELIVERY_REMINDER":
                return Boolean.TRUE.equals(receiveDeliveryReminder);
            default:
                return true;
        }
    }
    
    /**
     * 获取启用的渠道列表
     */
    public String getEnabledChannels() {
        StringBuilder channels = new StringBuilder();
        
        if (Boolean.TRUE.equals(enableInApp)) {
            channels.append("in-app,");
        }
        if (Boolean.TRUE.equals(enableEmail)) {
            channels.append("email,");
        }
        if (Boolean.TRUE.equals(enableSms)) {
            channels.append("sms,");
        }
        if (Boolean.TRUE.equals(enableIm) && (dingtalkUserid != null || wecomUserid != null || feishuUserid != null)) {
            channels.append("im,");
        }
        if (Boolean.TRUE.equals(enableAppPush) && appPushToken != null) {
            channels.append("app-push,");
        }
        if (Boolean.TRUE.equals(enableVoice)) {
            channels.append("voice,");
        }
        
        if (channels.length() > 0) {
            return channels.substring(0, channels.length() - 1);
        }
        return "";
    }
    
    /**
     * 获取紧急联系人
     */
    public String getEmergencyContact() {
        if (emergencyPhone != null && !emergencyPhone.trim().isEmpty()) {
            return emergencyPhone;
        }
        return null;
    }
    
    /**
     * 检查是否有即时通讯渠道
     */
    public boolean hasInstantMessaging() {
        return dingtalkUserid != null || wecomUserid != null || feishuUserid != null;
    }
    
    /**
     * 获取即时通讯渠道信息
     */
    public String getInstantMessagingInfo() {
        StringBuilder info = new StringBuilder();
        
        if (dingtalkUserid != null) {
            info.append("钉钉: ").append(dingtalkUserid).append("; ");
        }
        if (wecomUserid != null) {
            info.append("企业微信: ").append(wecomUserid).append("; ");
        }
        if (feishuUserid != null) {
            info.append("飞书: ").append(feishuUserid).append("; ");
        }
        
        return info.toString();
    }
}