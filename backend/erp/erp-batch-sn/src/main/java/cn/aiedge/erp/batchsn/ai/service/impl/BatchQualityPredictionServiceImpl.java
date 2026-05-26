package cn.aiedge.erp.batchsn.ai.service.impl;

import cn.aiedge.erp.batchsn.ai.config.AiBatchConfig;
import cn.aiedge.erp.batchsn.ai.dto.BatchQualityPredictionDto;
import cn.aiedge.erp.batchsn.ai.dto.BatchQualityTrainingDataDto;
import cn.aiedge.erp.batchsn.ai.service.BatchQualityPredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 批次质量智能预测服务实现
 */
@Service
public class BatchQualityPredictionServiceImpl implements BatchQualityPredictionService {
    
    private static final Logger log = LoggerFactory.getLogger(BatchQualityPredictionServiceImpl.class);
    
    @Autowired
    private AiBatchConfig aiBatchConfig;
    
    // 模拟模型存储
    private Map<String, Object> modelStore = new ConcurrentHashMap<>();
    private Map<Long, List<BatchQualityPredictionDto>> predictionHistory = new ConcurrentHashMap<>();
    
    private static final String MODEL_VERSION = "1.0.0";
    private static final String ALGORITHM = "xgboost";
    
    @Override
    public BatchQualityPredictionDto predictBatchQuality(Long batchId) {
        log.info("开始预测批次质量, batchId: {}", batchId);
        
        if (!aiBatchConfig.getQualityPrediction().isEnabled()) {
            log.warn("批次质量预测功能未启用");
            return createDisabledPrediction(batchId);
        }
        
        try {
            // 模拟从数据库获取批次信息
            String batchNumber = "BATCH-" + batchId;
            Long productId = 1000L + batchId % 10;
            String productName = "产品-" + productId;
            
            // 生成预测结果
            BatchQualityPredictionDto prediction = new BatchQualityPredictionDto();
            prediction.setId(System.currentTimeMillis());
            prediction.setBatchId(batchId);
            prediction.setBatchNumber(batchNumber);
            prediction.setProductId(productId);
            prediction.setProductName(productName);
            
            // 模拟AI预测
            double qualityScore = 70 + Math.random() * 25; // 70-95分
            double defectProbability = 0.1 + Math.random() * 0.3; // 0.1-0.4
            double confidence = 0.8 + Math.random() * 0.15; // 0.8-0.95
            
            prediction.setQualityScore(qualityScore);
            prediction.setDefectProbability(defectProbability);
            prediction.setConfidence(confidence);
            
            // 设置质量等级
            if (qualityScore >= 90) {
                prediction.setQualityLevel("优");
            } else if (qualityScore >= 80) {
                prediction.setQualityLevel("良");
            } else if (qualityScore >= 70) {
                prediction.setQualityLevel("中");
            } else {
                prediction.setQualityLevel("差");
            }
            
            // 设置特征数据
            prediction.setQualityFeatures(Map.of(
                "原材料质量", 85.5,
                "生产工艺", 82.3,
                "设备状态", 88.7,
                "环境控制", 76.9,
                "操作人员", 91.2
            ));
            
            prediction.setFeatureContributions(Map.of(
                "原材料质量", 0.35,
                "生产工艺", 0.25,
                "设备状态", 0.20,
                "环境控制", 0.15,
                "操作人员", 0.05
            ));
            
            // 设置模型信息
            prediction.setModelVersion(MODEL_VERSION);
            prediction.setAlgorithm(ALGORITHM);
            prediction.setPredictionTime(new Date());
            
            // 设置预警信息
            boolean needsAttention = qualityScore < 75 || defectProbability > 0.3;
            prediction.setNeedsAttention(needsAttention);
            if (needsAttention) {
                prediction.setAttentionReason(qualityScore < 75 ? "质量分数较低" : "缺陷概率较高");
                prediction.setRecommendations("建议进行质量检查，优化生产工艺");
            }
            
            // 历史对比
            prediction.setHistoricalAverageScore(82.5);
            prediction.setScoreDeviation(qualityScore - 82.5);
            prediction.setSimilarBatchCount(15);
            
            // 审计信息
            prediction.setCreatedBy("ai-system");
            prediction.setCreatedAt(new Date());
            
            // 保存预测记录
            savePredictionToHistory(prediction);
            
            log.info("批次质量预测完成, batchId: {}, 质量分数: {}, 缺陷概率: {}", 
                     batchId, qualityScore, defectProbability);
            
            return prediction;
            
        } catch (Exception e) {
            log.error("批次质量预测失败, batchId: {}", batchId, e);
            return createErrorPrediction(batchId, e.getMessage());
        }
    }
    
    @Override
    public List<BatchQualityPredictionDto> predictBatchQualityBatch(List<Long> batchIds) {
        log.info("批量预测批次质量, 批次数量: {}", batchIds.size());
        
        return batchIds.stream()
                .map(this::predictBatchQuality)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BatchQualityPredictionDto> predictQualityTrend(Long batchId, int days) {
        log.info("预测批次质量趋势, batchId: {}, 天数: {}", batchId, days);
        
        List<BatchQualityPredictionDto> trends = new ArrayList<>();
        Date baseTime = new Date();
        
        for (int i = 0; i < days; i++) {
            BatchQualityPredictionDto trend = new BatchQualityPredictionDto();
            trend.setBatchId(batchId);
            trend.setBatchNumber("BATCH-" + batchId);
            
            // 模拟趋势数据
            double baseScore = 75 + Math.random() * 20;
            double dailyChange = (Math.random() - 0.5) * 5; // -2.5 to +2.5
            double qualityScore = Math.max(0, Math.min(100, baseScore + dailyChange * i));
            
            trend.setQualityScore(qualityScore);
            trend.setDefectProbability(0.15 + Math.random() * 0.25);
            trend.setConfidence(0.85);
            
            if (qualityScore >= 90) {
                trend.setQualityLevel("优");
            } else if (qualityScore >= 80) {
                trend.setQualityLevel("良");
            } else if (qualityScore >= 70) {
                trend.setQualityLevel("中");
            } else {
                trend.setQualityLevel("差");
            }
            
            // 设置预测时间
            Date predictionTime = new Date(baseTime.getTime() + i * 24L * 60 * 60 * 1000);
            trend.setPredictionTime(predictionTime);
            trend.setValidUntil(new Date(predictionTime.getTime() + 7L * 24 * 60 * 60 * 1000));
            
            trends.add(trend);
        }
        
        return trends;
    }
    
    @Override
    public boolean trainModel(List<BatchQualityTrainingDataDto> trainingData) {
        log.info("开始训练质量预测模型, 训练数据量: {}", trainingData.size());
        
        if (trainingData == null || trainingData.isEmpty()) {
            log.error("训练数据为空");
            return false;
        }
        
        int validSamples = 0;
        double totalQualityScore = 0;
        
        for (BatchQualityTrainingDataDto data : trainingData) {
            if (data.isValidTrainingSample()) {
                validSamples++;
                if (data.getActualQualityScore() != null) {
                    totalQualityScore += data.getActualQualityScore();
                }
            }
        }
        
        if (validSamples < aiBatchConfig.getQualityPrediction().getMinTrainingSamples()) {
            log.error("有效训练样本不足, 需要: {}, 实际: {}", 
                     aiBatchConfig.getQualityPrediction().getMinTrainingSamples(), validSamples);
            return false;
        }
        
        // 模拟模型训练
        double averageScore = validSamples > 0 ? totalQualityScore / validSamples : 0;
        
        modelStore.put("model_version", MODEL_VERSION);
        modelStore.put("trained_at", new Date());
        modelStore.put("training_samples", validSamples);
        modelStore.put("average_quality_score", averageScore);
        modelStore.put("feature_count", trainingData.get(0).getFeatureCount());
        
        log.info("质量预测模型训练完成, 有效样本: {}, 平均质量分数: {}", validSamples, averageScore);
        return true;
    }
    
    @Override
    public ModelEvaluationResult evaluateModel() {
        log.info("评估质量预测模型");
        
        // 模拟评估结果
        ModelEvaluationResult result = new ModelEvaluationResult(
            0.85,  // accuracy
            0.82,  // precision
            0.87,  // recall
            0.84,  // f1Score
            0.89   // auc
        );
        
        result.setConfusionMatrix("[[85, 15], [12, 88]]");
        result.setClassificationReport("precision: 0.82, recall: 0.87, f1-score: 0.84");
        
        return result;
    }
    
    @Override
    public List<FeatureImportance> getFeatureImportance() {
        List<FeatureImportance> importanceList = new ArrayList<>();
        
        importanceList.add(new FeatureImportance("原材料质量", 0.35));
        importanceList.add(new FeatureImportance("生产工艺", 0.25));
        importanceList.add(new FeatureImportance("设备状态", 0.20));
        importanceList.add(new FeatureImportance("环境控制", 0.15));
        importanceList.add(new FeatureImportance("操作人员", 0.05));
        
        // 添加描述
        importanceList.get(0).setDescription("原材料质量对最终产品质量影响最大");
        importanceList.get(1).setDescription("生产工艺的稳定性和规范性");
        importanceList.get(2).setDescription("设备运行状态和维护情况");
        importanceList.get(3).setDescription("生产环境的温湿度控制");
        importanceList.get(4).setDescription("操作人员的技能和经验");
        
        return importanceList;
    }
    
    @Override
    public boolean isModelReady() {
        return modelStore.containsKey("model_version") && 
               aiBatchConfig.getQualityPrediction().isEnabled();
    }
    
    @Override
    public boolean updateModel(List<BatchQualityTrainingDataDto> incrementalData) {
        log.info("更新质量预测模型, 增量数据量: {}", incrementalData.size());
        
        if (incrementalData == null || incrementalData.isEmpty()) {
            log.warn("增量数据为空");
            return false;
        }
        
        int validSamples = 0;
        for (BatchQualityTrainingDataDto data : incrementalData) {
            if (data.isValidTrainingSample()) {
                validSamples++;
            }
        }
        
        if (validSamples < 10) {
            log.warn("有效增量数据不足, 需要至少10个样本, 实际: {}", validSamples);
            return false;
        }
        
        // 模拟模型更新
        Object currentSamples = modelStore.get("training_samples");
        int totalSamples = (currentSamples != null ? (Integer) currentSamples : 0) + validSamples;
        
        modelStore.put("training_samples", totalSamples);
        modelStore.put("last_updated", new Date());
        modelStore.put("incremental_updates", validSamples);
        
        log.info("质量预测模型更新完成, 新增样本: {}, 总样本: {}", validSamples, totalSamples);
        return true;
    }
    
    @Override
    public String getModelVersion() {
        return MODEL_VERSION;
    }
    
    @Override
    public boolean savePrediction(BatchQualityPredictionDto prediction) {
        if (prediction == null || prediction.getBatchId() == null) {
            return false;
        }
        
        savePredictionToHistory(prediction);
        return true;
    }
    
    @Override
    public List<BatchQualityPredictionDto> getPredictionHistory(Long batchId, int limit) {
        List<BatchQualityPredictionDto> history = predictionHistory.get(batchId);
        if (history == null) {
            return new ArrayList<>();
        }
        
        // 按时间倒序排序并限制数量
        return history.stream()
                .sorted((p1, p2) -> {
                    if (p1.getPredictionTime() == null || p2.getPredictionTime() == null) {
                        return 0;
                    }
                    return p2.getPredictionTime().compareTo(p1.getPredictionTime());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 保存预测记录到历史
     */
    private void savePredictionToHistory(BatchQualityPredictionDto prediction) {
        Long batchId = prediction.getBatchId();
        if (batchId == null) return;
        
        predictionHistory.computeIfAbsent(batchId, k -> new ArrayList<>())
                        .add(prediction);
        
        // 限制历史记录数量
        List<BatchQualityPredictionDto> history = predictionHistory.get(batchId);
        if (history.size() > 100) {
            history = history.subList(0, 100);
            predictionHistory.put(batchId, history);
        }
    }
    
    /**
     * 创建功能禁用时的预测结果
     */
    private BatchQualityPredictionDto createDisabledPrediction(Long batchId) {
        BatchQualityPredictionDto prediction = new BatchQualityPredictionDto();
        prediction.setBatchId(batchId);
        prediction.setBatchNumber("BATCH-" + batchId);
        prediction.setQualityLevel("未知");
        prediction.setQualityScore(null);
        prediction.setDefectProbability(null);
        prediction.setConfidence(0.0);
        prediction.setModelVersion("未启用");
        prediction.setPredictionTime(new Date());
        prediction.setNeedsAttention(false);
        prediction.setAttentionReason("质量预测功能未启用");
        return prediction;
    }
    
    /**
     * 创建错误时的预测结果
     */
    private BatchQualityPredictionDto createErrorPrediction(Long batchId, String errorMessage) {
        BatchQualityPredictionDto prediction = new BatchQualityPredictionDto();
        prediction.setBatchId(batchId);
        prediction.setBatchNumber("BATCH-" + batchId);
        prediction.setQualityLevel("错误");
        prediction.setQualityScore(null);
        prediction.setDefectProbability(null);
        prediction.setConfidence(0.0);
        prediction.setModelVersion("错误");
        prediction.setPredictionTime(new Date());
        prediction.setNeedsAttention(true);
        prediction.setAttentionReason("预测失败: " + errorMessage);
        return prediction;
    }
}