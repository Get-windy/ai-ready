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
public class CostBasedPricingStrategy implements PricingAlgorithm {
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.COST_BASED;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        BigDecimal cost = request.getCostPrice();
        return cost != null && cost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.COST_BASED) {
            throw new IllegalArgumentException("策略类型不匹配，期望: COST_BASED, 实际: " + strategy.getStrategyType());
        }
        
        BigDecimal cost = request.getCostPrice();
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("产品成本不能为空或小于等于0");
        }
        
        BigDecimal profitMargin = extractProfitMargin(strategy.getParameters());
        
        BigDecimal basePrice = cost.multiply(BigDecimal.ONE.add(profitMargin))
                                 .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal grossProfitMargin = basePrice.subtract(cost)
                .divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(basePrice);
        result.setFinalPrice(basePrice);
        result.setCostPrice(cost);
        result.setGrossProfitMargin(grossProfitMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setCalculationExplanation(String.format(
            "成本: %s, 利润率: %.2f%%, 基础价格: %s",
            cost.toPlainString(),
            profitMargin.multiply(BigDecimal.valueOf(100)).doubleValue(),
            basePrice.toPlainString()
        ));
        result.setSuccess(true);
        
        return result;
    }
    
    @Override
    public int calculatePriorityScore(PriceCalculationRequest request) {
        if (!isApplicable(request)) {
            return 0;
        }
        
        int score = 50;
        
        String customerLevel = request.getCustomerLevel();
        if (customerLevel != null) {
            switch (customerLevel.toUpperCase()) {
                case "STANDARD":
                case "BASIC":
                    score += 20;
                    break;
                case "GOLD":
                case "SILVER":
                    score += 10;
                    break;
                case "VIP":
                case "PREMIUM":
                    score += 5;
                    break;
            }
        }
        
        BigDecimal cost = request.getCostPrice();
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            score += 25;
        }
        
        return score;
    }
    
    @Override
    public String getDescription() {
        return "成本加成定价算法：价格 = 成本 × (1 + 利润率)。适用于标准产品，基于产品成本加上固定利润率计算价格。";
    }
    
    @Override
    public String getDefaultParameters() {
        return "{\"profitMargin\":0.2,\"minProfitMargin\":0.1,\"maxProfitMargin\":0.5}";
    }
    
    private BigDecimal extractProfitMargin(Map<String, Object> parameters) {
        if (parameters == null) {
            return new BigDecimal("0.20");
        }
        
        try {
            Object value = parameters.get("profitMargin");
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
        
        return new BigDecimal("0.20");
    }
    
    public BigDecimal getMinProfitMargin(Map<String, Object> parameters) {
        return extractParameter(parameters, "minProfitMargin", new BigDecimal("0.10"));
    }
    
    public BigDecimal getMaxProfitMargin(Map<String, Object> parameters) {
        return extractParameter(parameters, "maxProfitMargin", new BigDecimal("0.50"));
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
    
    public boolean validateProfitMargin(BigDecimal profitMargin, Map<String, Object> parameters) {
        if (profitMargin == null) {
            return false;
        }
        
        BigDecimal minMargin = getMinProfitMargin(parameters);
        BigDecimal maxMargin = getMaxProfitMargin(parameters);
        
        return profitMargin.compareTo(minMargin) >= 0 && 
               profitMargin.compareTo(maxMargin) <= 0;
    }
}