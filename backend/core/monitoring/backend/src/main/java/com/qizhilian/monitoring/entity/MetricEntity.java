package com.qizhilian.monitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 监控指标实体
 * 存储各种监控指标数据
 * 
 * @author AI-Ready Team
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "monitoring_metrics",
       indexes = {
           @Index(name = "idx_metric_name_time", columnList = "metricName,metricTime"),
           @Index(name = "idx_metric_type", columnList = "metricType"),
           @Index(name = "idx_source_id", columnList = "sourceId"),
           @Index(name = "idx_tenant_metric", columnList = "tenantId,metricName"),
           @Index(name = "idx_metric_time", columnList = "metricTime")
       })
public class MetricEntity extends BaseEntity {
    
    /**
     * 指标名称（唯一标识）
     */
    @Column(name = "metric_name", nullable = false, length = 128)
    private String metricName;
    
    /**
     * 指标类型
     * 系统指标: system.cpu, system.memory, system.disk, system.network
     * 应用指标: application.jvm, application.http, application.db
     * 业务指标: business.user, business.order, business.payment
     * 自定义指标: custom.*
     */
    @Column(name = "metric_type", nullable = false, length = 64)
    private String metricType;
    
    /**
     * 指标时间
     */
    @Column(name = "metric_time", nullable = false)
    private LocalDateTime metricTime;
    
    /**
     * 指标值
     */
    @Column(name = "metric_value", nullable = false, precision = 20, scale = 6)
    private BigDecimal metricValue;
    
    /**
     * 指标单位
     * 如: %, MB, GB, ms, count, req/s
     */
    @Column(name = "metric_unit", length = 32)
    private String metricUnit;
    
    /**
     * 数据来源
     * 如: agent, log, api, system, custom
     */
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;
    
    /**
     * 来源ID（具体来源标识）
     */
    @Column(name = "source_id", length = 128)
    private String sourceId;
    
    /**
     * 主机名/IP
     */
    @Column(name = "hostname", length = 256)
    private String hostname;
    
    /**
     * 服务名
     */
    @Column(name = "service_name", length = 128)
    private String serviceName;
    
    /**
     * 实例ID
     */
    @Column(name = "instance_id", length = 128)
    private String instanceId;
    
    /**
     * 环境标识
     * dev, test, staging, prod
     */
    @Column(name = "environment", length = 32)
    private String environment;
    
    /**
     * 应用名称
     */
    @Column(name = "app_name", length = 128)
    private String appName;
    
    /**
     * 应用版本
     */
    @Column(name = "app_version", length = 32)
    private String appVersion;
    
    /**
     * 指标标签（JSON格式）
     * 如: {"region":"cn-east-1","zone":"zone-a","node":"node-1"}
     */
    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels;
    
    /**
     * 指标元数据（JSON格式）
     * 如: {"min":0,"max":100,"threshold":80,"description":"CPU使用率"}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * 采集间隔（秒）
     */
    @Column(name = "collection_interval")
    private Integer collectionInterval;
    
    /**
     * 采集时间
     */
    @Column(name = "collection_time")
    private LocalDateTime collectionTime;
    
    /**
     * 数据质量
     * good, warning, error, missing
     */
    @Column(name = "data_quality", length = 32)
    private String dataQuality;
    
    /**
     * 数据置信度（0-100）
     */
    @Column(name = "confidence_score")
    private Integer confidenceScore;
    
    /**
     * 是否经过处理
     */
    @Column(name = "is_processed")
    private Boolean isProcessed = false;
    
    /**
     * 处理时间
     */
    @Column(name = "processed_time")
    private LocalDateTime processedTime;
    
    /**
     * 处理结果（JSON格式）
     */
    @Column(name = "processing_result", columnDefinition = "TEXT")
    private String processingResult;
    
    /**
     * 聚合类型
     * raw, min, max, avg, sum, count, p50, p95, p99
     */
    @Column(name = "aggregation_type", length = 32)
    private String aggregationType;
    
    /**
     * 聚合窗口（秒）
     */
    @Column(name = "aggregation_window")
    private Integer aggregationWindow;
    
    /**
     * 原始指标ID（如果是聚合数据）
     */
    @Column(name = "raw_metric_ids", length = 1024)
    private String rawMetricIds;
    
    /**
     * 数据版本
     */
    @Column(name = "data_version", length = 32)
    private String dataVersion;
    
    /**
     * 校验和
     */
    @Column(name = "checksum", length = 64)
    private String checksum;
    
    /**
     * 是否告警相关
     */
    @Column(name = "is_alert_related")
    private Boolean isAlertRelated = false;
    
    /**
     * 告警ID（如果触发告警）
     */
    @Column(name = "alert_id")
    private Long alertId;
    
    /**
     * 默认构造函数
     */
    public MetricEntity() {
        super();
        this.environment = "prod";
        this.dataQuality = "good";
        this.confidenceScore = 100;
    }
    
    /**
     * 带基本信息的构造函数
     * 
     * @param metricName 指标名称
     * @param metricType 指标类型
     * @param metricValue 指标值
     * @param metricUnit 指标单位
     * @param sourceType 数据来源类型
     */
    public MetricEntity(String metricName, String metricType, 
                       BigDecimal metricValue, String metricUnit, 
                       String sourceType) {
        this();
        this.metricName = metricName;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.metricUnit = metricUnit;
        this.sourceType = sourceType;
        this.metricTime = LocalDateTime.now();
        this.collectionTime = LocalDateTime.now();
    }
    
    /**
     * 带主机信息的构造函数
     * 
     * @param metricName 指标名称
     * @param metricType 指标类型
     * @param metricValue 指标值
     * @param metricUnit 指标单位
     * @param sourceType 数据来源类型
     * @param hostname 主机名
     * @param serviceName 服务名
     */
    public MetricEntity(String metricName, String metricType, 
                       BigDecimal metricValue, String metricUnit, 
                       String sourceType, String hostname, String serviceName) {
        this(metricName, metricType, metricValue, metricUnit, sourceType);
        this.hostname = hostname;
        this.serviceName = serviceName;
    }
    
    /**
     * 检查是否为系统指标
     * 
     * @return 是否为系统指标
     */
    public boolean isSystemMetric() {
        return metricType != null && metricType.startsWith("system.");
    }
    
    /**
     * 检查是否为应用指标
     * 
     * @return 是否为应用指标
     */
    public boolean isApplicationMetric() {
        return metricType != null && metricType.startsWith("application.");
    }
    
    /**
     * 检查是否为业务指标
     * 
     * @return 是否为业务指标
     */
    public boolean isBusinessMetric() {
        return metricType != null && metricType.startsWith("business.");
    }
    
    /**
     * 检查是否为自定义指标
     * 
     * @return 是否为自定义指标
     */
    public boolean isCustomMetric() {
        return metricType != null && metricType.startsWith("custom.");
    }
    
    /**
     * 标记为已处理
     * 
     * @param processingResult 处理结果
     */
    public void markAsProcessed(String processingResult) {
        this.isProcessed = true;
        this.processedTime = LocalDateTime.now();
        this.processingResult = processingResult;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为告警相关
     * 
     * @param alertId 告警ID
     */
    public void markAsAlertRelated(Long alertId) {
        this.isAlertRelated = true;
        this.alertId = alertId;
        this.addTag("alert");
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置应用信息
     * 
     * @param appName 应用名称
     * @param appVersion 应用版本
     * @param environment 环境标识
     */
    public void setAppInfo(String appName, String appVersion, String environment) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.environment = environment;
    }
    
    /**
     * 设置采集信息
     * 
     * @param collectionInterval 采集间隔
     */
    public void setCollectionInfo(Integer collectionInterval) {
        this.collectionInterval = collectionInterval;
        this.collectionTime = LocalDateTime.now();
    }
    
    /**
     * 设置数据质量
     * 
     * @param dataQuality 数据质量
     * @param confidenceScore 置信度分数
     */
    public void setDataQuality(String dataQuality, Integer confidenceScore) {
        this.dataQuality = dataQuality;
        this.confidenceScore = confidenceScore;
    }
}