package cn.aiedge.erp.price.engine.strategy.calculator;

import cn.aiedge.erp.price.engine.strategy.DiscountStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class StandardDiscountCalculator implements DiscountStrategy {
    
    @Override
    public DiscountRule.DiscountType getDiscountType() {
        return DiscountRule.DiscountType.STANDARD;
    }
    
    @Override
    public boolean isApplicable(DiscountRule rule, PriceCalculationRequest request) {
        if (!validateRuleParameters(rule)) {
            return false;
        }
        return rule.checkApplicability(request);
    }
    
    @Override
    public BigDecimal calculateDiscount(DiscountRule rule, PriceCalculationRequest request, BigDecimal basePrice) {
        if (!validateRuleParameters(rule) || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        if (rule.getDiscountRate() != null && rule.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
            discount = basePrice.multiply(rule.getDiscountRate())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            
            if (rule.getMaxDiscountAmount() != null && 
                discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                discount = rule.getMaxDiscountAmount();
            }
        } else if (rule.getFixedDiscountAmount() != null && 
                   rule.getFixedDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = rule.getFixedDiscountAmount();
            
            if (discount.compareTo(basePrice) > 0) {
                discount = basePrice;
            }
        }
        
        if (rule.getMinDiscountAmount() != null && 
            discount.compareTo(rule.getMinDiscountAmount()) < 0) {
            discount = BigDecimal.ZERO;
        }
        
        return discount.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public List<BigDecimal> calculateBulkDiscount(DiscountRule rule, List<PriceCalculationRequest> requests, 
                                                 List<BigDecimal> basePrices) {
        List<BigDecimal> discounts = new ArrayList<>();
        
        if (!validateRuleParameters(rule) || requests.size() != basePrices.size()) {
            for (int i = 0; i < requests.size(); i++) {
                discounts.add(BigDecimal.ZERO);
            }
            return discounts;
        }
        
        for (int i = 0; i < requests.size(); i++) {
            BigDecimal discount = calculateDiscount(rule, requests.get(i), basePrices.get(i));
            discounts.add(discount);
        }
        
        return discounts;
    }
    
    @Override
    public int calculatePriorityScore(DiscountRule rule, PriceCalculationRequest request) {
        if (!isApplicable(rule, request)) {
            return 0;
        }
        
        int score = 100;
        
        if (rule.getDiscountRate() != null) {
            score += rule.getDiscountRate().intValue();
        }
        
        score += rule.getPriority() * 10;
        
        return score;
    }
    
    @Override
    public String getDescription(DiscountRule rule) {
        StringBuilder desc = new StringBuilder();
        desc.append("标准折扣：");
        
        if (rule.getDiscountRate() != null && rule.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
            desc.append(String.format("折扣率 %.1f%%", rule.getDiscountRate()));
        }
        
        if (rule.getFixedDiscountAmount() != null && rule.getFixedDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            desc.append(String.format("固定折扣 %.2f 元", rule.getFixedDiscountAmount()));
        }
        
        return desc.toString();
    }
    
    @Override
    public boolean validateRuleParameters(DiscountRule rule) {
        if (rule == null || rule.getDiscountType() != DiscountRule.DiscountType.STANDARD) {
            return false;
        }
        
        boolean hasDiscountRate = rule.getDiscountRate() != null && 
                                  rule.getDiscountRate().compareTo(BigDecimal.ZERO) > 0 &&
                                  rule.getDiscountRate().compareTo(new BigDecimal("100")) <= 0;
        
        boolean hasFixedAmount = rule.getFixedDiscountAmount() != null && 
                                 rule.getFixedDiscountAmount().compareTo(BigDecimal.ZERO) > 0;
        
        return hasDiscountRate || hasFixedAmount;
    }
}