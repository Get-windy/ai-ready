package cn.aiedge.erp.price.engine.strategy.algorithm.pricing;

import cn.aiedge.erp.price.engine.strategy.PricingAlgorithm;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return PricingStrategy.StrategyType.MARKET_BASED;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        boolean hasMarketData = request.getMarketReferencePrice() != null || 
                               request.getCompetitorPrices() != null;
        
        return hasMarketData;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.MARKET_BASED) {
            throw new IllegalArgumentException("策略类型不匹配，期望: MARKET_BASED, 实际: " + strategy.getStrategyType());
        }
        
        // 计算市场基准价格
        BigDecimal marketBasePrice = calculateMarketBasePrice(request);
        
        // 计算市场系数
        BigDecimal marketFactor = calculateMarketFactor(request, strategy.getParameters());
        
        BigDecimal finalPrice = marketBasePrice.multiply(marketFactor)
                                              .setScale(2, RoundingMode.HALF_UP);
        
        // 如果有成本数据，计算毛利润
        BigDecimal cost = request.getCostPrice();
        BigDecimal grossProfitMargin = null;
        
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            grossProfitMargin = finalPrice.subtract(cost)
                    .divide(finalPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(marketBasePrice);
        result.setFinalPrice(finalPrice);
        result.setCostPrice(cost);
        result.setGrossProfitMargin(grossProfitMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setCalculationExplanation(String.format(
            "市场基准价: %s, 市场系数: %.2f, 最终价格: %s",
            marketBasePrice.toPlainString(),
            marketFactor.doubleValue(),
            finalPrice.toPlainString()
        ));
        result.setSuccess(true);
        
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
        
        if (request.getMarketReferencePrice() != null) {
            score += 25;
        }
        
        if (request.getCustomerLevel() != null) {
            switch (request.getCustomerLevel().toUpperCase()) {
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
        if (request.getMarketReferencePrice() != null) {
            marketBasePrice = request.getMarketReferencePrice();
        }
        
        if (marketBasePrice == null && request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty()) {
            List<BigDecimal> priceList = new ArrayList<>(request.getCompetitorPrices().values());
            marketBasePrice = calculateMedianPrice(priceList);
        }
        
        if (marketBasePrice == null && request.getBasePrice() != null) {
            marketBasePrice = request.getBasePrice();
        }
        
        if (marketBasePrice == null && request.getCostPrice() != null) {
            marketBasePrice = request.getCostPrice().multiply(new BigDecimal("1.20"))
                                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        if (marketBasePrice == null) {
            throw new IllegalArgumentException("无法计算市场基准价格，缺少必要的市场数据");
        }
        
        return marketBasePrice;
    }
    
    private BigDecimal calculateMarketFactor(PriceCalculationRequest request, Map<String, Object> parameters) {
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
        
        BigDecimal minFactor = extractParameter(parameters, "minMarketFactor", new BigDecimal("0.5"));
        BigDecimal maxFactor = extractParameter(parameters, "maxMarketFactor", new BigDecimal("2.0"));
        
        if (adjustedFactor.compareTo(minFactor) < 0) {
            adjustedFactor = minFactor;
        } else if (adjustedFactor.compareTo(maxFactor) > 0) {
            adjustedFactor = maxFactor;
        }
        
        return adjustedFactor.setScale(4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateCompetitorFactor(PriceCalculationRequest request) {
        BigDecimal marketBasePrice = calculateMarketBasePrice(request);
        Map<String, BigDecimal> competitorPrices = request.getCompetitorPrices();
        
        if (competitorPrices == null || competitorPrices.isEmpty()) {
            return BigDecimal.ONE;
        }
        
        List<BigDecimal> priceList = new ArrayList<>(competitorPrices.values());
        BigDecimal competitorMedian = calculateMedianPrice(priceList);
        
        if (competitorMedian == null || competitorMedian.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        
        BigDecimal ratio = marketBasePrice.divide(competitorMedian, 4, RoundingMode.HALF_UP);
        
        if (ratio.compareTo(new BigDecimal("1.1")) > 0) {
            return new BigDecimal("0.95");
        } else if (ratio.compareTo(new BigDecimal("0.9")) < 0) {
            return new BigDecimal("1.05");
        } else {
            return BigDecimal.ONE;
        }
    }
    
    private BigDecimal calculateDemandFactor(PriceCalculationRequest request) {
        return BigDecimal.ONE;
    }
    
    private BigDecimal calculateSeasonalFactor(PriceCalculationRequest request) {
        return BigDecimal.ONE;
    }
    
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
    
    private BigDecimal extractMarketFactor(Map<String, Object> parameters) {
        return extractParameter(parameters, "marketFactor", new BigDecimal("1.0"));
    }
    
    private BigDecimal extractParameter(Map<String, Object> parameters, String fieldName, BigDecimal defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        
        try {
            Object value = parameters.get(fieldName);
            if (value != null) {
                if (value instanceof BigDecimal) {
                    return (BigDecimal) value;
                } else if (value instanceof Number) {
                    return new BigDecimal(value.toString());
                } else if (value instanceof String) {
                    return new BigDecimal((String) value);
                }
            }
        } catch (Exception e) {
        }
        
        return defaultValue;
    }
}