package cn.aiedge.erp.batchsn.ai.service;

import cn.aiedge.erp.batchsn.ai.dto.BatchQualityPredictionDto;
import cn.aiedge.erp.batchsn.ai.dto.BatchQualityTrainingDataDto;

import java.util.List;

/**
 * 批次质量智能预测服务接口
 */
public interface BatchQualityPredictionService {
    
    /**
     * 预测批次质量
     * @param batchId 批次ID
     * @return 质量预测结果
     */
    BatchQualityPredictionDto predictBatchQuality(Long batchId);
    
    /**
     * 批量预测批次质量
     * @param batchIds 批次ID列表
     * @return 质量预测结果列表
     */
    List<BatchQualityPredictionDto> predictBatchQualityBatch(List<Long> batchIds);
    
    /**
     * 预测批次未来的质量趋势
     * @param batchId 批次ID
     * @param days 预测天数
     * @return 质量趋势预测
     */
    List<BatchQualityPredictionDto> predictQualityTrend(Long batchId, int days);
    
    /**
     * 训练质量预测模型
     * @param trainingData 训练数据
     * @return 训练结果
     */
    boolean trainModel(List<BatchQualityTrainingDataDto> trainingData);
    
    /**
     * 评估模型性能
     * @return 模型评估指标
     */
    ModelEvaluationResult evaluateModel();
    
    /**
     * 获取特征重要性
     * @return 特征重要性列表
     */
    List<FeatureImportance> getFeatureImportance();
    
    /**
     * 检查模型是否就绪
     * @return 模型就绪状态
     */
    boolean isModelReady();
    
    /**
     * 更新模型
     * @param incrementalData 增量数据
     * @return 更新结果
     */
    boolean updateModel(List<BatchQualityTrainingDataDto> incrementalData);
    
    /**
     * 获取模型版本信息
     * @return 模型版本
     */
    String getModelVersion();
    
    /**
     * 保存预测结果
     * @param prediction 预测结果
     * @return 保存结果
     */
    boolean savePrediction(BatchQualityPredictionDto prediction);
    
    /**
     * 获取历史预测记录
     * @param batchId 批次ID
     * @param limit 限制数量
     * @return 历史预测记录
     */
    List<BatchQualityPredictionDto> getPredictionHistory(Long batchId, int limit);
    
    /**
     * 模型评估结果
     */
    class ModelEvaluationResult {
        private double accuracy;
        private double precision;
        private double recall;
        private double f1Score;
        private double auc;
        private String confusionMatrix;
        private String classificationReport;
        
        public ModelEvaluationResult(double accuracy, double precision, double recall, double f1Score, double auc) {
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
            this.auc = auc;
        }
        
        // getters and setters
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public double getPrecision() { return precision; }
        public void setPrecision(double precision) { this.precision = precision; }
        public double getRecall() { return recall; }
        public void setRecall(double recall) { this.recall = recall; }
        public double getF1Score() { return f1Score; }
        public void setF1Score(double f1Score) { this.f1Score = f1Score; }
        public double getAuc() { return auc; }
        public void setAuc(double auc) { this.auc = auc; }
        public String getConfusionMatrix() { return confusionMatrix; }
        public void setConfusionMatrix(String confusionMatrix) { this.confusionMatrix = confusionMatrix; }
        public String getClassificationReport() { return classificationReport; }
        public void setClassificationReport(String classificationReport) { this.classificationReport = classificationReport; }
    }
    
    /**
     * 特征重要性
     */
    class FeatureImportance {
        private String featureName;
        private double importance;
        private String description;
        
        public FeatureImportance(String featureName, double importance) {
            this.featureName = featureName;
            this.importance = importance;
        }
        
        // getters and setters
        public String getFeatureName() { return featureName; }
        public void setFeatureName(String featureName) { this.featureName = featureName; }
        public double getImportance() { return importance; }
        public void setImportance(double importance) { this.importance = importance; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}