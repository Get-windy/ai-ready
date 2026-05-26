package cn.aiedge.erp.price.engine.strategy.algorithm.pricing;

import cn.aiedge.erp.price.engine.strategy.PricingAlgorithm;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 市场导向定价算法
 * 价格 = 基准价 × 市场系数
 * 基于市场竞争情况和市场行情定价
 */
@Component
public class MarketBasedPricingStrategy implements PricingAlgorithm {
    
    private static final String DEFAULT_PARAMETERS = "{\"marketFactor\":1.0,\"minMarketFactor\":0.5,\"maxMarketFactor\":2.0,\"competitorWeight\":0.4,\"demandWeight\":0.3,\"seasonWeight\":0.3}";
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.MARKET_ORIENTED;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        // 市场导向定价适用于有市场数据或竞争情况的场景
        boolean hasMarketData = request.getMarketAveragePrice() != null || 
                               request.getCompetitorPrices() != null ||
                               (request.getMarketFactors() != null && !request.getMarketFactors().isEmpty());
        
        boolean isCompetitiveProduct = request.getProductType() == null ||
                                      request.getProductType().toUpperCase().contains("COMMODITY") ||
                                      request.getProductType().toUpperCase().contains("STANDARD");
        
        return hasMarketData && isCompetitiveProduct;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // 验证策略类型
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.MARKET_ORIENTED) {
            throw new IllegalArgumentException("策略类型不匹配，期望: MARKET_ORIENTED, 实际: " + strategy.getStrategyType());
        }
        
        // 计算市场基准价格
        BigDecimal marketBasePrice = calculateMarketBasePrice(request);
        
        // 计算市场系数
        BigDecimal marketFactor = calculateMarketFactor(request, strategy.getParameters());
        
        // 计算最终价格
        BigDecimal finalPrice = marketBasePrice.multiply(marketFactor)
                                              .setScale(2, RoundingMode.HALF_UP);
        
        // 如果有成本数据，计算毛利润
        BigDecimal cost = request.getProductCost();
        BigDecimal grossProfit = null;
        BigDecimal grossMargin = null;
        
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            grossProfit = finalPrice.subtract(cost);
            grossMargin = grossProfit.divide(finalPrice, 4, RoundingMode.HALF_UP);
        }
        
        // 创建计算结果
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(marketBasePrice);
        result.setFinalPrice(finalPrice);
        result.setGrossProfit(grossProfit);
        result.setGrossMargin(grossMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setStrategyApplied("市场导向定价算法");
        result.setStrategyType(PricingStrategy.StrategyType.MARKET_ORIENTED.name());
        result.setCalculationNotes(String.format(
            "市场基准价: %s, 市场系数: %.2f, 最终价格: %s",
            marketBasePrice.toPlainString(),
            marketFactor.doubleValue(),
            finalPrice.toPlainString()
        ));
        result.setSuccess(true);
        result.setErrorCode(0);
        result.setErrorMessage("");
        
        return result;
    }
    
    @Override
    public int calculatePriorityScore(PriceCalculationRequest request) {
        if (!isApplicable(request)) {
            return 0;
        }
        
        int score = 40; // 基础分数
        
        // 根据市场竞争情况调整分数
        if (request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty()) {
            score += 30;
        }
        
        if (request.getMarketAveragePrice() != null) {
            score += 25;
        }
        
        // 根据客户等级调整分数
        if (request.getCustomerGrade() != null) {
            switch (request.getCustomerGrade().toUpperCase()) {
                case "GOLD":
                case "SILVER":
                    score += 20;
                    break;
                case "STANDARD":
                    score += 15;
                    break;
                case "VIP":
                case "PREMIUM":
                    score += 10;
                    break;
            }
        }
        
        // 根据产品类型调整分数
        if (request.getProductType() != null) {
            String productType = request.getProductType().toUpperCase();
            if (productType.contains("COMMODITY") || productType.contains("COMPETITIVE")) {
                score += 25;
            } else if (productType.contains("STANDARD")) {
                score += 20;
            }
        }
        
        // 如果有市场因素数据，增加分数
        if (request.getMarketFactors() != null && !request.getMarketFactors().isEmpty()) {
            score += 20;
        }
        
        return score;
    }
    
    @Override
    public String getDescription() {
        return "市场导向定价算法：价格 = 市场基准价 × 市场系数。基于市场竞争情况、供求关系、季节性因素等市场数据定价。";
    }
    
    @Override
    public String getDefaultParameters() {
        return DEFAULT_PARAMETERS;
    }
    
    /**
     * 计算市场基准价格
     */
    private BigDecimal calculateMarketBasePrice(PriceCalculationRequest request) {
        BigDecimal marketBasePrice = null;
        
        // 1. 首先使用市场平均价
        if (request.getMarketAveragePrice() != null) {
            marketBasePrice = request.getMarketAveragePrice();
        }
        
        // 2. 如果没有市场平均价，使用竞争对手价格的中位数
        if (marketBasePrice == null && request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty()) {
            marketBasePrice = calculateMedianPrice(request.getCompetitorPrices());
        }
        
        // 3. 如果都没有，使用产品标准价
        if (marketBasePrice == null && request.getProductStandardPrice() != null) {
            marketBasePrice = request.getProductStandardPrice();
        }
        
        // 4. 如果还是没有，使用产品成本加成
        if (marketBasePrice == null && request.getProductCost() != null) {
            // 默认20%利润率
            marketBasePrice = request.getProductCost().multiply(new BigDecimal("1.20"))
                                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        if (marketBasePrice == null) {
            throw new IllegalArgumentException("无法计算市场基准价格，缺少必要的市场数据");
        }
        
        return marketBasePrice;
    }
    
    /**
     * 计算市场系数
     */
    private BigDecimal calculateMarketFactor(PriceCalculationRequest request, String parameters) {
        BigDecimal baseFactor = extractMarketFactor(parameters);
        
        // 应用市场因素调整
        BigDecimal adjustedFactor = baseFactor;
        
        // 1. 竞争对手价格影响
        if (request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty()) {
            BigDecimal competitorFactor = calculateCompetitorFactor(request);
            BigDecimal competitorWeight = extractParameter(parameters, "competitorWeight", new BigDecimal("0.4"));
            adjustedFactor = adjustedFactor.multiply(
                BigDecimal.ONE.add(competitorFactor.subtract(BigDecimal.ONE).multiply(competitorWeight))
            );
        }
        
        // 2. 市场需求影响
        if (request.getMarketFactors() != null && request.getMarketFactors().containsKey("demandLevel")) {
            BigDecimal demandFactor = calculateDemandFactor(request);
            BigDecimal demandWeight = extractParameter(parameters, "demandWeight", new BigDecimal("0.3"));
            adjustedFactor = adjustedFactor.multiply(
                BigDecimal.ONE.add(demandFactor.subtract(BigDecimal.ONE).multiply(demandWeight))
            );
        }
        
        // 3. 季节性影响
        if (request.getSeasonalFactors() != null && !request.getSeasonalFactors().isEmpty()) {
            BigDecimal seasonalFactor = calculateSeasonalFactor(request);
            BigDecimal seasonWeight = extractParameter(parameters, "seasonWeight", new BigDecimal("0.3"));
            adjustedFactor = adjustedFactor.multiply(
                BigDecimal.ONE.add(seasonalFactor.subtract(BigDecimal.ONE).multiply(seasonWeight))
            );
        }
        
        // 确保系数在有效范围内
        BigDecimal minFactor = extractParameter(parameters, "minMarketFactor", new BigDecimal("0.5"));
        BigDecimal maxFactor = extractParameter(parameters, "maxMarketFactor", new BigDecimal("2.0"));
        
        if (adjustedFactor.compareTo(minFactor) < 0) {
            adjustedFactor = minFactor;
        } else if (adjustedFactor.compareTo(maxFactor) > 0) {
            adjustedFactor = maxFactor;
        }
        
        return adjustedFactor.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算竞争对手价格影响系数
     */
    private BigDecimal calculateCompetitorFactor(PriceCalculationRequest request) {
        BigDecimal marketBasePrice = calculateMarketBasePrice(request);
        BigDecimal competitorMedian = calculateMedianPrice(request.getCompetitorPrices());
        
        if (competitorMedian == null || competitorMedian.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        
        // 如果我们的基准价高于竞争对手中位数，可能需要降价
        // 如果低于竞争对手中位数，可以适当提价
        BigDecimal ratio = marketBasePrice.divide(competitorMedian, 4, RoundingMode.HALF_UP);
        
        if (ratio.compareTo(new BigDecimal("1.1")) > 0) {
            // 价格比竞争对手高10%以上，建议降价系数
            return new BigDecimal("0.95");
        } else if (ratio.compareTo(new BigDecimal("0.9")) < 0) {
            // 价格比竞争对手低10%以上，可以适当提价
            return new BigDecimal("1.05");
        } else {
            return BigDecimal.ONE;
        }
    }
    
    /**
     * 计算市场需求影响系数
     */
    private BigDecimal calculateDemandFactor(PriceCalculationRequest request) {
        String demandLevel = request.getMarketFactors().get("demandLevel");
        if (demandLevel == null) {
            return BigDecimal.ONE;
        }
        
        switch (demandLevel.toUpperCase()) {
            case "HIGH":
            case "VERY_HIGH":
                return new BigDecimal("1.15"); // 需求旺盛，可以提价
            case "MEDIUM":
                return new BigDecimal("1.05");
            case "LOW":
                return new BigDecimal("0.95");
            case "VERY_LOW":
                return new BigDecimal("0.85"); // 需求低迷，需要降价
            default:
                return BigDecimal.ONE;
        }
    }
    
    /**
     * 计算季节性影响系数
     */
    private BigDecimal calculateSeasonalFactor(PriceCalculationRequest request) {
        // 这里可以扩展为更复杂的季节性计算
        // 简单实现：如果有季节性因素，根据季度调整
        if (request.getSeasonalFactors() != null) {
            BigDecimal seasonalFactor = request.getSeasonalFactors().values().stream()
                    .findFirst()
                    .orElse(BigDecimal.ONE);
            return seasonalFactor;
        }
        
        return BigDecimal.ONE;
    }
    
    /**
     * 计算价格中位数
     */
    private BigDecimal calculateMedianPrice(java.util.List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        
        java.util.List<BigDecimal> sortedPrices = new java.util.ArrayList<>(prices);
        sortedPrices.sort(BigDecimal::compareTo);
        
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            // 偶数个，取中间两个的平均值
            BigDecimal middle1 = sortedPrices.get(size / 2 - 1);
            BigDecimal middle2 = sortedPrices.get(size / 2);
            return middle1.add(middle2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else {
            // 奇数个，取中间值
            return sortedPrices.get(size / 2);
        }
    }
    
    /**
     * 提取市场系数
     */
    private BigDecimal extractMarketFactor(String parameters) {
        return extractParameter(parameters, "marketFactor", new BigDecimal("1.0"));
    }
    
    /**
     * 从参数中提取指定字段的值
     */
    private BigDecimal extractParameter(String parameters, String fieldName, BigDecimal defaultValue) {
        if (parameters == null || !parameters.contains("\"" + fieldName + "\":")) {
            return defaultValue;
        }
        
        try {
            String[] parts = parameters.split("\"" + fieldName + "\":");
            if (parts.length > 1) {
                String valuePart = parts[1].split(",")[0].trim();
                if (valuePart.endsWith("}") || valuePart.endsWith("]")) {
                    valuePart = valuePart.substring(0, valuePart.length() - 1);
                }
                return new BigDecimal(valuePart);
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return defaultValue;
    }
}