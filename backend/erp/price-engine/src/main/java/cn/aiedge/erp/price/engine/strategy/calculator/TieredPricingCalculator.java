package cn.aiedge.erp.price.engine.strategy.calculator;

import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PriceTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 层级定价计算器
 * 根据客户等级和购买数量选择最合适的价层，计算最终价格
 */
public class TieredPricingCalculator {

    /**
     * 根据请求选择最合适的价层并计算价格
     *
     * @param tiers   可用价层列表
     * @param request 价格计算请求
     * @return 计算结果（含所选价层及最终价格）
     */
    public TieredPriceResult calculateBestTierPrice(List<PriceTier> tiers, PriceCalculationRequest request) {
        if (tiers == null || tiers.isEmpty() || request == null) {
            return null;
        }

        // 过滤出适用的价层
        List<PriceTier> applicableTiers = filterApplicableTiers(tiers, request);
        if (applicableTiers.isEmpty()) {
            return null;
        }

        // 按优先级排序（数字越小优先级越高）
        applicableTiers.sort(Comparator.comparingInt(PriceTier::getPriority));

        // 选择优先级最高的价层
        PriceTier selectedTier = applicableTiers.get(0);

        // 计算价格
        BigDecimal basePrice = request.getEffectiveBasePrice();
        BigDecimal finalPrice = selectedTier.calculatePrice(basePrice);

        TieredPriceResult result = new TieredPriceResult();
        result.setSelectedTier(selectedTier);
        result.setOriginalBasePrice(basePrice);
        result.setTierPrice(finalPrice);
        result.setDiscountAmount(basePrice.subtract(finalPrice));
        result.setDiscountRate(basePrice.compareTo(BigDecimal.ZERO) > 0
                ? result.getDiscountAmount().multiply(BigDecimal.valueOf(100))
                .divide(basePrice, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.setAllApplicableTiers(applicableTiers);

        return result;
    }

    /**
     * 批量计算多个产品的层级价格
     */
    public List<TieredPriceResult> calculateBatchTierPrices(Map<String, List<PriceTier>> productTiersMap,
                                                             List<PriceCalculationRequest> requests) {
        if (productTiersMap == null || requests == null) {
            return Collections.emptyList();
        }

        return requests.stream()
                .map(req -> {
                    List<PriceTier> productTiers = productTiersMap.get(req.getProductId());
                    if (productTiers == null || productTiers.isEmpty()) {
                        return null;
                    }
                    return calculateBestTierPrice(productTiers, req);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定产品的所有可用价层
     */
    public List<PriceTier> getAvailableTiersForProduct(List<PriceTier> allTiers, String productId, String customerLevel) {
        if (allTiers == null || productId == null) {
            return Collections.emptyList();
        }

        return allTiers.stream()
                .filter(t -> "active".equals(t.getStatus()))
                .filter(t -> productId.equals(t.getProductId())
                        || (t.getCategoryId() != null && t.getCategoryId().equals(productId)))
                .filter(t -> customerLevel == null || t.isCustomerLevelApplicable(customerLevel))
                .sorted(Comparator.comparingInt(PriceTier::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 过滤适用于当前请求的价层
     */
    private List<PriceTier> filterApplicableTiers(List<PriceTier> tiers, PriceCalculationRequest request) {
        return tiers.stream()
                .filter(t -> "active".equals(t.getStatus()))
                .filter(t -> t.isQuantityInRange(request.getQuantity()))
                .filter(t -> t.isCustomerLevelApplicable(request.getCustomerLevel()))
                .filter(t -> {
                    // 产品匹配：相同productId 或 相同categoryId
                    if (t.getProductId() != null) {
                        return t.getProductId().equals(request.getProductId());
                    }
                    if (t.getCategoryId() != null) {
                        return t.getCategoryId().equals(request.getCategory());
                    }
                    return true; // 全局价层
                })
                .collect(Collectors.toList());
    }

    /**
     * 层级定价结果
     */
    public static class TieredPriceResult {
        private PriceTier selectedTier;
        private BigDecimal originalBasePrice;
        private BigDecimal tierPrice;
        private BigDecimal discountAmount;
        private BigDecimal discountRate;
        private List<PriceTier> allApplicableTiers;

        public PriceTier getSelectedTier() { return selectedTier; }
        public void setSelectedTier(PriceTier selectedTier) { this.selectedTier = selectedTier; }

        public BigDecimal getOriginalBasePrice() { return originalBasePrice; }
        public void setOriginalBasePrice(BigDecimal originalBasePrice) { this.originalBasePrice = originalBasePrice; }

        public BigDecimal getTierPrice() { return tierPrice; }
        public void setTierPrice(BigDecimal tierPrice) { this.tierPrice = tierPrice; }

        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

        public BigDecimal getDiscountRate() { return discountRate; }
        public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }

        public List<PriceTier> getAllApplicableTiers() { return allApplicableTiers; }
        public void setAllApplicableTiers(List<PriceTier> allApplicableTiers) { this.allApplicableTiers = allApplicableTiers; }

        /**
         * 获取层级名称
         */
        public String getTierName() {
            return selectedTier != null ? selectedTier.getTierName() : "";
        }

        /**
         * 获取层级编码
         */
        public String getTierCode() {
            return selectedTier != null ? selectedTier.getTierCode() : "";
        }
    }
}
