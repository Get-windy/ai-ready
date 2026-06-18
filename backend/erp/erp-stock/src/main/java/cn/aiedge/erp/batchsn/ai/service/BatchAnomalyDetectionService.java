package cn.aiedge.erp.batchsn.ai.service;

import cn.aiedge.erp.batchsn.ai.dto.BatchAnomalyDto;
import cn.aiedge.erp.batchsn.ai.dto.BatchDataPointDto;

import java.util.List;
import java.util.Map;

/**
 * 批次数据异常检测和预警服务接口
 */
public interface BatchAnomalyDetectionService {
    
    /**
     * 检测批次数据异常
     * @param batchId 批次ID
     * @return 异常检测结果
     */
    BatchAnomalyDto detectAnomalies(Long batchId);
    
    /**
     * 批量检测异常
     * @param batchIds 批次ID列表
     * @return 异常检测结果列表
     */
    List<BatchAnomalyDto> detectAnomaliesBatch(List<Long> batchIds);
    
    /**
     * 实时数据流异常检测
     * @param dataPoints 数据点列表
     * @return 实时异常检测结果
     */
    List<AnomalyDetectionResult> detectRealtimeAnomalies(List<BatchDataPointDto> dataPoints);
    
    /**
     * 训练异常检测模型
     * @param normalData 正常数据
     * @param anomalyData 异常数据（可选）
     * @return 训练结果
     */
    boolean trainModel(List<BatchDataPointDto> normalData, List<BatchDataPointDto> anomalyData);
    
    /**
     * 评估模型性能
     * @return 模型评估指标
     */
    AnomalyModelEvaluation evaluateModel();
    
    /**
     * 获取异常统计
     * @param timeRange 时间范围
     * @return 异常统计
     */
    AnomalyStatistics getAnomalyStatistics(TimeRange timeRange);
    
    /**
     * 设置预警规则
     * @param rules 预警规则
     * @return 设置结果
     */
    boolean setAlertRules(List<AlertRule> rules);
    
    /**
     * 获取预警规则
     * @return 预警规则列表
     */
    List<AlertRule> getAlertRules();
    
    /**
     * 触发预警
     * @param anomaly 异常数据
     * @return 预警触发结果
     */
    AlertTriggerResult triggerAlert(BatchAnomalyDto anomaly);
    
    /**
     * 获取未处理的预警
     * @return 未处理预警列表
     */
    List<AlertInfo> getPendingAlerts();
    
    /**
     * 处理预警
     * @param alertId 预警ID
     * @param action 处理动作
     * @param notes 处理备注
     * @return 处理结果
     */
    boolean handleAlert(String alertId, String action, String notes);
    
    /**
     * 异常检测结果
     */
    class AnomalyDetectionResult {
        private String dataPointId;
        private Double anomalyScore; // 异常分数 0-1
        private Boolean isAnomaly; // 是否异常
        private Double confidence; // 置信度
        private String anomalyType; // 异常类型
        private String description; // 异常描述
        private Map<String, Double> featureContributions; // 特征贡献度
        
        // getters and setters
        public String getDataPointId() { return dataPointId; }
        public void setDataPointId(String dataPointId) { this.dataPointId = dataPointId; }
        public Double getAnomalyScore() { return anomalyScore; }
        public void setAnomalyScore(Double anomalyScore) { this.anomalyScore = anomalyScore; }
        public Boolean getIsAnomaly() { return isAnomaly; }
        public void setIsAnomaly(Boolean isAnomaly) { this.isAnomaly = isAnomaly; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getAnomalyType() { return anomalyType; }
        public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Double> getFeatureContributions() { return featureContributions; }
        public void setFeatureContributions(Map<String, Double> featureContributions) { this.featureContributions = featureContributions; }
    }
    
    /**
     * 异常模型评估
     */
    class AnomalyModelEvaluation {
        private Double precision; // 精确率
        private Double recall; // 召回率
        private Double f1Score; // F1分数
        private Double auc; // AUC
        private Double falsePositiveRate; // 误报率
        private Double detectionRate; // 检出率
        private String confusionMatrix; // 混淆矩阵
        private String evaluationReport; // 评估报告
        
        // getters and setters
        public Double getPrecision() { return precision; }
        public void setPrecision(Double precision) { this.precision = precision; }
        public Double getRecall() { return recall; }
        public void setRecall(Double recall) { this.recall = recall; }
        public Double getF1Score() { return f1Score; }
        public void setF1Score(Double f1Score) { this.f1Score = f1Score; }
        public Double getAuc() { return auc; }
        public void setAuc(Double auc) { this.auc = auc; }
        public Double getFalsePositiveRate() { return falsePositiveRate; }
        public void setFalsePositiveRate(Double falsePositiveRate) { this.falsePositiveRate = falsePositiveRate; }
        public Double getDetectionRate() { return detectionRate; }
        public void setDetectionRate(Double detectionRate) { this.detectionRate = detectionRate; }
        public String getConfusionMatrix() { return confusionMatrix; }
        public void setConfusionMatrix(String confusionMatrix) { this.confusionMatrix = confusionMatrix; }
        public String getEvaluationReport() { return evaluationReport; }
        public void setEvaluationReport(String evaluationReport) { this.evaluationReport = evaluationReport; }
    }
    
    /**
     * 时间范围
     */
    class TimeRange {
        private String startTime;
        private String endTime;
        
        public TimeRange(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // getters and setters
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }
    
    /**
     * 异常统计
     */
    class AnomalyStatistics {
        private TimeRange timeRange;
        private Integer totalBatches;
        private Integer anomalousBatches;
        private Double anomalyRate; // 异常率
        private Map<String, Integer> anomalyTypeCounts; // 异常类型统计
        private Map<String, Integer> severityCounts; // 严重程度统计
        private List<AnomalyTrend> dailyTrends; // 每日趋势
        
        // getters and setters
        public TimeRange getTimeRange() { return timeRange; }
        public void setTimeRange(TimeRange timeRange) { this.timeRange = timeRange; }
        public Integer getTotalBatches() { return totalBatches; }
        public void setTotalBatches(Integer totalBatches) { this.totalBatches = totalBatches; }
        public Integer getAnomalousBatches() { return anomalousBatches; }
        public void setAnomalousBatches(Integer anomalousBatches) { this.anomalousBatches = anomalousBatches; }
        public Double getAnomalyRate() { return anomalyRate; }
        public void setAnomalyRate(Double anomalyRate) { this.anomalyRate = anomalyRate; }
        public Map<String, Integer> getAnomalyTypeCounts() { return anomalyTypeCounts; }
        public void setAnomalyTypeCounts(Map<String, Integer> anomalyTypeCounts) { this.anomalyTypeCounts = anomalyTypeCounts; }
        public Map<String, Integer> getSeverityCounts() { return severityCounts; }
        public void setSeverityCounts(Map<String, Integer> severityCounts) { this.severityCounts = severityCounts; }
        public List<AnomalyTrend> getDailyTrends() { return dailyTrends; }
        public void setDailyTrends(List<AnomalyTrend> dailyTrends) { this.dailyTrends = dailyTrends; }
    }
    
    /**
     * 异常趋势
     */
    class AnomalyTrend {
        private String date;
        private Integer anomalyCount;
        private Double anomalyRate;
        
        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getAnomalyCount() { return anomalyCount; }
        public void setAnomalyCount(Integer anomalyCount) { this.anomalyCount = anomalyCount; }
        public Double getAnomalyRate() { return anomalyRate; }
        public void setAnomalyRate(Double anomalyRate) { this.anomalyRate = anomalyRate; }
    }
    
    /**
     * 预警规则
     */
    class AlertRule {
        private String ruleId;
        private String ruleName;
        private String condition; // 条件表达式
        private String severity; // 严重程度
        private String alertChannel; // 预警渠道
        private String recipients; // 接收人
        private Boolean enabled; // 是否启用
        private String description; // 规则描述
        
        // getters and setters
        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getAlertChannel() { return alertChannel; }
        public void setAlertChannel(String alertChannel) { this.alertChannel = alertChannel; }
        public String getRecipients() { return recipients; }
        public void setRecipients(String recipients) { this.recipients = recipients; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 预警触发结果
     */
    class AlertTriggerResult {
        private String alertId;
        private Boolean triggered;
        private String ruleId;
        private String message;
        private String severity;
        
        // getters and setters
        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }
        public Boolean getTriggered() { return triggered; }
        public void setTriggered(Boolean triggered) { this.triggered = triggered; }
        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    /**
     * 预警信息
     */
    class AlertInfo {
        private String alertId;
        private String ruleId;
        private String batchId;
        private String message;
        private String severity;
        private String status; // 状态: pending, acknowledged, resolved
        private String triggeredAt;
        private String acknowledgedAt;
        private String resolvedAt;
        private String acknowledgedBy;
        private String resolvedBy;
        private String resolutionNotes;
        
        // getters and setters
        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }
        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(String triggeredAt) { this.triggeredAt = triggeredAt; }
        public String getAcknowledgedAt() { return acknowledgedAt; }
        public void setAcknowledgedAt(String acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
        public String getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
        public String getAcknowledgedBy() { return acknowledgedBy; }
        public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
        public String getResolvedBy() { return resolvedBy; }
        public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
        public String getResolutionNotes() { return resolutionNotes; }
        public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    }
}