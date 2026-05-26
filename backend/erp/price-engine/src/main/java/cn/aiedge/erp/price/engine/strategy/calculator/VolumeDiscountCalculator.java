package cn.aiedge.erp.price.engine.strategy.calculator;

import cn.aiedge.erp.price.engine.strategy.DiscountStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class VolumeDiscountCalculator implements DiscountStrategy {
    
    @Override
    public boolean validateRuleParameters(DiscountRule rule) {
        if (rule.getDiscountValue() == null || rule.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (rule.getMinQuantity() == null || rule.getMinQuantity() <= 0) {
            return false;
        }
        return true;
    }
    
    @Override
    public BigDecimal calculateDiscount(DiscountRule rule, PriceCalculationRequest request, BigDecimal currentPrice) {
        if (!validateRuleParameters(rule)) {
            return BigDecimal.ZERO;
        }
        
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity < rule.getMinQuantity()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discountRate = rule.getDiscountValue();
        BigDecimal discountAmount = currentPrice.multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        if (rule.getMaxDiscountAmount() != null && discountAmount.compareTo(rule.getMaxDiscountAmount()) > 0) {
            discountAmount = rule.getMaxDiscountAmount();
        }
        
        return discountAmount;
    }
    
    @Override
    public int calculatePriorityScore(DiscountRule rule, PriceCalculationRequest request) {
        int baseScore = rule.getPriority() * 10;
        
        Integer quantity = request.getQuantity();
        if (quantity != null && rule.getMinQuantity() != null) {
            int quantityBonus = (quantity / rule.getMinQuantity()) * 5;
            baseScore += Math.min(quantityBonus, 50);
        }
        
        return baseScore;
    }
    
    @Override
    public String getDescription(DiscountRule rule) {
        return String.format("数量折扣: 购买%d件以上, 折扣%.2f%%", 
                rule.getMinQuantity(), rule.getDiscountValue());
    }
    
    @Override
    public List<BigDecimal> calculateBulkDiscount(DiscountRule rule, List<PriceCalculationRequest> requests, List<BigDecimal> basePrices) {
        if (requests.size() != basePrices.size()) {
            return null;
        }
        
        List<BigDecimal> discounts = new ArrayList<>();
        int totalQuantity = requests.stream()
                .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 1)
                .sum();
        
        if (totalQuantity < rule.getMinQuantity()) {
            for (int i = 0; i < requests.size(); i++) {
                discounts.add(BigDecimal.ZERO);
            }
            return discounts;
        }
        
        BigDecimal totalBasePrice = basePrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDiscount = totalBasePrice.multiply(rule.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        for (int i = 0; i < requests.size(); i++) {
            BigDecimal proportion = basePrices.get(i).divide(totalBasePrice, 4, RoundingMode.HALF_UP);
            BigDecimal individualDiscount = totalDiscount.multiply(proportion)
                    .setScale(2, RoundingMode.HALF_UP);
            discounts.add(individualDiscount);
        }
        
        return discounts;
    }
}