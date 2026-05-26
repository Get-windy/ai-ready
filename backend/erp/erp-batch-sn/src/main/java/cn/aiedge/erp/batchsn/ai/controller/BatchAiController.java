package cn.aiedge.erp.batchsn.ai.controller;

import cn.aiedge.erp.batchsn.ai.dto.BatchAnomalyDto;
import cn.aiedge.erp.batchsn.ai.dto.BatchQualityPredictionDto;
import cn.aiedge.erp.batchsn.ai.dto.BatchQualityTrainingDataDto;
import cn.aiedge.erp.batchsn.ai.dto.BatchSearchResultDto;
import cn.aiedge.erp.batchsn.ai.dto.IntelligentSearchRequestDto;
import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationDto;
import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationRequestDto;
import cn.aiedge.erp.batchsn.ai.service.BatchAnomalyDetectionService;
import cn.aiedge.erp.batchsn.ai.service.BatchQualityPredictionService;
import cn.aiedge.erp.batchsn.ai.service.IntelligentSearchService;
import cn.aiedge.erp.batchsn.ai.service.PriceRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 批次管理AI智能功能API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/batch-sn/ai")
@Tag(name = "批次管理AI智能功能")
public class BatchAiController {
    
    @Autowired
    private BatchQualityPredictionService qualityPredictionService;
    
    @Autowired
    private PriceRecommendationService priceRecommendationService;
    
    @Autowired
    private BatchAnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private IntelligentSearchService intelligentSearchService;
    
    // ==================== 质量预测API ====================
    
    @GetMapping("/quality/predict/{batchId}")
    @Operation(summary = "预测批次质量")
    public ResponseEntity<BatchQualityPredictionDto> predictBatchQuality(
            @Parameter(description = "批次ID", required = true) 
            @PathVariable Long batchId) {
        
        log.info("API调用: 预测批次质量, batchId: {}", batchId);
        
        if (batchId == null || batchId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            BatchQualityPredictionDto prediction = qualityPredictionService.predictBatchQuality(batchId);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            log.error("预测批次质量失败, batchId: {}", batchId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/quality/predict/batch")
    @Operation(summary = "批量预测批次质量")
    public ResponseEntity<List<BatchQualityPredictionDto>> predictBatchQualityBatch(
            @Parameter(description = "批次ID列表", required = true)
            @RequestBody List<Long> batchIds) {
        
        log.info("API调用: 批量预测批次质量, 批次数量: {}", batchIds.size());
        
        if (batchIds == null || batchIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<BatchQualityPredictionDto> predictions = qualityPredictionService.predictBatchQualityBatch(batchIds);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            log.error("批量预测批次质量失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/quality/trend/{batchId}")
    @Operation(summary = "预测批次质量趋势")
    public ResponseEntity<List<BatchQualityPredictionDto>> predictQualityTrend(
            @Parameter(description = "批次ID", required = true) 
            @PathVariable Long batchId,
            @Parameter(description = "预测天数", example = "7") 
            @RequestParam(required = false, defaultValue = "7") int days) {
        
        log.info("API调用: 预测批次质量趋势, batchId: {}, days: {}", batchId, days);
        
        if (batchId == null || batchId <= 0 || days <= 0 || days > 30) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<BatchQualityPredictionDto> trends = qualityPredictionService.predictQualityTrend(batchId, days);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("预测批次质量趋势失败, batchId: {}", batchId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/quality/train")
    @Operation(summary = "训练质量预测模型")
    public ResponseEntity<String> trainQualityModel(
            @Parameter(description = "训练数据", required = true)
            @RequestBody List<BatchQualityTrainingDataDto> trainingData) {
        
        log.info("API调用: 训练质量预测模型, 数据量: {}", trainingData.size());
        
        if (trainingData == null || trainingData.isEmpty()) {
            return ResponseEntity.badRequest().body("训练数据不能为空");
        }
        
        try {
            boolean success = qualityPredictionService.trainModel(trainingData);
            if (success) {
                return ResponseEntity.ok("模型训练成功");
            } else {
                return ResponseEntity.badRequest().body("模型训练失败");
            }
        } catch (Exception e) {
            log.error("训练质量预测模型失败", e);
            return ResponseEntity.internalServerError().body("训练失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/quality/model/evaluate")
    @Operation(summary = "评估质量预测模型")
    public ResponseEntity<BatchQualityPredictionService.ModelEvaluationResult> evaluateQualityModel() {
        log.info("API调用: 评估质量预测模型");
        
        try {
            BatchQualityPredictionService.ModelEvaluationResult result = qualityPredictionService.evaluateModel();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("评估质量预测模型失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/quality/model/features")
    @Operation(summary = "获取特征重要性")
    public ResponseEntity<List<BatchQualityPredictionService.FeatureImportance>> getFeatureImportance() {
        log.info("API调用: 获取特征重要性");
        
        try {
            List<BatchQualityPredictionService.FeatureImportance> features = qualityPredictionService.getFeatureImportance();
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            log.error("获取特征重要性失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ==================== 价格推荐API ====================
    
    @PostMapping("/price/recommend")
    @Operation(summary = "推荐价格策略")
    public ResponseEntity<PriceRecommendationDto> recommendPrice(
            @Parameter(description = "价格推荐请求", required = true)
            @RequestBody PriceRecommendationRequestDto request) {
        
        log.info("API调用: 推荐价格策略, productId: {}", request.getProductId());
        
        if (request == null || request.getProductId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            PriceRecommendationDto recommendation = priceRecommendationService.recommendPrice(request);
            return ResponseEntity.ok(recommendation);
        } catch (Exception e) {
            log.error("推荐价格策略失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/price/recommend/batch")
    @Operation(summary = "批量推荐价格策略")
    public ResponseEntity<List<PriceRecommendationDto>> recommendPriceBatch(
            @Parameter(description = "价格推荐请求列表", required = true)
            @RequestBody List<PriceRecommendationRequestDto> requests) {
        
        log.info("API调用: 批量推荐价格策略, 请求数量: {}", requests.size());
        
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<PriceRecommendationDto> recommendations = priceRecommendationService.recommendPriceBatch(requests);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("批量推荐价格策略失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ==================== 异常检测API ====================
    
    @GetMapping("/anomaly/detect/{batchId}")
    @Operation(summary = "检测批次异常")
    public ResponseEntity<BatchAnomalyDto> detectBatchAnomalies(
            @Parameter(description = "批次ID", required = true) 
            @PathVariable Long batchId) {
        
        log.info("API调用: 检测批次异常, batchId: {}", batchId);
        
        if (batchId == null || batchId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            BatchAnomalyDto anomaly = anomalyDetectionService.detectAnomalies(batchId);
            return ResponseEntity.ok(anomaly);
        } catch (Exception e) {
            log.error("检测批次异常失败, batchId: {}", batchId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/anomaly/detect/batch")
    @Operation(summary = "批量检测异常")
    public ResponseEntity<List<BatchAnomalyDto>> detectBatchAnomaliesBatch(
            @Parameter(description = "批次ID列表", required = true)
            @RequestBody List<Long> batchIds) {
        
        log.info("API调用: 批量检测异常, 批次数量: {}", batchIds.size());
        
        if (batchIds == null || batchIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<BatchAnomalyDto> anomalies = anomalyDetectionService.detectAnomaliesBatch(batchIds);
            return ResponseEntity.ok(anomalies);
        } catch (Exception e) {
            log.error("批量检测异常失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/anomaly/stats")
    @Operation(summary = "获取异常统计")
    public ResponseEntity<BatchAnomalyDetectionService.AnomalyStatistics> getAnomalyStatistics(
            @Parameter(description = "开始时间", example = "2026-04-01") 
            @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间", example = "2026-04-30") 
            @RequestParam(required = false) String endTime) {
        
        log.info("API调用: 获取异常统计, startTime: {}, endTime: {}", startTime, endTime);
        
        try {
            BatchAnomalyDetectionService.TimeRange timeRange = 
                new BatchAnomalyDetectionService.TimeRange(startTime, endTime);
            
            BatchAnomalyDetectionService.AnomalyStatistics stats = 
                anomalyDetectionService.getAnomalyStatistics(timeRange);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取异常统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ==================== 智能搜索API ====================
    
    @PostMapping("/search/intelligent")
    @Operation(summary = "智能搜索批次")
    public ResponseEntity<BatchSearchResultDto> intelligentSearch(
            @Parameter(description = "智能搜索请求", required = true)
            @RequestBody IntelligentSearchRequestDto request) {
        
        log.info("API调用: 智能搜索批次, query: {}", request.getQuery());
        
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            BatchSearchResultDto result = intelligentSearchService.intelligentSearch(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("智能搜索批次失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/search/semantic")
    @Operation(summary = "语义搜索批次")
    public ResponseEntity<List<BatchSearchResultDto>> semanticSearch(
            @Parameter(description = "搜索查询", required = true) 
            @RequestParam String query) {
        
        log.info("API调用: 语义搜索批次, query: {}", query);
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<BatchSearchResultDto> results = intelligentSearchService.semanticSearch(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("语义搜索批次失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/search/similar/{batchId}")
    @Operation(summary = "查找相似批次")
    public ResponseEntity<List<IntelligentSearchService.SimilarBatchResult>> findSimilarBatches(
            @Parameter(description = "批次ID", required = true) 
            @PathVariable Long batchId,
            @Parameter(description = "返回数量", example = "10") 
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        log.info("API调用: 查找相似批次, batchId: {}, limit: {}", batchId, limit);
        
        if (batchId == null || batchId <= 0 || limit <= 0 || limit > 100) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<IntelligentSearchService.SimilarBatchResult> results = 
                intelligentSearchService.findSimilarBatches(batchId, limit);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("查找相似批次失败, batchId: {}", batchId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/search/correlate")
    @Operation(summary = "关联分析批次")
    public ResponseEntity<IntelligentSearchService.CorrelationAnalysisResult> analyzeCorrelations(
            @Parameter(description = "批次ID列表", required = true)
            @RequestBody List<Long> batchIds) {
        
        log.info("API调用: 关联分析批次, 批次数量: {}", batchIds.size());
        
        if (batchIds == null || batchIds.isEmpty() || batchIds.size() > 50) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            IntelligentSearchService.CorrelationAnalysisResult result = 
                intelligentSearchService.analyzeCorrelations(batchIds);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("关联分析批次失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ==================== 系统状态API ====================
    
    @GetMapping("/system/status")
    @Operation(summary = "获取AI系统状态")
    public ResponseEntity<AiSystemStatus> getSystemStatus() {
        log.info("API调用: 获取AI系统状态");
        
        try {
            AiSystemStatus status = new AiSystemStatus();
            status.setQualityPredictionReady(qualityPredictionService.isModelReady());
            status.setQualityModelVersion(qualityPredictionService.getModelVersion());
            status.setTimestamp(new java.util.Date());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取AI系统状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * AI系统状态
     */
    public static class AiSystemStatus {
        private boolean qualityPredictionReady;
        private String qualityModelVersion;
        private java.util.Date timestamp;
        
        // getters and setters
        public boolean isQualityPredictionReady() { return qualityPredictionReady; }
        public void setQualityPredictionReady(boolean qualityPredictionReady) { this.qualityPredictionReady = qualityPredictionReady; }
        public String getQualityModelVersion() { return qualityModelVersion; }
        public void setQualityModelVersion(String qualityModelVersion) { this.qualityModelVersion = qualityModelVersion; }
        public java.util.Date getTimestamp() { return timestamp; }
        public void setTimestamp(java.util.Date timestamp) { this.timestamp = timestamp; }
    }
}