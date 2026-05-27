package cn.aiedge.erp.price.engine.strategy.algorithm.pricing;

import cn.aiedge.erp.price.engine.strategy.PricingAlgorithm;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class CompetitivePricingStrategy implements PricingAlgorithm {
    
    private static final String DEFAULT_PARAMETERS = "{\"competitionIntensity\":0.7,\"minPriceRatio\":0.8,\"maxPriceRatio\":1.2}";
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.COMPETITIVE_BASED;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        Map<String, BigDecimal> competitorPrices = request.getCompetitorPrices();
        return competitorPrices != null && !competitorPrices.isEmpty();
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.COMPETITIVE_BASED) {
            throw new IllegalArgumentException("策略类型不匹配，期望: COMPETITIVE_BASED, 实际: " + strategy.getStrategyType());
        }
        
        BigDecimal competitionBasePrice = calculateCompetitionBasePrice(request);
        
        BigDecimal priceAdjustmentRatio = extractParameter(strategy.getParameters(), "competitionIntensity", new BigDecimal("0.95"));
        
        BigDecimal finalPrice = competitionBasePrice.multiply(priceAdjustmentRatio)
                                                   .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal cost = request.getCostPrice();
        BigDecimal grossProfitMargin = null;
        
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            grossProfitMargin = finalPrice.subtract(cost)
                    .divide(finalPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(competitionBasePrice);
        result.setFinalPrice(finalPrice);
        result.setCostPrice(cost);
        result.setGrossProfitMargin(grossProfitMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setCalculationExplanation(String.format(
            "竞争基准价: %s, 调整系数: %.2f, 最终价格: %s",
            competitionBasePrice.toPlainString(),
            priceAdjustmentRatio.doubleValue(),
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
        
        int score = 45;
        
        Map<String, BigDecimal> competitorPrices = request.getCompetitorPrices();
        if (competitorPrices != null && !competitorPrices.isEmpty()) {
            score += 35;
            int competitorCount = competitorPrices.size();
            if (competitorCount >= 5) {
                score += 20;
            } else if (competitorCount >= 3) {
                score += 15;
            } else {
                score += 10;
            }
        }
        
        return score;
    }
    
    @Override
    public String getDescription() {
        return "竞争性定价算法：基于竞争对手价格和市场位置定价。适用于竞争激烈的市场。";
    }
    
    @Override
    public String getDefaultParameters() {
        return DEFAULT_PARAMETERS;
    }
    
    private BigDecimal calculateCompetitionBasePrice(PriceCalculationRequest request) {
        BigDecimal competitionBasePrice = null;
        
        Map<String, BigDecimal> competitorPrices = request.getCompetitorPrices();
        if (competitorPrices != null && !competitorPrices.isEmpty()) {
            competitionBasePrice = calculateMedianPrice(competitorPrices);
        }
        
        if (competitionBasePrice == null) {
            competitionBasePrice = request.getMarketReferencePrice();
        }
        
        if (competitionBasePrice == null) {
            BigDecimal cost = request.getCostPrice();
            if (cost != null) {
                competitionBasePrice = cost.multiply(new BigDecimal("1.15"))
                                         .setScale(2, RoundingMode.HALF_UP);
            }
        }
        
        if (competitionBasePrice == null) {
            throw new IllegalArgumentException("无法计算竞争基准价格，缺少必要的竞争数据");
        }
        
        return competitionBasePrice;
    }
    
    private BigDecimal calculateMedianPrice(Map<String, BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        
        List<BigDecimal> priceList = new java.util.ArrayList<>(prices.values());
        priceList.sort(BigDecimal::compareTo);
        
        int size = priceList.size();
        if (size % 2 == 0) {
            BigDecimal middle1 = priceList.get(size / 2 - 1);
            BigDecimal middle2 = priceList.get(size / 2);
            return middle1.add(middle2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else {
            return priceList.get(size / 2);
        }
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