package cn.aiedge.erp.price.engine.strategy.algorithm.pricing;

import cn.aiedge.erp.price.engine.strategy.PricingAlgorithm;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ValueBasedPricingStrategy implements PricingAlgorithm {
    
    private static final String DEFAULT_PARAMETERS = "{\"valueMultiplier\":2.5,\"minValueMultiplier\":1.5,\"maxValueMultiplier\":5.0}";
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.VALUE_BASED;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        BigDecimal basePrice = request.getBasePrice();
        return basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.VALUE_BASED) {
            throw new IllegalArgumentException("策略类型不匹配，期望: VALUE_BASED, 实际: " + strategy.getStrategyType());
        }
        
        BigDecimal valueBasePrice = request.getBasePrice();
        if (valueBasePrice == null || valueBasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            valueBasePrice = request.getMarketReferencePrice();
        }
        if (valueBasePrice == null || valueBasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            valueBasePrice = request.getCostPrice();
            if (valueBasePrice != null) {
                valueBasePrice = valueBasePrice.multiply(new BigDecimal("3.0"));
            }
        }
        
        if (valueBasePrice == null) {
            throw new IllegalArgumentException("无法计算价值基准价格，缺少必要的产品价值数据");
        }
        
        BigDecimal valueMultiplier = extractValueMultiplier(strategy.getParameters());
        
        BigDecimal finalPrice = valueBasePrice.multiply(valueMultiplier)
                                             .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal cost = request.getCostPrice();
        BigDecimal grossProfitMargin = null;
        
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            grossProfitMargin = finalPrice.subtract(cost)
                    .divide(finalPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(valueBasePrice);
        result.setFinalPrice(finalPrice);
        result.setCostPrice(cost);
        result.setGrossProfitMargin(grossProfitMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setCalculationExplanation(String.format(
            "价值基准价: %s, 价值乘数: %.2f, 最终价格: %s",
            valueBasePrice.toPlainString(),
            valueMultiplier.doubleValue(),
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
        
        int score = 60;
        
        String customerLevel = request.getCustomerLevel();
        if (customerLevel != null) {
            switch (customerLevel.toUpperCase()) {
                case "VIP":
                case "PREMIUM":
                    score += 35;
                    break;
                case "GOLD":
                    score += 25;
                    break;
                case "SILVER":
                    score += 15;
                    break;
                default:
                    score += 5;
            }
        }
        
        return score;
    }
    
    @Override
    public String getDescription() {
        return "价值导向定价算法：基于产品为客户创造的价值定价。适用于高价值、差异化、品牌产品。";
    }
    
    @Override
    public String getDefaultParameters() {
        return DEFAULT_PARAMETERS;
    }
    
    private BigDecimal extractValueMultiplier(Map<String, Object> parameters) {
        return extractParameter(parameters, "valueMultiplier", new BigDecimal("2.5"));
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
    
    public BigDecimal getMinValueMultiplier(Map<String, Object> parameters) {
        return extractParameter(parameters, "minValueMultiplier", new BigDecimal("1.5"));
    }
    
    public BigDecimal getMaxValueMultiplier(Map<String, Object> parameters) {
        return extractParameter(parameters, "maxValueMultiplier", new BigDecimal("5.0"));
    }
}