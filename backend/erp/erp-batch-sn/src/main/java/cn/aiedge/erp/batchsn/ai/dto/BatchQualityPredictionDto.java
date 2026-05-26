package cn.aiedge.erp.batchsn.ai.dto;

import java.util.Date;
import java.util.Map;

/**
 * 批次质量预测DTO
 */
public class BatchQualityPredictionDto {
    
    private Long id;
    private Long batchId;
    private String batchNumber;
    private Long productId;
    private String productName;
    
    // 质量预测结果
    private String qualityLevel; // 优、良、中、差
    private Double qualityScore; // 0-100分
    private Double defectProbability; // 缺陷概率 0-1
    private Double confidence; // 预测置信度 0-1
    
    // 质量特征
    private Map<String, Object> qualityFeatures;
    private Map<String, Double> featureContributions; // 特征贡献度
    
    // 预测信息
    private String modelVersion;
    private String algorithm;
    private Date predictionTime;
    private Date validUntil; // 预测有效期
    
    // 预警信息
    private Boolean needsAttention; // 是否需要关注
    private String attentionReason; // 关注原因
    private String recommendations; // 改进建议
    
    // 历史对比
    private Double historicalAverageScore; // 历史平均分
    private Double scoreDeviation; // 与历史偏差
    private Integer similarBatchCount; // 相似批次数量
    
    // 审计信息
    private String createdBy;
    private Date createdAt;
    private String updatedBy;
    private Date updatedAt;
    
    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }
    
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    
    public Double getDefectProbability() { return defectProbability; }
    public void setDefectProbability(Double defectProbability) { this.defectProbability = defectProbability; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public Map<String, Object> getQualityFeatures() { return qualityFeatures; }
    public void setQualityFeatures(Map<String, Object> qualityFeatures) { this.qualityFeatures = qualityFeatures; }
    
    public Map<String, Double> getFeatureContributions() { return featureContributions; }
    public void setFeatureContributions(Map<String, Double> featureContributions) { this.featureContributions = featureContributions; }
    
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    
    public Date getPredictionTime() { return predictionTime; }
    public void setPredictionTime(Date predictionTime) { this.predictionTime = predictionTime; }
    
    public Date getValidUntil() { return validUntil; }
    public void setValidUntil(Date validUntil) { this.validUntil = validUntil; }
    
    public Boolean getNeedsAttention() { return needsAttention; }
    public void setNeedsAttention(Boolean needsAttention) { this.needsAttention = needsAttention; }
    
    public String getAttentionReason() { return attentionReason; }
    public void setAttentionReason(String attentionReason) { this.attentionReason = attentionReason; }
    
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    
    public Double getHistoricalAverageScore() { return historicalAverageScore; }
    public void setHistoricalAverageScore(Double historicalAverageScore) { this.historicalAverageScore = historicalAverageScore; }
    
    public Double getScoreDeviation() { return scoreDeviation; }
    public void setScoreDeviation(Double scoreDeviation) { this.scoreDeviation = scoreDeviation; }
    
    public Integer getSimilarBatchCount() { return similarBatchCount; }
    public void setSimilarBatchCount(Integer similarBatchCount) { this.similarBatchCount = similarBatchCount; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * 获取质量等级描述
     */
    public String getQualityLevelDescription() {
        switch (qualityLevel) {
            case "优": return "质量优秀，无需关注";
            case "良": return "质量良好，符合标准";
            case "中": return "质量中等，建议关注";
            case "差": return "质量较差，需要改进";
            default: return "未知质量等级";
        }
    }
    
    /**
     * 判断是否需要预警
     */
    public boolean shouldAlert() {
        return needsAttention != null && needsAttention && 
               (qualityScore != null && qualityScore < 60) || 
               (defectProbability != null && defectProbability > 0.3);
    }
    
    /**
     * 获取风险等级
     */
    public String getRiskLevel() {
        if (defectProbability == null) return "未知";
        
        if (defectProbability < 0.1) return "低风险";
        else if (defectProbability < 0.3) return "中风险";
        else if (defectProbability < 0.5) return "高风险";
        else return "极高风险";
    }
}