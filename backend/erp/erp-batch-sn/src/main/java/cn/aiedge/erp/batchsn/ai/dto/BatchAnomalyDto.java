package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 批次异常DTO
 */
@Data
public class BatchAnomalyDto {
    
    private Long id;
    private Long batchId;
    private String batchNumber;
    private Long productId;
    private String productName;
    
    // 异常信息
    private String anomalyType; // 异常类型: quality, quantity, timing, etc.
    private String anomalyCode; // 异常代码
    private String anomalyDescription; // 异常描述
    private Double anomalyScore; // 异常分数 0-1
    private Double confidence; // 置信度 0-1
    
    // 严重程度
    private String severity; // 严重程度: critical, high, medium, low
    private Integer severityLevel; // 严重程度等级 1-10
    private String impactAssessment; // 影响评估
    
    // 检测信息
    private String detectionAlgorithm; // 检测算法
    private String modelVersion; // 模型版本
    private Date detectionTime; // 检测时间
    private String detectedBy; // 检测系统
    
    // 特征贡献
    private Map<String, Double> featureContributions; // 特征贡献度
    private String rootCauseAnalysis; // 根因分析
    private String contributingFactors; // 影响因素
    
    // 处理状态
    private String status; // 状态: detected, acknowledged, investigating, resolved, false_positive
    private Date acknowledgedTime; // 确认时间
    private String acknowledgedBy; // 确认人
    private Date investigationStartTime; // 调查开始时间
    private String investigator; // 调查人
    private Date resolutionTime; // 解决时间
    private String resolutionMethod; // 解决方法
    private String resolutionNotes; // 解决备注
    
    // 关联信息
    private Long relatedAnomalyId; // 关联异常ID
    private String correlationType; // 关联类型
    private List<Long> affectedBatchIds; // 受影响的批次ID
    
    // 预警信息
    private Boolean alertTriggered; // 是否触发预警
    private String alertId; // 预警ID
    private String alertChannel; // 预警渠道
    private String alertRecipients; // 预警接收人
    
    // 审计信息
    private String createdBy;
    private Date createdAt;
    private String updatedBy;
    private Date updatedAt;
    
    /**
     * 判断是否为严重异常
     */
    public boolean isCritical() {
        return "critical".equals(severity) || 
               (severityLevel != null && severityLevel >= 8);
    }
    
    /**
     * 判断是否需要立即处理
     */
    public boolean needsImmediateAttention() {
        return isCritical() || 
               (anomalyScore != null && anomalyScore > 0.8) ||
               "detected".equals(status);
    }
    
    /**
     * 获取异常状态描述
     */
    public String getStatusDescription() {
        switch (status) {
            case "detected": return "已检测到异常";
            case "acknowledged": return "已确认异常";
            case "investigating": return "正在调查中";
            case "resolved": return "已解决";
            case "false_positive": return "误报";
            default: return "未知状态";
        }
    }
    
    /**
     * 获取异常类型描述
     */
    public String getAnomalyTypeDescription() {
        switch (anomalyType) {
            case "quality": return "质量异常";
            case "quantity": return "数量异常";
            case "timing": return "时间异常";
            case "cost": return "成本异常";
            case "process": return "过程异常";
            case "equipment": return "设备异常";
            case "material": return "材料异常";
            case "environment": return "环境异常";
            default: return "未知异常类型";
        }
    }
}