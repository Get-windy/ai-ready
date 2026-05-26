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

/**
 * 价值导向定价算法
 * 基于产品为客户创造的价值定价，适用于高价值、差异化产品
 */
@Component
public class ValueBasedPricingStrategy implements PricingAlgorithm {
    
    private static final String DEFAULT_PARAMETERS = "{\"valueMultiplier\":2.5,\"minValueMultiplier\":1.5,\"maxValueMultiplier\":5.0,\"customerValueWeight\":0.4,\"productDifferentiationWeight\":0.3,\"brandValueWeight\":0.3}";
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.VALUE_ORIENTED;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        // 价值导向定价适用于高价值、差异化产品
        boolean isPremiumProduct = request.getProductType() != null && 
                                  (request.getProductType().toUpperCase().contains("PREMIUM") ||
                                   request.getProductType().toUpperCase().contains("LUXURY") ||
                                   request.getProductType().toUpperCase().contains("HIGH_END"));
        
        boolean isVipCustomer = request.getCustomerGrade() != null &&
                               (request.getCustomerGrade().toUpperCase().contains("VIP") ||
                                request.getCustomerGrade().toUpperCase().contains("PREMIUM"));
        
        boolean hasValueFactors = request.getMarketFactors() != null &&
                                 (request.getMarketFactors().containsKey("productDifferentiation") ||
                                  request.getMarketFactors().containsKey("brandValue"));
        
        return isPremiumProduct || isVipCustomer || hasValueFactors;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // 验证策略类型
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.VALUE_ORIENTED) {
            throw new IllegalArgumentException("策略类型不匹配，期望: VALUE_ORIENTED, 实际: " + strategy.getStrategyType());
        }
        
        // 计算价值基准价格
        BigDecimal valueBasePrice = calculateValueBasePrice(request);
        
        // 计算价值乘数
        BigDecimal valueMultiplier = calculateValueMultiplier(request, strategy.getParameters());
        
        // 计算最终价格
        BigDecimal finalPrice = valueBasePrice.multiply(valueMultiplier)
                                             .setScale(2, RoundingMode.HALF_UP);
        
        // 计算毛利润（如果有成本数据）
        BigDecimal cost = request.getProductCost();
        BigDecimal grossProfit = null;
        BigDecimal grossMargin = null;
        
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            grossProfit = finalPrice.subtract(cost);
            grossMargin = grossProfit.divide(finalPrice, 4, RoundingMode.HALF_UP);
        }
        
        // 创建计算结果
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(valueBasePrice);
        result.setFinalPrice(finalPrice);
        result.setGrossProfit(grossProfit);
        result.setGrossMargin(grossMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setStrategyApplied("价值导向定价算法");
        result.setStrategyType(PricingStrategy.StrategyType.VALUE_ORIENTED.name());
        result.setCalculationNotes(String.format(
            "价值基准价: %s, 价值乘数: %.2f, 最终价格: %s",
            valueBasePrice.toPlainString(),
            valueMultiplier.doubleValue(),
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
        
        int score = 60; // 基础分数较高，因为价值导向定价优先级高
        
        // 根据产品类型调整分数
        if (request.getProductType() != null) {
            String productType = request.getProductType().toUpperCase();
            if (productType.contains("LUXURY") || productType.contains("PREMIUM")) {
                score += 40;
            } else if (productType.contains("HIGH_END") || productType.contains("DIFFERENTIATED")) {
                score += 30;
            } else if (productType.contains("BRANDED")) {
                score += 20;
            }
        }
        
        // 根据客户等级调整分数
        if (request.getCustomerGrade() != null) {
            switch (request.getCustomerGrade().toUpperCase()) {
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
        
        // 根据市场因素调整分数
        if (request.getMarketFactors() != null) {
            Map<String, String> factors = request.getMarketFactors();
            if (factors.containsKey("productDifferentiation") && 
                "HIGH".equalsIgnoreCase(factors.get("productDifferentiation"))) {
                score += 30;
            }
            if (factors.containsKey("brandValue") && 
                "HIGH".equalsIgnoreCase(factors.get("brandValue"))) {
                score += 25;
            }
            if (factors.containsKey("customerLifetimeValue") && 
                "HIGH".equalsIgnoreCase(factors.get("customerLifetimeValue"))) {
                score += 20;
            }
        }
        
        return score;
    }
    
    @Override
    public String getDescription() {
        return "价值导向定价算法：基于产品为客户创造的价值定价。适用于高价值、差异化、品牌产品，考虑客户感知价值、产品差异化程度、品牌价值等因素。";
    }
    
    @Override
    public String getDefaultParameters() {
        return DEFAULT_PARAMETERS;
    }
    
    /**
     * 计算价值基准价格
     */
    private BigDecimal calculateValueBasePrice(PriceCalculationRequest request) {
        BigDecimal valueBasePrice = null;
        
        // 1. 首先使用产品标准价（如果适用于价值导向）
        if (request.getProductStandardPrice() != null && isPremiumProduct(request)) {
            valueBasePrice = request.getProductStandardPrice();
        }
        
        // 2. 如果没有标准价，使用市场高端价格
        if (valueBasePrice == null && request.getMarketAveragePrice() != null) {
            // 价值导向产品通常比市场平均价高
            valueBasePrice = request.getMarketAveragePrice().multiply(new BigDecimal("1.5"))
                                   .setScale(2, RoundingMode.HALF_UP);
        }
        
        // 3. 如果还没有，基于成本计算（高利润率）
        if (valueBasePrice == null && request.getProductCost() != null) {
            // 价值导向产品通常有高利润率
            valueBasePrice = request.getProductCost().multiply(new BigDecimal("3.0"))
                                   .setScale(2, RoundingMode.HALF_UP);
        }
        
        if (valueBasePrice == null) {
            throw new IllegalArgumentException("无法计算价值基准价格，缺少必要的产品价值数据");
        }
        
        return valueBasePrice;
    }
    
    /**
     * 计算价值乘数
     */
    private BigDecimal calculateValueMultiplier(PriceCalculationRequest request, String parameters) {
        BigDecimal baseMultiplier = extractValueMultiplier(parameters);
        
        // 应用价值因素调整
        BigDecimal adjustedMultiplier = baseMultiplier;
        
        // 1. 客户价值影响
        BigDecimal customerValueFactor = calculateCustomerValueFactor(request);
        BigDecimal customerValueWeight = extractParameter(parameters, "customerValueWeight", new BigDecimal("0.4"));
        adjustedMultiplier = adjustedMultiplier.multiply(
            BigDecimal.ONE.add(customerValueFactor.subtract(BigDecimal.ONE).multiply(customerValueWeight))
        );
        
        // 2. 产品差异化影响
        BigDecimal differentiationFactor = calculateDifferentiationFactor(request);
        BigDecimal differentiationWeight = extractParameter(parameters, "productDifferentiationWeight", new BigDecimal("0.3"));
        adjustedMultiplier = adjustedMultiplier.multiply(
            BigDecimal.ONE.add(differentiationFactor.subtract(BigDecimal.ONE).multiply(differentiationWeight))
        );
        
        // 3. 品牌价值影响
        BigDecimal brandValueFactor = calculateBrandValueFactor(request);
        BigDecimal brandValueWeight = extractParameter(parameters, "brandValueWeight", new BigDecimal("0.3"));
        adjustedMultiplier = adjustedMultiplier.multiply(
            BigDecimal.ONE.add(brandValueFactor.subtract(BigDecimal.ONE).multiply(brandValueWeight))
        );
        
        // 确保乘数在有效范围内
        BigDecimal minMultiplier = extractParameter(parameters, "minValueMultiplier", new BigDecimal("1.5"));
        BigDecimal maxMultiplier = extractParameter(parameters, "maxValueMultiplier", new BigDecimal("5.0"));
        
        if (adjustedMultiplier.compareTo(minMultiplier) < 0) {
            adjustedMultiplier = minMultiplier;
        } else if (adjustedMultiplier.compareTo(maxMultiplier) > 0) {
            adjustedMultiplier = maxMultiplier;
        }
        
        return adjustedMultiplier.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算客户价值影响因子
     */
    private BigDecimal calculateCustomerValueFactor(PriceCalculationRequest request) {
        if (request.getCustomerGrade() == null) {
            return new BigDecimal("1.0");
        }
        
        switch (request.getCustomerGrade().toUpperCase()) {
            case "VIP":
            case "PREMIUM":
                return new BigDecimal("1.4"); // VIP客户愿意支付更高价格
            case "GOLD":
                return new BigDecimal("1.2");
            case "SILVER":
                return new BigDecimal("1.1");
            case "STANDARD":
                return new BigDecimal("1.0");
            case "BASIC":
                return new BigDecimal("0.9");
            default:
                return new BigDecimal("1.0");
        }
    }
    
    /**
     * 计算产品差异化影响因子
     */
    private BigDecimal calculateDifferentiationFactor(PriceCalculationRequest request) {
        if (request.getMarketFactors() == null || 
            !request.getMarketFactors().containsKey("productDifferentiation")) {
            // 根据产品类型判断
            if (isPremiumProduct(request)) {
                return new BigDecimal("1.3");
            }
            return new BigDecimal("1.0");
        }
        
        String differentiation = request.getMarketFactors().get("productDifferentiation");
        if (differentiation == null) {
            return new BigDecimal("1.0");
        }
        
        switch (differentiation.toUpperCase()) {
            case "VERY_HIGH":
                return new BigDecimal("1.5");
            case "HIGH":
                return new BigDecimal("1.3");
            case "MEDIUM":
                return new BigDecimal("1.1");
            case "LOW":
                return new BigDecimal("0.9");
            case "VERY_LOW":
                return new BigDecimal("0.7");
            default:
                return new BigDecimal("1.0");
        }
    }
    
    /**
     * 计算品牌价值影响因子
     */
    private BigDecimal calculateBrandValueFactor(PriceCalculationRequest request) {
        if (request.getMarketFactors() == null || 
            !request.getMarketFactors().containsKey("brandValue")) {
            // 根据产品类型判断
            if (request.getProductType() != null && 
                request.getProductType().toUpperCase().contains("BRANDED")) {
                return new BigDecimal("1.2");
            }
            return new BigDecimal("1.0");
        }
        
        String brandValue = request.getMarketFactors().get("brandValue");
        if (brandValue == null) {
            return new BigDecimal("1.0");
        }
        
        switch (brandValue.toUpperCase()) {
            case "PREMIUM":
            case "LUXURY":
                return new BigDecimal("1.4");
            case "WELL_KNOWN":
                return new BigDecimal("1.2");
            case "AVERAGE":
                return new BigDecimal("1.0");
            case "UNKNOWN":
                return new BigDecimal("0.8");
            default:
                return new BigDecimal("1.0");
        }
    }
    
    /**
     * 检查是否为高端产品
     */
    private boolean isPremiumProduct(PriceCalculationRequest request) {
        if (request.getProductType() == null) {
            return false;
        }
        
        String productType = request.getProductType().toUpperCase();
        return productType.contains("LUXURY") ||
               productType.contains("PREMIUM") ||
               productType.contains("HIGH_END") ||
               productType.contains("DELUXE");
    }
    
    /**
     * 提取价值乘数
     */
    private BigDecimal extractValueMultiplier(String parameters) {
        return extractParameter(parameters, "valueMultiplier", new BigDecimal("2.5"));
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
     * 获取最小价值乘数
     */
    public BigDecimal getMinValueMultiplier(String parameters) {
        return extractParameter(parameters, "minValueMultiplier", new BigDecimal("1.5"));
    }
    
    /**
     * 获取最大价值乘数
     */
    public BigDecimal getMaxValueMultiplier(String parameters) {
        return extractParameter(parameters, "maxValueMultiplier", new BigDecimal("5.0"));
    }
}