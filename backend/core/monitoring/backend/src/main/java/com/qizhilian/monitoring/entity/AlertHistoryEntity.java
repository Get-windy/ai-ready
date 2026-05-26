package com.qizhilian.monitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 告警历史实体
 * 存储告警触发、通知、恢复等历史记录
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "alert_history",
       indexes = {
           @Index(name = "idx_alert_rule", columnList = "ruleId"),
           @Index(name = "idx_alert_status", columnList = "alertStatus"),
           @Index(name = "idx_alert_severity", columnList = "severity"),
           @Index(name = "idx_alert_time", columnList = "triggerTime"),
           @Index(name = "idx_tenant_alert", columnList = "tenantId,alertStatus")
       })
public class AlertHistoryEntity extends BaseEntity {

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "metric_name", nullable = false, length = 128)
    private String metricName;

    @Column(name = "metric_value", length = 64)
    private String metricValue;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "alert_status", nullable = false, length = 32)
    private String alertStatus = "firing"; // firing, resolved, acknowledged, suppressed

    @Column(name = "alert_title", length = 256)
    private String alertTitle;

    @Column(name = "alert_content", columnDefinition = "TEXT")
    private String alertContent;

    @Column(name = "trigger_time", nullable = false)
    private LocalDateTime triggerTime;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(name = "acknowledged_time")
    private LocalDateTime acknowledgedTime;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    @Column(name = "notification_channels", columnDefinition = "TEXT")
    private String notificationChannels;

    @Column(name = "notification_status", length = 32)
    private String notificationStatus = "pending"; // pending, sent, failed, partial

    @Column(name = "notification_result", columnDefinition = "TEXT")
    private String notificationResult;

    @Column(name = "hostname", length = 256)
    private String hostname;

    @Column(name = "service_name", length = 128)
    private String serviceName;

    @Column(name = "environment", length = 32)
    private String environment;

    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels;

    @Column(name = "silence_duration")
    private Integer silenceDuration = 300;

    @Column(name = "trigger_count")
    private Integer triggerCount = 1;

    @Column(name = "recovery_notification")
    private Boolean recoveryNotification = true;

    public AlertHistoryEntity() {
        super();
        this.businessCode = "ALERT_HISTORY";
    }

    public void resolve() {
        this.alertStatus = "resolved";
        this.resolvedTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void acknowledge(String user) {
        this.alertStatus = "acknowledged";
        this.acknowledgedTime = LocalDateTime.now();
        this.acknowledgedBy = user;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isFiring() {
        return "firing".equals(this.alertStatus);
    }

    public boolean isResolved() {
        return "resolved".equals(this.alertStatus);
    }
}
