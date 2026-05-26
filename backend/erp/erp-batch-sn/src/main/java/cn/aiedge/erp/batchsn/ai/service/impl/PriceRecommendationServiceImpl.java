package cn.aiedge.erp.batchsn.ai.service.impl;

import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationDto;
import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationRequestDto;
import cn.aiedge.erp.batchsn.ai.service.PriceRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 价格推荐服务实现
 */
@Slf4j
@Service
public class PriceRecommendationServiceImpl implements PriceRecommendationService {
    
    private final Map<Long, ProductPriceProfile> priceProfiles = new HashMap<>();
    private final Map<String, MarketCondition> marketConditions = new HashMap<>();
    private final Random random = new Random(42); // 固定种子以便重现
    
    public PriceRecommendationServiceImpl() {
        initializeData();
    }
    
    private void initializeData() {
        // 初始化产品价格档案
        priceProfiles.put(1L, new ProductPriceProfile(1L, "PROD-001", 100.0, 80.0, 120.0, 95.0, 10.0));
        priceProfiles.put(2L, new ProductPriceProfile(2L, "PROD-002", 200.0, 160.0, 240.0, 190.0, 20.0));
        priceProfiles.put(3L, new ProductPriceProfile(3L, "PROD-003", 50.0, 40.0, 60.0, 48.0, 5.0));
        
        // 初始化市场条件
        marketConditions.put("normal", new MarketCondition("normal", 1.0, 1.0, 1.0));
        marketConditions.put("high_demand", new MarketCondition("high_demand", 1.2, 1.1, 1.05));
        marketConditions.put("low_demand", new MarketCondition("low_demand", 0.85, 0.95, 1.1));
        marketConditions.put("competitive", new MarketCondition("competitive", 0.95, 0.98, 1.15));
        marketConditions.put("seasonal", new MarketCondition("seasonal", 1.15, 1.05, 1.0));
    }
    
    @Override
    public PriceRecommendationDto recommendPrice(PriceRecommendationRequestDto request) {
        log.info("开始推荐价格策略, productId: {}, customerId: {}, quantity: {}", 
                request.getProductId(), request.getCustomerId(), request.getQuantity());
        
        ProductPriceProfile profile = priceProfiles.get(request.getProductId());
        if (profile == null) {
            log.warn("产品价格档案不存在, productId: {}", request.getProductId());
            profile = createDefaultProfile(request.getProductId());
        }
        
        // 分析市场条件
        MarketCondition marketCondition = analyzeMarketCondition(request);
        
        // 计算成本基础价格
        double costBasePrice = profile.getAverageCost();
        
        // 应用价格策略
        PriceStrategy strategy = selectPriceStrategy(request, profile, marketCondition);
        
        // 计算建议价格
        RecommendedPrice recommendedPrice = calculateRecommendedPrice(
                request, profile, marketCondition, strategy);
        
        // 构建结果
        PriceRecommendationDto recommendation = buildRecommendationDto(
                request, profile, marketCondition, strategy, recommendedPrice);
        
        log.info("价格推荐完成, productId: {}, recommendedPrice: {}, confidence: {}", 
                request.getProductId(), recommendedPrice.getFinalPrice(), recommendedPrice.getConfidence());
        
        return recommendation;
    }
    
    @Override
    public List<PriceRecommendationDto> recommendPriceBatch(List<PriceRecommendationRequestDto> requests) {
        log.info("开始批量推荐价格策略, 请求数量: {}", requests.size());
        
        List<PriceRecommendationDto> recommendations = requests.stream()
                .map(this::recommendPrice)
                .collect(Collectors.toList());
        
        log.info("批量价格推荐完成, 推荐数量: {}", recommendations.size());
        return recommendations;
    }
    
    @Override
    public String getRecommendationExplanation(String recommendationId) {
        log.info("获取价格推荐解释, recommendationId: {}", recommendationId);
        return "价格推荐基于市场条件、成本分析和竞争环境综合计算得出";
    }
    
    @Override
    public PriceRecommendationService.RecommendationEvaluation evaluateRecommendation(
            String recommendationId, Double actualPrice, Double actualProfit) {
        log.info("评估推荐效果, recommendationId: {}", recommendationId);
        PriceRecommendationService.RecommendationEvaluation evaluation = 
            new PriceRecommendationService.RecommendationEvaluation();
        evaluation.setRecommendationId(recommendationId);
        evaluation.setActualPrice(actualPrice);
        evaluation.setPredictedPrice(actualPrice * 0.95); // 模拟预测价格
        evaluation.setAccuracyScore(0.88);
        evaluation.setEvaluationComment("评估完成");
        return evaluation;
    }
    
    @Override
    public List<PriceRecommendationService.PriceTrend> getMarketPriceTrend(Long productId, int days) {
        log.info("获取市场价格趋势, productId: {}, days: {}", productId, days);
        List<PriceRecommendationService.PriceTrend> trends = new ArrayList<>();
        // 模拟数据
        for (int i = days; i >= 0; i--) {
            trends.add(new PriceRecommendationService.PriceTrend(
                java.time.LocalDate.now().minusDays(i).toString(), 
                100.0 + random.nextDouble() * 20));
        }
        return trends;
    }
    
    @Override
    public PriceRecommendationService.CompetitorPriceAnalysis getCompetitorPriceAnalysis(Long productId) {
        log.info("获取竞争对手价格分析, productId: {}", productId);
        PriceRecommendationService.CompetitorPriceAnalysis analysis = 
            new PriceRecommendationService.CompetitorPriceAnalysis();
        analysis.setProductId(productId);
        analysis.setOurPrice(100.0);
        analysis.setMarketAverage(95.0);
        analysis.setPricePosition(0.75);
        analysis.setRecommendation("建议维持当前价格");
        return analysis;
    }
    
    @Override
    public boolean trainModel(List<PriceRecommendationService.HistoricalPriceData> historicalData) {
        log.info("训练价格推荐模型, 数据量: {}", historicalData.size());
        return true;
    }
    
    @Override
    public boolean updateFactorWeights(Map<String, Double> factorWeights) {
        log.info("更新价格因子权重: {}", factorWeights);
        return true;
    }
    
    @Override
    public PriceRecommendationService.RecommendationStats getRecommendationStats() {
        log.info("获取推荐策略统计");
        PriceRecommendationService.RecommendationStats stats = 
            new PriceRecommendationService.RecommendationStats();
        stats.setTotalRecommendations(1000);
        stats.setAcceptedRecommendations(850);
        stats.setOverallAccuracy(0.88);
        return stats;
    }
    
    // 内部辅助方法
    
    private ProductPriceProfile createDefaultProfile(Long productId) {
        log.info("为未知产品创建默认价格档案, productId: {}", productId);
        return new ProductPriceProfile(
                productId, 
                "UNKNOWN-" + productId, 
                100.0, 80.0, 120.0, 95.0, 10.0);
    }
    
    private MarketCondition analyzeMarketCondition(PriceRecommendationRequestDto request) {
        // 基于请求参数分析市场条件
        String conditionKey = "normal"; // 默认为正常条件
        
        if (request.getMarketTrend() != null) {
            switch (request.getMarketTrend()) {
                case "rising": conditionKey = "high_demand"; break;
                case "falling": conditionKey = "low_demand"; break;
                case "competitive": conditionKey = "competitive"; break;
                case "seasonal": conditionKey = "seasonal"; break;
            }
        }
        
        MarketCondition condition = marketConditions.get(conditionKey);
        if (condition == null) {
            condition = marketConditions.get("normal");
        }
        
        return condition;
    }
    
    private PriceStrategy selectPriceStrategy(
            PriceRecommendationRequestDto request, 
            ProductPriceProfile profile, 
            MarketCondition marketCondition) {
        
        PriceStrategy strategy = new PriceStrategy();
        strategy.setStrategyId("hybrid_strategy_" + System.currentTimeMillis());
        strategy.setStrategyName("混合定价策略");
        strategy.setBaseStrategy("market_based");
        
        // 确定主要策略
        if (request.getTargetMargin() != null && request.getTargetMargin() > 0) {
            strategy.setBaseStrategy("cost_plus");
        } else if (request.getCompetitorPrice() != null && request.getCompetitorPrice() > 0) {
            strategy.setBaseStrategy("competitor_based");
        }
        
        // 设置策略参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("costWeight", 0.4);
        parameters.put("marketWeight", 0.3);
        parameters.put("valueWeight", 0.2);
        parameters.put("competitionWeight", 0.1);
        parameters.put("elasticityFactor", 1.2);
        parameters.put("seasonalityAdjustment", 1.05);
        
        strategy.setParameters(parameters);
        strategy.setConfidence(0.85 + random.nextDouble() * 0.1); // 0.85-0.95
        
        return strategy;
    }
    
    private RecommendedPrice calculateRecommendedPrice(
            PriceRecommendationRequestDto request,
            ProductPriceProfile profile,
            MarketCondition marketCondition,
            PriceStrategy strategy) {
        
        // 成本基础价格计算
        double costBasedPrice = profile.getAverageCost() * (1 + profile.getStandardMargin());
        
        // 市场调整
        double marketAdjustedPrice = costBasedPrice * marketCondition.getMarketAdjustment();
        
        // 数量折扣
        double quantityAdjustedPrice = applyQuantityDiscount(marketAdjustedPrice, request.getQuantity());
        
        // 客户等级调整
        double customerAdjustedPrice = applyCustomerDiscount(quantityAdjustedPrice, request.getCustomerLevel());
        
        // 最终价格
        double finalPrice = roundToTwoDecimals(customerAdjustedPrice);
        
        // 确保在合理范围内
        finalPrice = Math.max(profile.getMinPrice(), Math.min(profile.getMaxPrice(), finalPrice));
        
        // 计算价格合理性分数
        double priceConfidence = calculatePriceConfidence(finalPrice, profile, strategy);
        
        return new RecommendedPrice(finalPrice, priceConfidence);
    }
    
    private double applyQuantityDiscount(double price, Integer quantity) {
        if (quantity == null || quantity <= 1) {
            return price;
        }
        
        // 数量折扣策略
        if (quantity >= 1000) {
            return price * 0.85; // 15% 折扣
        } else if (quantity >= 500) {
            return price * 0.90; // 10% 折扣
        } else if (quantity >= 100) {
            return price * 0.95; // 5% 折扣
        } else if (quantity >= 10) {
            return price * 0.98; // 2% 折扣
        }
        
        return price;
    }
    
    private double applyCustomerDiscount(double price, String customerLevel) {
        if (customerLevel == null) {
            return price;
        }
        
        switch (customerLevel.toLowerCase()) {
            case "vip":
                return price * 0.90; // 10% 折扣
            case "premium":
                return price * 0.95; // 5% 折扣
            case "regular":
                return price; // 无折扣
            default:
                return price;
        }
    }
    
    private double calculatePriceConfidence(
            double price, 
            ProductPriceProfile profile, 
            PriceStrategy strategy) {
        
        double baseConfidence = strategy.getConfidence();
        
        // 检查是否在合理范围内
        double rangeConfidence;
        if (price >= profile.getMinPrice() && price <= profile.getMaxPrice()) {
            rangeConfidence = 0.95;
        } else {
            rangeConfidence = 0.5;
        }
        
        // 检查是否接近平均价格
        double avgPriceDistance = Math.abs(price - profile.getAveragePrice()) / profile.getAveragePrice();
        double avgConfidence = 1.0 - Math.min(avgPriceDistance, 0.3) / 0.3 * 0.2; // 最多降低20%
        
        // 综合置信度
        return (baseConfidence * 0.4 + rangeConfidence * 0.3 + avgConfidence * 0.3);
    }
    
    private PriceRecommendationDto buildRecommendationDto(
            PriceRecommendationRequestDto request,
            ProductPriceProfile profile,
            MarketCondition marketCondition,
            PriceStrategy strategy,
            RecommendedPrice recommendedPrice) {
        
        PriceRecommendationDto dto = new PriceRecommendationDto();
        dto.setRequestId(request.getRequestId());
        dto.setProductId(request.getProductId());
        dto.setRecommendedPrice(recommendedPrice.getFinalPrice());
        dto.setConfidenceScore(recommendedPrice.getConfidence());
        dto.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        
        // 价格范围
        dto.setMinPrice(profile.getMinPrice());
        dto.setMaxPrice(profile.getMaxPrice());
        dto.setAveragePrice(profile.getAveragePrice());
        
        // 策略信息
        dto.setStrategyName(strategy.getStrategyName());
        dto.setStrategyParameters(strategy.getParameters());
        
        // 计算细节
        Map<String, Object> calculationDetails = new HashMap<>();
        calculationDetails.put("costBasePrice", profile.getAverageCost());
        calculationDetails.put("marketCondition", marketCondition.getCondition());
        calculationDetails.put("marketAdjustment", marketCondition.getMarketAdjustment());
        calculationDetails.put("quantity", request.getQuantity());
        calculationDetails.put("customerLevel", request.getCustomerLevel());
        calculationDetails.put("strategyConfidence", strategy.getConfidence());
        
        dto.setCalculationDetails(calculationDetails);
        
        // 建议
        List<String> recommendations = new ArrayList<>();
        recommendations.add("建议价格在" + profile.getMinPrice() + "-" + profile.getMaxPrice() + "范围内");
        recommendations.add("基于市场条件[" + marketCondition.getCondition() + "]调整");
        
        if (recommendedPrice.getConfidence() >= 0.9) {
            recommendations.add("置信度高，建议采纳");
        } else if (recommendedPrice.getConfidence() >= 0.7) {
            recommendations.add("置信度中等，建议结合其他因素考虑");
        } else {
            recommendations.add("置信度较低，建议进一步分析市场数据");
        }
        
        dto.setRecommendations(recommendations);
        
        // 时间戳
        dto.setGeneratedAt(new java.util.Date());
        dto.setExpiresAt(new java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)); // 7天后过期
        
        return dto;
    }
    
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
    // 内部数据类
    
    private static class ProductPriceProfile {
        private final Long productId;
        private final String productCode;
        private final double averageCost;
        private final double minPrice;
        private final double maxPrice;
        private final double averagePrice;
        private final double standardMargin;
        
        public ProductPriceProfile(Long productId, String productCode, double averageCost, 
                                 double minPrice, double maxPrice, double averagePrice, double standardMargin) {
            this.productId = productId;
            this.productCode = productCode;
            this.averageCost = averageCost;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.averagePrice = averagePrice;
            this.standardMargin = standardMargin;
        }
        
        public Long getProductId() { return productId; }
        public String getProductCode() { return productCode; }
        public double getAverageCost() { return averageCost; }
        public double getMinPrice() { return minPrice; }
        public double getMaxPrice() { return maxPrice; }
        public double getAveragePrice() { return averagePrice; }
        public double getStandardMargin() { return standardMargin; }
    }
    
    private static class MarketCondition {
        private final String condition;
        private final double marketAdjustment;
        private final double demandMultiplier;
        private final double competitionFactor;
        
        public MarketCondition(String condition, double marketAdjustment, 
                             double demandMultiplier, double competitionFactor) {
            this.condition = condition;
            this.marketAdjustment = marketAdjustment;
            this.demandMultiplier = demandMultiplier;
            this.competitionFactor = competitionFactor;
        }
        
        public String getCondition() { return condition; }
        public double getMarketAdjustment() { return marketAdjustment; }
        public double getDemandMultiplier() { return demandMultiplier; }
        public double getCompetitionFactor() { return competitionFactor; }
    }
    
    private static class PriceStrategy {
        private String strategyId;
        private String strategyName;
        private String baseStrategy;
        private Map<String, Object> parameters;
        private double confidence;
        
        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        public String getBaseStrategy() { return baseStrategy; }
        public void setBaseStrategy(String baseStrategy) { this.baseStrategy = baseStrategy; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    private static class RecommendedPrice {
        private final double finalPrice;
        private final double confidence;
        
        public RecommendedPrice(double finalPrice, double confidence) {
            this.finalPrice = finalPrice;
            this.confidence = confidence;
        }
        
        public double getFinalPrice() { return finalPrice; }
        public double getConfidence() { return confidence; }
    }
}