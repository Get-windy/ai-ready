package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 批次质量训练数据DTO
 */
@Data
public class BatchQualityTrainingDataDto {
    
    private Long batchId;
    private String batchNumber;
    private Long productId;
    private String productName;
    
    // 特征数据
    private Map<String, Object> features;
    
    // 原材料特征
    private Map<String, Object> rawMaterialFeatures;
    
    // 生产过程特征
    private Map<String, Object> productionFeatures;
    
    // 环境特征
    private Map<String, Object> environmentFeatures;
    
    // 设备特征
    private Map<String, Object> equipmentFeatures;
    
    // 人员特征
    private Map<String, Object> operatorFeatures;
    
    // 标签数据（实际质量结果）
    private String actualQualityLevel; // 实际质量等级
    private Double actualQualityScore; // 实际质量分数
    private Integer defectCount; // 缺陷数量
    private String defectTypes; // 缺陷类型，逗号分隔
    private Boolean passedInspection; // 是否通过检验
    
    // 时间信息
    private Date productionDate;
    private Date inspectionDate;
    private Date trainingDate;
    
    // 数据质量标记
    private Boolean isLabeled; // 是否已标注
    private Double labelConfidence; // 标注置信度
    private String labeledBy; // 标注人
    private String dataSource; // 数据来源
    
    // 预处理标记
    private Boolean isNormalized; // 是否已标准化
    private Boolean isCleaned; // 是否已清洗
    private Boolean hasMissingValues; // 是否有缺失值
    private String preprocessingNotes; // 预处理备注
    
    /**
     * 获取特征总数
     */
    public int getFeatureCount() {
        int count = 0;
        if (features != null) count += features.size();
        if (rawMaterialFeatures != null) count += rawMaterialFeatures.size();
        if (productionFeatures != null) count += productionFeatures.size();
        if (environmentFeatures != null) count += environmentFeatures.size();
        if (equipmentFeatures != null) count += equipmentFeatures.size();
        if (operatorFeatures != null) count += operatorFeatures.size();
        return count;
    }
    
    /**
     * 判断是否为有效训练样本
     */
    public boolean isValidTrainingSample() {
        return isLabeled != null && isLabeled && 
               actualQualityLevel != null && 
               features != null && !features.isEmpty();
    }
    
    /**
     * 获取数据质量评分
     */
    public double getDataQualityScore() {
        double score = 0.0;
        if (isLabeled != null && isLabeled) score += 30;
        if (labelConfidence != null && labelConfidence > 0.8) score += 20;
        if (isCleaned != null && isCleaned) score += 20;
        if (!hasMissingValues) score += 15;
        if (getFeatureCount() >= 10) score += 15;
        return score;
    }
    
    /**
     * 获取数据质量等级
     */
    public String getDataQualityLevel() {
        double score = getDataQualityScore();
        if (score >= 90) return "优";
        else if (score >= 75) return "良";
        else if (score >= 60) return "中";
        else return "差";
    }
}