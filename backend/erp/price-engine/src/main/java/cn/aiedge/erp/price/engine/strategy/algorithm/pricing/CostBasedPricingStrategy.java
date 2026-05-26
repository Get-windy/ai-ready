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
 * 成本加成定价算法
 * 价格 = 成本 × (1 + 利润率)
 */
@Component
public class CostBasedPricingStrategy implements PricingAlgorithm {
    
    private static final String DEFAULT_PARAMETERS = "{\"profitMargin\":0.2,\"minProfitMargin\":0.1,\"maxProfitMargin\":0.5}";
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.COST_PLUS;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        // 成本加成定价适用于所有有成本数据的场景
        boolean hasCostData = request.getProductCost() != null && request.getProductCost().compareTo(BigDecimal.ZERO) > 0;
        boolean isStandardProduct = request.getProductType() == null || 
                                   !request.getProductType().toUpperCase().contains("LUXURY") &&
                                   !request.getProductType().toUpperCase().contains("PREMIUM");
        
        return hasCostData && isStandardProduct;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // 验证策略类型
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.COST_PLUS) {
            throw new IllegalArgumentException("策略类型不匹配，期望: COST_PLUS, 实际: " + strategy.getStrategyType());
        }
        
        // 获取成本
        BigDecimal cost = request.getProductCost();
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("产品成本不能为空或小于等于0");
        }
        
        // 解析利润率参数
        BigDecimal profitMargin = extractProfitMargin(strategy.getParameters());
        
        // 计算基础价格
        BigDecimal basePrice = cost.multiply(BigDecimal.ONE.add(profitMargin))
                                 .setScale(2, RoundingMode.HALF_UP);
        
        // 计算毛利润
        BigDecimal grossProfit = basePrice.subtract(cost);
        BigDecimal grossMargin = grossProfit.divide(basePrice, 4, RoundingMode.HALF_UP);
        
        // 创建计算结果
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(basePrice);
        result.setFinalPrice(basePrice);
        result.setGrossProfit(grossProfit);
        result.setGrossMargin(grossMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setStrategyApplied("成本加成定价算法");
        result.setStrategyType(PricingStrategy.StrategyType.COST_PLUS.name());
        result.setCalculationNotes(String.format(
            "成本: %s, 利润率: %.2f%%, 基础价格: %s",
            cost.toPlainString(),
            profitMargin.multiply(BigDecimal.valueOf(100)).doubleValue(),
            basePrice.toPlainString()
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
        
        int score = 50; // 基础分数
        
        // 根据客户等级调整分数
        if (request.getCustomerGrade() != null) {
            switch (request.getCustomerGrade().toUpperCase()) {
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
        
        // 根据产品类型调整分数
        if (request.getProductType() != null) {
            String productType = request.getProductType().toUpperCase();
            if (productType.contains("STANDARD") || productType.contains("BASIC")) {
                score += 15;
            } else if (productType.contains("COMMODITY")) {
                score += 10;
            }
        }
        
        // 如果有明确的成本数据，增加分数
        if (request.getProductCost() != null && request.getProductCost().compareTo(BigDecimal.ZERO) > 0) {
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
        return DEFAULT_PARAMETERS;
    }
    
    /**
     * 从参数字符串中提取利润率
     */
    private BigDecimal extractProfitMargin(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            // 默认利润率 20%
            return new BigDecimal("0.20");
        }
        
        try {
            // 简单解析JSON字符串，实际应用中应该使用JSON解析库
            if (parameters.contains("\"profitMargin\":")) {
                String[] parts = parameters.split("\"profitMargin\":");
                if (parts.length > 1) {
                    String valuePart = parts[1].split(",")[0].trim();
                    if (valuePart.endsWith("}")) {
                        valuePart = valuePart.substring(0, valuePart.length() - 1);
                    }
                    return new BigDecimal(valuePart);
                }
            }
        } catch (Exception e) {
            // 解析失败，使用默认值
        }
        
        return new BigDecimal("0.20");
    }
    
    /**
     * 获取最小利润率
     */
    public BigDecimal getMinProfitMargin(String parameters) {
        return extractParameter(parameters, "minProfitMargin", new BigDecimal("0.10"));
    }
    
    /**
     * 获取最大利润率
     */
    public BigDecimal getMaxProfitMargin(String parameters) {
        return extractParameter(parameters, "maxProfitMargin", new BigDecimal("0.50"));
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
    
    /**
     * 验证利润率是否在有效范围内
     */
    public boolean validateProfitMargin(BigDecimal profitMargin, String parameters) {
        if (profitMargin == null) {
            return false;
        }
        
        BigDecimal minMargin = getMinProfitMargin(parameters);
        BigDecimal maxMargin = getMaxProfitMargin(parameters);
        
        return profitMargin.compareTo(minMargin) >= 0 && 
               profitMargin.compareTo(maxMargin) <= 0;
    }
}