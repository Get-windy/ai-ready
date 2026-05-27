package cn.aiedge.erp.price.engine.strategy.factory;

import cn.aiedge.erp.price.engine.strategy.PricingAlgorithm;
import cn.aiedge.erp.price.engine.strategy.DiscountStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.algorithm.pricing.*;
import cn.aiedge.erp.price.engine.strategy.calculator.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定价策略工厂类
 * 采用工厂模式根据条件选择最佳定价策略和折扣策略
 */
@Component
public class PricingStrategyFactory {
    
    private final Map<PricingStrategy.StrategyType, PricingAlgorithm> pricingAlgorithms = new HashMap<>();
    private final Map<DiscountRule.DiscountType, DiscountStrategy> discountStrategies = new HashMap<>();
    
    /**
     * 构造函数 - 注册所有可用的定价算法和折扣策略
     */
    public PricingStrategyFactory() {
        registerPricingAlgorithms();
        registerDiscountStrategies();
    }
    
    /**
     * 注册定价算法
     */
    private void registerPricingAlgorithms() {
        pricingAlgorithms.put(PricingStrategy.StrategyType.COST_BASED, new CostBasedPricingStrategy());
        pricingAlgorithms.put(PricingStrategy.StrategyType.MARKET_BASED, new MarketBasedPricingStrategy());
        pricingAlgorithms.put(PricingStrategy.StrategyType.VALUE_BASED, new ValueBasedPricingStrategy());
        pricingAlgorithms.put(PricingStrategy.StrategyType.COMPETITIVE_BASED, new CompetitivePricingStrategy());
    }
    
    /**
     * 注册折扣策略
     */
    private void registerDiscountStrategies() {
        discountStrategies.put(DiscountRule.DiscountType.STANDARD, new StandardDiscountCalculator());
        discountStrategies.put(DiscountRule.DiscountType.PROMOTIONAL, new PromotionalDiscountCalculator());
        discountStrategies.put(DiscountRule.DiscountType.VOLUME, new VolumeDiscountCalculator());
        discountStrategies.put(DiscountRule.DiscountType.BUNDLE, new BundleDiscountCalculator());
    }
    
    /**
     * 根据计算请求选择最佳定价算法
     */
    public PricingAlgorithm selectBestPricingAlgorithm(PriceCalculationRequest request) {
        List<PricingAlgorithm> applicableAlgorithms = pricingAlgorithms.values().stream()
                .filter(algorithm -> algorithm.isApplicable(request))
                .sorted((a1, a2) -> {
                    int score1 = a1.calculatePriorityScore(request);
                    int score2 = a2.calculatePriorityScore(request);
                    return Integer.compare(score2, score1); // 降序排序
                })
                .collect(Collectors.toList());
        
        if (applicableAlgorithms.isEmpty()) {
            throw new IllegalArgumentException("没有适用于当前请求的定价算法: " + request);
        }
        
        return applicableAlgorithms.get(0); // 返回优先级最高的算法
    }
    
    /**
     * 根据定价策略类型获取定价算法
     */
    public PricingAlgorithm getPricingAlgorithm(PricingStrategy.StrategyType strategyType) {
        PricingAlgorithm algorithm = pricingAlgorithms.get(strategyType);
        if (algorithm == null) {
            throw new IllegalArgumentException("不支持的定价策略类型: " + strategyType);
        }
        return algorithm;
    }
    
    /**
     * 根据折扣规则选择折扣策略
     */
    public DiscountStrategy selectDiscountStrategy(DiscountRule discountRule) {
        DiscountStrategy strategy = discountStrategies.get(discountRule.getDiscountType());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的折扣类型: " + discountRule.getDiscountType());
        }
        return strategy;
    }
    
    /**
     * 获取所有适用的折扣策略
     */
    public List<DiscountStrategy> getApplicableDiscountStrategies(List<DiscountRule> rules, PriceCalculationRequest request) {
        return rules.stream()
                .filter(rule -> {
                    DiscountStrategy strategy = discountStrategies.get(rule.getDiscountType());
                    return strategy != null && strategy.isApplicable(rule, request);
                })
                .sorted((r1, r2) -> {
                    DiscountStrategy s1 = discountStrategies.get(r1.getDiscountType());
                    DiscountStrategy s2 = discountStrategies.get(r2.getDiscountType());
                    int score1 = s1.calculatePriorityScore(r1, request);
                    int score2 = s2.calculatePriorityScore(r2, request);
                    return Integer.compare(score2, score1); // 降序排序
                })
                .map(rule -> discountStrategies.get(rule.getDiscountType()))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据客户等级推荐定价策略类型
     */
    public PricingStrategy.StrategyType recommendStrategyTypeForCustomerGrade(String customerGrade) {
        if (customerGrade == null) {
            return PricingStrategy.StrategyType.COST_BASED;
        }
        
        switch (customerGrade.toUpperCase()) {
            case "VIP":
            case "PREMIUM":
                return PricingStrategy.StrategyType.VALUE_BASED;
            case "GOLD":
            case "SILVER":
                return PricingStrategy.StrategyType.MARKET_BASED;
            case "STANDARD":
            case "BASIC":
                return PricingStrategy.StrategyType.COMPETITIVE_BASED;
            default:
                return PricingStrategy.StrategyType.COST_BASED;
        }
    }
    
    /**
     * 根据产品类型推荐定价策略类型
     */
    public PricingStrategy.StrategyType recommendStrategyTypeForProductType(String productType) {
        if (productType == null) {
            return PricingStrategy.StrategyType.COST_BASED;
        }
        
        switch (productType.toUpperCase()) {
            case "LUXURY":
            case "PREMIUM":
                return PricingStrategy.StrategyType.VALUE_BASED;
            case "COMMODITY":
            case "STANDARD":
                return PricingStrategy.StrategyType.MARKET_BASED;
            case "PROMOTIONAL":
            case "DISCOUNTED":
                return PricingStrategy.StrategyType.COMPETITIVE_BASED;
            default:
                return PricingStrategy.StrategyType.COST_BASED;
        }
    }
    
    /**
     * 获取所有注册的定价算法类型
     */
    public Set<PricingStrategy.StrategyType> getAvailablePricingAlgorithmTypes() {
        return pricingAlgorithms.keySet();
    }
    
    /**
     * 获取所有注册的折扣策略类型
     */
    public Set<DiscountRule.DiscountType> getAvailableDiscountStrategyTypes() {
        return discountStrategies.keySet();
    }
    
    /**
     * 验证定价策略参数
     */
    public boolean validatePricingStrategyParameters(PricingStrategy.StrategyType strategyType, String parameters) {
        try {
            PricingAlgorithm algorithm = getPricingAlgorithm(strategyType);
            // 这里可以添加更复杂的参数验证逻辑
            return parameters != null && !parameters.trim().isEmpty();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 验证折扣规则参数
     */
    public boolean validateDiscountRuleParameters(DiscountRule discountRule) {
        DiscountStrategy strategy = discountStrategies.get(discountRule.getDiscountType());
        if (strategy == null) {
            return false;
        }
        return strategy.validateRuleParameters(discountRule);
    }
}