package cn.aiedge.erp.price.engine.strategy;

import cn.aiedge.erp.price.engine.strategy.calculator.VolumeDiscountCalculator;
import cn.aiedge.erp.price.engine.strategy.calculator.BundleDiscountCalculator;
import cn.aiedge.erp.price.engine.strategy.calculator.StandardDiscountCalculator;
import cn.aiedge.erp.price.engine.strategy.calculator.PromotionalDiscountCalculator;
import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 折扣引擎
 * 负责执行折扣规则计算，管理折扣策略，并计算最终折扣后价格
 */
public class DiscountEngine {
    
    private final Map<DiscountRule.DiscountType, DiscountStrategy> discountStrategies;
    
    public DiscountEngine() {
        this.discountStrategies = new EnumMap<>(DiscountRule.DiscountType.class);
        initializeDefaultStrategies();
    }
    
    /**
     * 注册折扣策略
     */
    public void registerDiscountStrategy(DiscountRule.DiscountType discountType, DiscountStrategy strategy) {
        discountStrategies.put(discountType, strategy);
    }
    
    /**
     * 获取折扣策略
     */
    public DiscountStrategy getDiscountStrategy(DiscountRule.DiscountType discountType) {
        return discountStrategies.get(discountType);
    }
    
    /**
     * 计算所有适用的折扣
     */
    public List<DiscountRule> findApplicableDiscountRules(
            List<DiscountRule> allRules,
            PriceCalculationRequest request,
            BigDecimal basePrice) {
        
        List<DiscountRule> applicableRules = new ArrayList<>();
        
        for (DiscountRule rule : allRules) {
            // 检查规则是否有效
            if (!rule.isValid() || !rule.isActive()) {
                continue;
            }
            
            // 检查规则是否适用于当前请求
            if (!rule.isApplicable(request)) {
                continue;
            }
            
            // 检查规则是否在有效期内
            if (!rule.isEffective(request.getTimestamp())) {
                continue;
            }
            
            // 检查折扣策略是否存在并验证参数
            DiscountStrategy strategy = discountStrategies.get(rule.getDiscountType());
            if (strategy == null || !strategy.validateRuleParameters(rule)) {
                continue;
            }
            
            // 检查适用条件（产品、客户、区域、渠道等）
            if (!rule.checkApplicability(request)) {
                continue;
            }
            
            // 检查数量阈值
            if (!rule.checkQuantityThreshold(request.getQuantity())) {
                continue;
            }
            
            // 检查价格阈值（如果有）
            if (!rule.checkPriceThreshold(basePrice)) {
                continue;
            }
            
            applicableRules.add(rule);
        }
        
        return applicableRules;
    }
    
    /**
     * 计算单一产品的折扣金额
     */
    public BigDecimal calculateTotalDiscount(
            List<DiscountRule> applicableRules,
            PriceCalculationRequest request,
            BigDecimal basePrice,
            PriceCalculationResult result) {
        
        if (applicableRules.isEmpty() || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 按优先级排序（优先级分数高的在前）
        applicableRules.sort((rule1, rule2) -> {
            DiscountStrategy strategy1 = discountStrategies.get(rule1.getDiscountType());
            DiscountStrategy strategy2 = discountStrategies.get(rule2.getDiscountType());
            int score1 = strategy1 != null ? strategy1.calculatePriorityScore(rule1, request) : 0;
            int score2 = strategy2 != null ? strategy2.calculatePriorityScore(rule2, request) : 0;
            return Integer.compare(score2, score1); // 降序排序
        });
        
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal remainingPrice = basePrice;
        
        // 应用折扣规则（顺序应用）
        for (DiscountRule rule : applicableRules) {
            DiscountStrategy strategy = discountStrategies.get(rule.getDiscountType());
            if (strategy == null) {
                continue;
            }
            
            // 检查折扣规则是否允许组合应用
            if (rule.isExclusive() && totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                continue; // 互斥折扣，已经有其他折扣应用，跳过
            }
            
            // 计算当前规则的折扣金额
            BigDecimal ruleDiscount = strategy.calculateDiscount(rule, request, remainingPrice);
            
            // 确保折扣不超过剩余价格
            if (ruleDiscount.compareTo(remainingPrice) > 0) {
                ruleDiscount = remainingPrice;
            }
            
            // 确保总折扣不超过基础价格
            if (totalDiscount.add(ruleDiscount).compareTo(basePrice) > 0) {
                ruleDiscount = basePrice.subtract(totalDiscount);
            }
            
            if (ruleDiscount.compareTo(BigDecimal.ZERO) > 0) {
                totalDiscount = totalDiscount.add(ruleDiscount);
                remainingPrice = remainingPrice.subtract(ruleDiscount);
                
                // 记录折扣明细
                if (result != null) {
                    result.addDiscountDetail(
                        rule.getRuleId(),
                        rule.getDiscountType(),
                        ruleDiscount,
                        strategy.getDescription(rule)
                    );
                }
                
                // 如果折扣类型是互斥的，则不再应用其他折扣
                if (rule.isExclusive()) {
                    break;
                }
            }
            
            // 如果已没有剩余价格可折扣，则停止
            if (remainingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 批量计算折扣（用于批量订单或组合折扣）
     */
    public Map<String, BigDecimal> calculateBulkDiscounts(
            List<DiscountRule> applicableRules,
            List<PriceCalculationRequest> requests,
            List<BigDecimal> basePrices) {
        
        Map<String, BigDecimal> discountMap = new LinkedHashMap<>();
        
        if (applicableRules.isEmpty() || requests.size() != basePrices.size()) {
            return discountMap;
        }
        
        // 按优先级排序
        applicableRules.sort((rule1, rule2) -> {
            // 简化排序：按规则优先级分数排序（可以使用更复杂的逻辑）
            return Integer.compare(rule2.getPriority(), rule1.getPriority());
        });
        
        // 初始折扣为0
        for (int i = 0; i < requests.size(); i++) {
            discountMap.put(requests.get(i).getRequestId(), BigDecimal.ZERO);
        }
        
        BigDecimal totalBasePrice = basePrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 应用批量折扣规则
        for (DiscountRule rule : applicableRules) {
            DiscountStrategy strategy = discountStrategies.get(rule.getDiscountType());
            if (strategy == null) {
                continue;
            }
            
            // 检查规则是否支持批量计算
            List<BigDecimal> ruleDiscounts = strategy.calculateBulkDiscount(rule, requests, basePrices);
            
            if (ruleDiscounts != null && ruleDiscounts.size() == requests.size()) {
                // 累加折扣
                for (int i = 0; i < requests.size(); i++) {
                    BigDecimal currentDiscount = discountMap.get(requests.get(i).getRequestId());
                    BigDecimal newDiscount = ruleDiscounts.get(i);
                    
                    if (newDiscount != null && newDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        discountMap.put(requests.get(i).getRequestId(), 
                            currentDiscount.add(newDiscount));
                    }
                }
            }
            
            // 检查是否达到最大折扣限制
            BigDecimal currentTotalDiscount = discountMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (rule.getMaxDiscountAmount() != null && 
                currentTotalDiscount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                // 调整折扣使其不超过最大折扣金额
                BigDecimal adjustmentFactor = rule.getMaxDiscountAmount()
                        .divide(currentTotalDiscount, 4, RoundingMode.HALF_UP);
                
                for (String requestId : discountMap.keySet()) {
                    BigDecimal discount = discountMap.get(requestId);
                    discountMap.put(requestId, discount.multiply(adjustmentFactor)
                            .setScale(2, RoundingMode.HALF_UP));
                }
                break;
            }
        }
        
        return discountMap;
    }
    
    /**
     * 计算折扣率
     */
    public BigDecimal calculateDiscountRate(BigDecimal basePrice, BigDecimal discountAmount) {
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return discountAmount.divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算最终价格（基础价格 - 折扣）
     */
    public BigDecimal calculateFinalPrice(BigDecimal basePrice, BigDecimal discountAmount) {
        BigDecimal finalPrice = basePrice.subtract(discountAmount);
        return finalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : 
               finalPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算折扣毛利率
     */
    public BigDecimal calculateDiscountGrossMargin(
            BigDecimal costPrice, 
            BigDecimal finalPrice, 
            BigDecimal discountAmount) {
        
        if (costPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal revenue = finalPrice;
        BigDecimal grossProfit = revenue.subtract(costPrice);
        
        return grossProfit.divide(costPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算折扣对利润率的影响
     */
    public BigDecimal calculateDiscountImpactOnMargin(
            BigDecimal originalMarginRate,
            BigDecimal discountRate) {
        
        if (originalMarginRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal impact = originalMarginRate.subtract(discountRate);
        return impact.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : 
               impact.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 检查折扣规则是否冲突
     */
    public List<String> checkDiscountConflicts(List<DiscountRule> rules, PriceCalculationRequest request) {
        List<String> conflicts = new ArrayList<>();
        
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                DiscountRule rule1 = rules.get(i);
                DiscountRule rule2 = rules.get(j);
                
                // 检查互斥规则
                if (rule1.isExclusive() && rule2.isExclusive()) {
                    conflicts.add(String.format("折扣规则 %s 和 %s 均为互斥折扣，无法同时应用",
                            rule1.getRuleName(), rule2.getRuleName()));
                }
                
                // 检查相同类型的折扣规则（某些情况下不允许重复应用）
                if (rule1.getDiscountType() == rule2.getDiscountType() && 
                    rule1.isSameProductCategory(request.getProductId()) && 
                    rule2.isSameProductCategory(request.getProductId())) {
                    conflicts.add(String.format("相同折扣类型 %s 的规则 %s 和 %s 可能冲突",
                            rule1.getDiscountType(), rule1.getRuleName(), rule2.getRuleName()));
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * 初始化默认折扣策略
     */
    private void initializeDefaultStrategies() {
        registerDiscountStrategy(DiscountRule.DiscountType.STANDARD, new StandardDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.PROMOTIONAL, new PromotionalDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.VOLUME, new VolumeDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.BUNDLE, new BundleDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.SEASONAL, new StandardDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.CLEARANCE, new StandardDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.LOYALTY, new StandardDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.FIRST_PURCHASE, new StandardDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.BIRTHDAY, new StandardDiscountCalculator());
        registerDiscountStrategy(DiscountRule.DiscountType.SPECIAL_EVENT, new StandardDiscountCalculator());
    }
}