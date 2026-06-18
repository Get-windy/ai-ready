package cn.aiedge.erp.sales.pricing.service.algorithm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量采购折扣算法接口
 * 基于采购数量的阶梯式折扣计算
 */
public interface IVolumeDiscountAlgorithm {
    
    /**
     * 计算批量采购折扣价格
     * 
     * @param basePrice 基础价格
     * @param quantity 采购数量
     * @param discountTiers 折扣阶梯配置
     * @return 计算后的价格
     */
    BigDecimal calculateVolumeDiscountPrice(BigDecimal basePrice, int quantity, List<DiscountTier> discountTiers);
    
    /**
     * 获取适用的折扣阶梯
     * 
     * @param quantity 采购数量
     * @param discountTiers 折扣阶梯配置
     * @return 适用的折扣阶梯
     */
    DiscountTier getApplicableTier(int quantity, List<DiscountTier> discountTiers);
    
    /**
     * 计算批量采购折扣率
     * 
     * @param quantity 采购数量
     * @param discountTiers 折扣阶梯配置
     * @return 折扣率（例如：0.1 表示10%折扣）
     */
    BigDecimal calculateDiscountRate(int quantity, List<DiscountTier> discountTiers);
    
    /**
     * 计算批量采购总价
     * 
     * @param basePrice 基础价格
     * @param quantity 采购数量
     * @param discountTiers 折扣阶梯配置
     * @return 总价
     */
    BigDecimal calculateTotalPrice(BigDecimal basePrice, int quantity, List<DiscountTier> discountTiers);
    
    /**
     * 获取最优采购数量建议
     * 
     * @param basePrice 基础价格
     * @param targetQuantity 目标数量
     * @param discountTiers 折扣阶梯配置
     * @return 最优采购建议
     */
    OptimalPurchaseSuggestion getOptimalPurchaseSuggestion(BigDecimal basePrice, int targetQuantity, 
                                                          List<DiscountTier> discountTiers);
    
    /**
     * 验证折扣阶梯配置
     * 
     * @param discountTiers 折扣阶梯配置
     * @return 验证结果
     */
    DiscountTierValidationResult validateDiscountTiers(List<DiscountTier> discountTiers);
    
    /**
     * 获取默认折扣阶梯配置
     * 
     * @param productCategory 产品类别
     * @return 默认折扣阶梯
     */
    List<DiscountTier> getDefaultDiscountTiers(String productCategory);
    
    /**
     * 折扣阶梯
     */
    class DiscountTier {
        private final int minQuantity;       // 最小数量（包含）
        private final int maxQuantity;       // 最大数量（包含，0表示无上限）
        private final BigDecimal discountRate; // 折扣率（0-1）
        private final String tierName;       // 阶梯名称
        private final String description;    // 阶梯描述
        
        public DiscountTier(int minQuantity, int maxQuantity, BigDecimal discountRate, 
                           String tierName, String description) {
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
            this.discountRate = discountRate;
            this.tierName = tierName;
            this.description = description;
        }
        
        // Getter方法
        public int getMinQuantity() { return minQuantity; }
        public int getMaxQuantity() { return maxQuantity; }
        public BigDecimal getDiscountRate() { return discountRate; }
        public String getTierName() { return tierName; }
        public String getDescription() { return description; }
        
        /**
         * 检查数量是否在此阶梯内
         */
        public boolean isInTier(int quantity) {
            return quantity >= minQuantity && (maxQuantity == 0 || quantity <= maxQuantity);
        }
        
        /**
         * 获取折扣后的单价
         */
        public BigDecimal getDiscountedUnitPrice(BigDecimal basePrice) {
            return basePrice.multiply(BigDecimal.ONE.subtract(discountRate));
        }
        
        /**
         * 获取折扣后的总价
         */
        public BigDecimal getDiscountedTotalPrice(BigDecimal basePrice, int quantity) {
            return getDiscountedUnitPrice(basePrice).multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    /**
     * 最优采购建议
     */
    class OptimalPurchaseSuggestion {
        private final int suggestedQuantity;      // 建议采购数量
        private final BigDecimal unitPrice;       // 建议单价
        private final BigDecimal totalPrice;      // 建议总价
        private final BigDecimal savings;         // 节省金额
        private final BigDecimal savingsRate;     // 节省比例
        private final DiscountTier appliedTier;   // 应用的折扣阶梯
        private final String suggestionReason;    // 建议理由
        
        public OptimalPurchaseSuggestion(int suggestedQuantity, BigDecimal unitPrice,
                                        BigDecimal totalPrice, BigDecimal savings,
                                        BigDecimal savingsRate, DiscountTier appliedTier,
                                        String suggestionReason) {
            this.suggestedQuantity = suggestedQuantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
            this.savings = savings;
            this.savingsRate = savingsRate;
            this.appliedTier = appliedTier;
            this.suggestionReason = suggestionReason;
        }
        
        // Getter方法
        public int getSuggestedQuantity() { return suggestedQuantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public BigDecimal getSavings() { return savings; }
        public BigDecimal getSavingsRate() { return savingsRate; }
        public DiscountTier getAppliedTier() { return appliedTier; }
        public String getSuggestionReason() { return suggestionReason; }
    }
    
    /**
     * 折扣阶梯验证结果
     */
    class DiscountTierValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> validationErrors;
        
        public DiscountTierValidationResult(boolean valid, String message, List<String> validationErrors) {
            this.valid = valid;
            this.message = message;
            this.validationErrors = validationErrors;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getValidationErrors() { return validationErrors; }
    }
    
    /**
     * 批量采购折扣配置
     */
    class VolumeDiscountConfig {
        private final String configId;
        private final String configName;
        private final List<DiscountTier> discountTiers;
        private final boolean cumulativeDiscount;  // 是否累计折扣
        private final BigDecimal maxDiscountRate;  // 最大折扣率
        private final String applicableProducts;   // 适用产品范围
        private final String applicableCustomers;  // 适用客户范围
        
        public VolumeDiscountConfig(String configId, String configName, 
                                   List<DiscountTier> discountTiers, boolean cumulativeDiscount,
                                   BigDecimal maxDiscountRate, String applicableProducts,
                                   String applicableCustomers) {
            this.configId = configId;
            this.configName = configName;
            this.discountTiers = discountTiers;
            this.cumulativeDiscount = cumulativeDiscount;
            this.maxDiscountRate = maxDiscountRate;
            this.applicableProducts = applicableProducts;
            this.applicableCustomers = applicableCustomers;
        }
        
        // Getter方法
        public String getConfigId() { return configId; }
        public String getConfigName() { return configName; }
        public List<DiscountTier> getDiscountTiers() { return discountTiers; }
        public boolean isCumulativeDiscount() { return cumulativeDiscount; }
        public BigDecimal getMaxDiscountRate() { return maxDiscountRate; }
        public String getApplicableProducts() { return applicableProducts; }
        public String getApplicableCustomers() { return applicableCustomers; }
    }
}