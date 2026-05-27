package cn.aiedge.erp.price.engine.strategy.calculator;

import cn.aiedge.erp.price.engine.strategy.DiscountStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BundleDiscountCalculator implements DiscountStrategy {
    
    @Override
    public DiscountRule.DiscountType getDiscountType() {
        return DiscountRule.DiscountType.BUNDLE;
    }
    
    @Override
    public boolean isApplicable(DiscountRule rule, PriceCalculationRequest request) {
        if (rule == null || request == null) {
            return false;
        }
        if (rule.getDiscountType() != DiscountRule.DiscountType.BUNDLE) {
            return false;
        }
        List<String> bundleProductIds = rule.getBundleProductIds();
        if (bundleProductIds == null || bundleProductIds.isEmpty()) {
            return false;
        }
        String productId = request.getProductId();
        if (productId == null) {
            return false;
        }
        return bundleProductIds.contains(productId);
    }
    
    @Override
    public boolean validateRuleParameters(DiscountRule rule) {
        if (rule.getDiscountValue() == null || rule.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (rule.getBundleProductIds() == null || rule.getBundleProductIds().isEmpty()) {
            return false;
        }
        return true;
    }
    
    @Override
    public BigDecimal calculateDiscount(DiscountRule rule, PriceCalculationRequest request, BigDecimal currentPrice) {
        if (!validateRuleParameters(rule)) {
            return BigDecimal.ZERO;
        }
        
        List<String> bundleProductIds = rule.getBundleProductIds();
        String productId = request.getProductId();
        
        if (productId == null || !bundleProductIds.contains(productId)) {
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
        
        if (rule.getBundleProductIds() != null && request.getProductId() != null) {
            if (rule.getBundleProductIds().contains(request.getProductId())) {
                baseScore += 30;
            }
        }
        
        return baseScore;
    }
    
    @Override
    public String getDescription(DiscountRule rule) {
        return String.format("组合折扣: 组合商品折扣%.2f%%", rule.getDiscountValue());
    }
    
    @Override
    public List<BigDecimal> calculateBulkDiscount(DiscountRule rule, List<PriceCalculationRequest> requests, List<BigDecimal> basePrices) {
        if (requests.size() != basePrices.size()) {
            return null;
        }
        
        List<BigDecimal> discounts = new ArrayList<>();
        List<String> bundleProductIds = rule.getBundleProductIds();
        
        if (bundleProductIds == null || bundleProductIds.isEmpty()) {
            for (int i = 0; i < requests.size(); i++) {
                discounts.add(BigDecimal.ZERO);
            }
            return discounts;
        }
        
        int bundleCount = 0;
        BigDecimal bundleTotalPrice = BigDecimal.ZERO;
        List<Integer> bundleIndices = new ArrayList<>();
        
        for (int i = 0; i < requests.size(); i++) {
            String productId = requests.get(i).getProductId();
            if (productId != null && bundleProductIds.contains(productId)) {
                bundleCount++;
                bundleTotalPrice = bundleTotalPrice.add(basePrices.get(i));
                bundleIndices.add(i);
            }
        }
        
        boolean allBundleProductsPresent = bundleProductIds.stream()
                .allMatch(id -> requests.stream().anyMatch(r -> id.equals(r.getProductId())));
        
        if (!allBundleProductsPresent) {
            for (int i = 0; i < requests.size(); i++) {
                discounts.add(BigDecimal.ZERO);
            }
            return discounts;
        }
        
        BigDecimal totalDiscount = bundleTotalPrice.multiply(rule.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        for (int i = 0; i < requests.size(); i++) {
            if (bundleIndices.contains(i)) {
                BigDecimal proportion = basePrices.get(i).divide(bundleTotalPrice, 4, RoundingMode.HALF_UP);
                BigDecimal individualDiscount = totalDiscount.multiply(proportion)
                        .setScale(2, RoundingMode.HALF_UP);
                discounts.add(individualDiscount);
            } else {
                discounts.add(BigDecimal.ZERO);
            }
        }
        
        return discounts;
    }
}