package com.qizhilian.monitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 * 存储告警规则配置信息
 * 
 * @author AI-Ready Team
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "alert_rules",
       indexes = {
           @Index(name = "idx_rule_name", columnList = "ruleName"),
           @Index(name = "idx_rule_type", columnList = "ruleType"),
           @Index(name = "idx_enabled", columnList = "enabled"),
           @Index(name = "idx_priority", columnList = "priority"),
           @Index(name = "idx_metric_name", columnList = "metricName"),
           @Index(name = "idx_tenant_rule", columnList = "tenantId,ruleName")
       })
public class AlertRuleEntity extends BaseEntity {
    
    /**
     * 规则名称（唯一标识）
     */
    @Column(name = "rule_name", nullable = false, unique = true, length = 128)
    private String ruleName;
    
    /**
     * 规则描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 规则类型
     * threshold: 阈值告警
     * trend: 趋势告警
     * anomaly: 异常检测告警
     * pattern: 模式匹配告警
     * composite: 复合告警
     */
    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType = "threshold";
    
    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 优先级（1-5，1为最高）
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 3;
    
    /**
     * 指标名称
     */
    @Column(name = "metric_name", nullable = false, length = 128)
    private String metricName;
    
    /**
     * 指标类型
     */
    @Column(name = "metric_type", length = 64)
    private String metricType;
    
    /**
     * 规则条件（JSON格式）
     * {
     *   "operator": ">=",
     *   "threshold": 80,
     *   "unit": "%",
     *   "duration": 300,
     *   "times": 3
     * }
     */
    @Column(name = "condition_config", nullable = false, columnDefinition = "TEXT")
    private String conditionConfig;
    
    /**
     * 告警级别
     * info: 信息
     * warning: 警告
     * error: 错误
     * critical: 严重
     */
    @Column(name = "severity", nullable = false, length = 32)
    private String severity = "warning";
    
    /**
     * 告警标题模板
     */
    @Column(name = "title_template", length = 256)
    private String titleTemplate;
    
    /**
     * 告警内容模板
     */
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;
    
    /**
     * 通知渠道（JSON格式）
     * ["email", "dingtalk", "wecom", "sms"]
     */
    @Column(name = "notification_channels", columnDefinition = "TEXT")
    private String notificationChannels;
    
    /**
     * 通知接收人（JSON格式）
     * ["user1@example.com", "user2@example.com", "group:dev-team"]
     */
    @Column(name = "notification_receivers", columnDefinition = "TEXT")
    private String notificationReceivers;
    
    /**
     * 告警分组
     */
    @Column(name = "alert_group", length = 64)
    private String alertGroup;
    
    /**
     * 告警标签（JSON格式）
     */
    @Column(name = "alert_tags", columnDefinition = "TEXT")
    private String alertTags;
    
    /**
     * 告警抑制规则（JSON格式）
     */
    @Column(name = "suppression_rules", columnDefinition = "TEXT")
    private String suppressionRules;
    
    /**
     * 告警升级规则（JSON格式）
     */
    @Column(name = "escalation_rules", columnDefinition = "TEXT")
    private String escalationRules;
    
    /**
     * 告警恢复条件
     */
    @Column(name = "recovery_condition", columnDefinition = "TEXT")
    private String recoveryCondition;
    
    /**
     * 告警恢复通知
     */
    @Column(name = "recovery_notification")
    private Boolean recoveryNotification = true;
    
    /**
     * 告警静默时间（秒）
     */
    @Column(name = "silence_duration")
    private Integer silenceDuration = 300;
    
    /**
     * 最大告警次数
     */
    @Column(name = "max_alert_count")
    private Integer maxAlertCount = 10;
    
    /**
     * 告警冷却时间（秒）
     */
    @Column(name = "cooldown_duration")
    private Integer cooldownDuration = 600;
    
    /**
     * 检查间隔（秒）
     */
    @Column(name = "check_interval", nullable = false)
    private Integer checkInterval = 60;
    
    /**
     * 最后检查时间
     */
    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;
    
    /**
     * 最后触发时间
     */
    @Column(name = "last_trigger_time")
    private LocalDateTime lastTriggerTime;
    
    /**
     * 触发次数
     */
    @Column(name = "trigger_count")
    private Integer triggerCount = 0;
    
    /**
     * 成功恢复次数
     */
    @Column(name = "recovery_count")
    private Integer recoveryCount = 0;
    
    /**
     * 规则配置版本
     */
    @Column(name = "config_version", length = 32)
    private String configVersion = "1.0";
    
    /**
     * 规则配置哈希
     */
    @Column(name = "config_hash", length = 64)
    private String configHash;
    
    /**
     * 规则来源
     */
    @Column(name = "rule_source", length = 32)
    private String ruleSource = "manual";
    
    /**
     * YAML规则定义（原始配置）
     */
    @Column(name = "yaml_definition", columnDefinition = "TEXT")
    private String yamlDefinition;
    
    /**
     * 规则脚本（自定义规则）
     */
    @Column(name = "rule_script", columnDefinition = "TEXT")
    private String ruleScript;
    
    /**
     * 脚本语言
     */
    @Column(name = "script_language", length = 32)
    private String scriptLanguage;
    
    /**
     * 规则依赖（JSON格式）
     */
    @Column(name = "dependencies", columnDefinition = "TEXT")
    private String dependencies;
    
    /**
     * 规则执行超时（秒）
     */
    @Column(name = "execution_timeout")
    private Integer executionTimeout = 30;
    
    /**
     * 规则执行结果缓存（秒）
     */
    @Column(name = "result_cache_ttl")
    private Integer resultCacheTtl = 300;
    
    /**
     * 是否自动生成
     */
    @Column(name = "auto_generated")
    private Boolean autoGenerated = false;
    
    /**
     * 生成来源
     */
    @Column(name = "generation_source", length = 128)
    private String generationSource;
    
    /**
     * 生成参数（JSON格式）
     */
    @Column(name = "generation_params", columnDefinition = "TEXT")
    private String generationParams;
    
    /**
     * 默认构造函数
     */
    public AlertRuleEntity() {
        super();
        this.businessCode = "ALERT_RULE";
        this.titleTemplate = "告警: ${ruleName} - ${metricName} = ${metricValue}${metricUnit}";
        this.contentTemplate = """
            规则名称: ${ruleName}
            指标名称: ${metricName}
            当前值: ${metricValue}${metricUnit}
            阈值: ${threshold}${thresholdUnit}
            告警级别: ${severity}
            触发时间: ${triggerTime}
            主机名: ${hostname}
            服务名: ${serviceName}
            环境: ${environment}
            """;
    }
    
    /**
     * 带基本信息的构造函数
     * 
     * @param ruleName 规则名称
     * @param description 规则描述
     * @param metricName 指标名称
     * @param conditionConfig 条件配置
     * @param severity 告警级别
     */
    public AlertRuleEntity(String ruleName, String description, 
                          String metricName, String conditionConfig, 
                          String severity) {
        this();
        this.ruleName = ruleName;
        this.description = description;
        this.metricName = metricName;
        this.conditionConfig = conditionConfig;
        this.severity = severity;
    }
    
    /**
     * 带通知配置的构造函数
     * 
     * @param ruleName 规则名称
     * @param description 规则描述
     * @param metricName 指标名称
     * @param conditionConfig 条件配置
     * @param severity 告警级别
     * @param notificationChannels 通知渠道
     * @param notificationReceivers 通知接收人
     */
    public AlertRuleEntity(String ruleName, String description, 
                          String metricName, String conditionConfig, 
                          String severity, String notificationChannels, 
                          String notificationReceivers) {
        this(ruleName, description, metricName, conditionConfig, severity);
        this.notificationChannels = notificationChannels;
        this.notificationReceivers = notificationReceivers;
    }
    
    /**
     * 检查是否为阈值告警
     * 
     * @return 是否为阈值告警
     */
    public boolean isThresholdRule() {
        return "threshold".equals(ruleType);
    }
    
    /**
     * 检查是否为趋势告警
     * 
     * @return 是否为趋势告警
     */
    public boolean isTrendRule() {
        return "trend".equals(ruleType);
    }
    
    /**
     * 检查是否为异常检测告警
     * 
     * @return 是否为异常检测告警
     */
    public boolean isAnomalyRule() {
        return "anomaly".equals(ruleType);
    }
    
    /**
     * 检查是否为复合告警
     * 
     * @return 是否为复合告警
     */
    public boolean isCompositeRule() {
        return "composite".equals(ruleType);
    }
    
    /**
     * 检查是否为模式匹配告警
     * 
     * @return 是否为模式匹配告警
     */
    public boolean isPatternRule() {
        return "pattern".equals(ruleType);
    }
    
    /**
     * 触发告警
     */
    public void triggerAlert() {
        this.lastTriggerTime = LocalDateTime.now();
        this.triggerCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 恢复告警
     */
    public void recoverAlert() {
        this.recoveryCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 执行检查
     */
    public void executeCheck() {
        this.lastCheckTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 启用规则
     */
    public void enableRule() {
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 禁用规则
     */
    public void disableRule() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置优先级
     * 
     * @param priority 优先级（1-5）
     */
    public void setPriority(Integer priority) {
        if (priority != null && priority >= 1 && priority <= 5) {
            this.priority = priority;
        }
    }
    
    /**
     * 设置告警级别
     * 
     * @param severity 告警级别
     */
    public void setSeverity(String severity) {
        if (severity != null && 
            (severity.equals("info") || severity.equals("warning") || 
             severity.equals("error") || severity.equals("critical"))) {
            this.severity = severity;
        }
    }
    
    /**
     * 检查是否在静默期内
     * 
     * @return 是否在静默期内
     */
    public boolean isInSilencePeriod() {
        if (lastTriggerTime == null || silenceDuration == null || silenceDuration <= 0) {
            return false;
        }
        LocalDateTime silenceEndTime = lastTriggerTime.plusSeconds(silenceDuration);
        return LocalDateTime.now().isBefore(silenceEndTime);
    }
    
    /**
     * 检查是否在冷却期内
     * 
     * @return 是否在冷却期内
     */
    public boolean isInCooldownPeriod() {
        if (lastTriggerTime == null || cooldownDuration == null || cooldownDuration <= 0) {
            return false;
        }
        LocalDateTime cooldownEndTime = lastTriggerTime.plusSeconds(cooldownDuration);
        return LocalDateTime.now().isBefore(cooldownEndTime);
    }
    
    /**
     * 检查是否达到最大告警次数
     * 
     * @return 是否达到最大告警次数
     */
    public boolean isMaxAlertCountReached() {
        return maxAlertCount != null && triggerCount != null && triggerCount >= maxAlertCount;
    }
    
    /**
     * 重置计数器
     */
    public void resetCounters() {
        this.triggerCount = 0;
        this.recoveryCount = 0;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置YAML定义
     * 
     * @param yamlDefinition YAML定义
     */
    public void setYamlDefinition(String yamlDefinition) {
        this.yamlDefinition = yamlDefinition;
        this.ruleSource = "yaml";
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置脚本规则
     * 
     * @param ruleScript 规则脚本
     * @param scriptLanguage 脚本语言
     */
    public void setScriptRule(String ruleScript, String scriptLanguage) {
        this.ruleScript = ruleScript;
        this.scriptLanguage = scriptLanguage;
        this.ruleSource = "script";
        this.updatedAt = LocalDateTime.now();
    }
}