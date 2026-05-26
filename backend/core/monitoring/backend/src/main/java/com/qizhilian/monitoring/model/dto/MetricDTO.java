package com.qizhilian.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 监控指标数据传输对象
 * 
 * @author AI-Ready Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricDTO {
    
    /**
     * 指标名称
     */
    @NotBlank(message = "指标名称不能为空")
    private String metricName;
    
    /**
     * 指标类型
     */
    @NotBlank(message = "指标类型不能为空")
    private String metricType;
    
    /**
     * 指标值
     */
    @NotNull(message = "指标值不能为空")
    private BigDecimal metricValue;
    
    /**
     * 指标单位
     */
    private String metricUnit;
    
    /**
     * 指标时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime metricTime;
    
    /**
     * 数据来源类型
     */
    @NotBlank(message = "数据来源类型不能为空")
    private String sourceType;
    
    /**
     * 来源ID
     */
    private String sourceId;
    
    /**
     * 主机名/IP
     */
    private String hostname;
    
    /**
     * 服务名
     */
    private String serviceName;
    
    /**
     * 实例ID
     */
    private String instanceId;
    
    /**
     * 环境标识
     */
    private String environment;
    
    /**
     * 应用名称
     */
    private String appName;
    
    /**
     * 应用版本
     */
    private String appVersion;
    
    /**
     * 指标标签
     */
    private Map<String, String> labels;
    
    /**
     * 指标元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 采集间隔（秒）
     */
    @Positive(message = "采集间隔必须为正数")
    private Integer collectionInterval;
    
    /**
     * 数据质量
     */
    private String dataQuality;
    
    /**
     * 数据置信度（0-100）
     */
    private Integer confidenceScore;
    
    /**
     * 租户ID
     */
    private String tenantId;
    
    /**
     * 业务标识
     */
    private String businessCode;
    
    /**
     * 数据源
     */
    private String dataSource;
    
    /**
     * 数据标签
     */
    private String tags;
    
    /**
     * 验证指标数据
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        if (metricValue == null) {
            return false;
        }
        
        // 检查指标值是否为有效数值
        if (metricValue.compareTo(BigDecimal.valueOf(-1000000)) < 0 || 
            metricValue.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            return false;
        }
        
        // 检查时间是否合理
        if (metricTime != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneYearAgo = now.minusYears(1);
            LocalDateTime oneDayFuture = now.plusDays(1);
            
            if (metricTime.isBefore(oneYearAgo) || metricTime.isAfter(oneDayFuture)) {
                return false;
            }
        }
        
        // 检查置信度
        if (confidenceScore != null && (confidenceScore < 0 || confidenceScore > 100)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取指标标签字符串
     * 
     * @return 标签字符串
     */
    public String getLabelsString() {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 获取元数据字符串
     * 
     * @return 元数据字符串
     */
    public String getMetadataString() {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 创建系统指标DTO
     * 
     * @param metricName 指标名称
     * @param metricValue 指标值
     * @param metricUnit 指标单位
     * @param hostname 主机名
     * @return MetricDTO
     */
    public static MetricDTO createSystemMetric(String metricName, BigDecimal metricValue, 
                                              String metricUnit, String hostname) {
        MetricDTO dto = new MetricDTO();
        dto.setMetricName(metricName);
        dto.setMetricType("system." + metricName.split("\\.")[1]);
        dto.setMetricValue(metricValue);
        dto.setMetricUnit(metricUnit);
        dto.setMetricTime(LocalDateTime.now());
        dto.setSourceType("system");
        dto.setHostname(hostname);
        dto.setEnvironment("prod");
        dto.setDataQuality("good");
        dto.setConfidenceScore(95);
        return dto;
    }
    
    /**
     * 创建应用指标DTO
     * 
     * @param metricName 指标名称
     * @param metricValue 指标值
     * @param metricUnit 指标单位
     * @param appName 应用名称
     * @param instanceId 实例ID
     * @return MetricDTO
     */
    public static MetricDTO createApplicationMetric(String metricName, BigDecimal metricValue, 
                                                   String metricUnit, String appName, 
                                                   String instanceId) {
        MetricDTO dto = new MetricDTO();
        dto.setMetricName(metricName);
        dto.setMetricType("application." + metricName.split("\\.")[1]);
        dto.setMetricValue(metricValue);
        dto.setMetricUnit(metricUnit);
        dto.setMetricTime(LocalDateTime.now());
        dto.setSourceType("agent");
        dto.setServiceName(appName);
        dto.setInstanceId(instanceId);
        dto.setAppName(appName);
        dto.setEnvironment("prod");
        dto.setDataQuality("good");
        dto.setConfidenceScore(90);
        return dto;
    }
    
    /**
     * 创建业务指标DTO
     * 
     * @param metricName 指标名称
     * @param metricValue 指标值
     * @param metricUnit 指标单位
     * @param businessCode 业务标识
     * @return MetricDTO
     */
    public static MetricDTO createBusinessMetric(String metricName, BigDecimal metricValue, 
                                                String metricUnit, String businessCode) {
        MetricDTO dto = new MetricDTO();
        dto.setMetricName(metricName);
        dto.setMetricType("business." + metricName.split("\\.")[1]);
        dto.setMetricValue(metricValue);
        dto.setMetricUnit(metricUnit);
        dto.setMetricTime(LocalDateTime.now());
        dto.setSourceType("api");
        dto.setBusinessCode(businessCode);
        dto.setEnvironment("prod");
        dto.setDataQuality("good");
        dto.setConfidenceScore(85);
        return dto;
    }
}