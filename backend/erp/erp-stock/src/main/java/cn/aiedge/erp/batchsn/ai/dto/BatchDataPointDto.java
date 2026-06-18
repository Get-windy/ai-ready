package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 批次数据点DTO
 */
@Data
public class BatchDataPointDto {
    
    private String dataPointId;
    private Long batchId;
    private String batchNumber;
    
    // 数据类型
    private String dataType; // quality, production, environment, equipment, etc.
    private String dataSource; // sensor, manual, system, etc.
    private String dataCategory; // 数据类别
    
    // 数据值
    private Map<String, Object> values; // 数据值映射
    private Map<String, String> units; // 单位映射
    private Map<String, Double> normalizedValues; // 标准化后的值
    
    // 时间信息
    private Date timestamp;
    private Long timestampMillis;
    private String timeZone;
    
    // 数据质量
    private Double dataQualityScore; // 数据质量分数 0-1
    private Boolean isMissingValues; // 是否有缺失值
    private Boolean isOutlier; // 是否是异常值
    private String dataQualityIssues; // 数据质量问题
    private Boolean isValid; // 是否有效
    
    // 特征信息
    private Map<String, Object> features; // 特征数据
    private Map<String, Double> featureImportance; // 特征重要性
    
    // 处理信息
    private Boolean isProcessed; // 是否已处理
    private String processingMethod; // 处理方法
    private Date processingTime; // 处理时间
    
    // 元数据
    private Map<String, Object> metadata;
    private String tags; // 标签，逗号分隔
    private String notes; // 备注
    
    /**
     * 获取主要数值
     */
    public Double getPrimaryValue() {
        if (values == null || values.isEmpty()) return null;
        
        // 尝试获取常见的数值字段
        String[] primaryKeys = {"value", "reading", "measurement", "score", "level"};
        for (String key : primaryKeys) {
            if (values.containsKey(key)) {
                Object val = values.get(key);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                }
            }
        }
        
        // 如果没有找到，返回第一个数值
        for (Object val : values.values()) {
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否为数值数据
     */
    public boolean isNumericData() {
        return getPrimaryValue() != null;
    }
    
    /**
     * 获取数据质量等级
     */
    public String getDataQualityLevel() {
        if (dataQualityScore == null) return "未知";
        
        if (dataQualityScore >= 0.9) return "优";
        else if (dataQualityScore >= 0.8) return "良";
        else if (dataQualityScore >= 0.7) return "中";
        else return "差";
    }
    
    /**
     * 判断是否为有效训练数据
     */
    public boolean isValidForTraining() {
        return isValid != null && isValid && 
               dataQualityScore != null && dataQualityScore >= 0.7 &&
               timestamp != null && values != null && !values.isEmpty();
    }
}