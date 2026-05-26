package cn.aiedge.erp.price.engine.optimization.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 价格优化结果DTO
 */
public record OptimizationResult(
        String requestId,
        boolean success,
        LocalDateTime optimizationTime,
        BigDecimal bestFitnessValue,
        List<OptimizedPrice> optimizedPrices,
        OptimizationMetrics metrics,
        List<ConstraintViolation> constraintViolations,
        List<Recommendation> recommendations,
        Map<String, Object> optimizationDetails
) {
    public record OptimizedPrice(
            String productId,
            String variableName,
            BigDecimal optimizedValue,
            BigDecimal improvementPercentage,
            BigDecimal confidenceScore,
            List<PriceImpact> impacts
    ) {}
    
    public record OptimizationMetrics(
            int iterationsCompleted,
            double executionTimeSeconds,
            double convergenceRate,
            int feasibleSolutionsFound,
            BigDecimal averageFitnessImprovement,
            Map<String, BigDecimal> algorithmMetrics
    ) {}
    
    public record ConstraintViolation(
            String constraintId,
            BigDecimal violationAmount,
            BigDecimal currentValue,
            BigDecimal requiredValue,
            String violationType,
            String description
    ) {}
    
    public record Recommendation(
            String recommendationId,
            RecommendationType type,
            String description,
            BigDecimal expectedImpact,
            BigDecimal confidence,
            String implementationGuidance
    ) {
        public enum RecommendationType {
            PRICE_ADJUSTMENT,    // 价格调整
            DISCOUNT_STRATEGY,   // 折扣策略
            PROMOTIONAL_TIMING,  // 促销时机
            PRODUCT_BUNDLING,    // 产品捆绑
            CUSTOMER_SEGMENTATION // 客户细分
        }
    }
    
    public record PriceImpact(
            String impactType,
            BigDecimal impactValue,
            ImpactDirection direction,
            String description
    ) {
        public enum ImpactDirection {
            POSITIVE,    // 正面影响
            NEGATIVE,    // 负面影响
            NEUTRAL      // 中性影响
        }
    }
}