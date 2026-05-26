package cn.aiedge.erp.sales.pricing.service.algorithm;

import cn.aiedge.erp.sales.pricing.enums.CustomerLevel;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 客户分级定价算法接口
 * 根据客户等级和历史采购的差异化定价
 */
public interface ICustomerTieredPricingAlgorithm {
    
    /**
     * 根据客户等级计算价格
     * 
     * @param basePrice 基础价格
     * @param customerLevel 客户等级
     * @return 计算后的价格
     */
    BigDecimal calculatePriceByCustomerLevel(BigDecimal basePrice, CustomerLevel customerLevel);
    
    /**
     * 根据客户等级计算折扣率
     * 
     * @param customerLevel 客户等级
     * @return 折扣率（例如：0.1 表示10%折扣）
     */
    BigDecimal calculateDiscountByCustomerLevel(CustomerLevel customerLevel);
    
    /**
     * 根据客户历史采购金额计算价格
     * 
     * @param basePrice 基础价格
     * @param historicalPurchaseAmount 历史采购金额
     * @param purchaseCount 采购次数
     * @return 计算后的价格
     */
    BigDecimal calculatePriceByPurchaseHistory(BigDecimal basePrice, BigDecimal historicalPurchaseAmount, int purchaseCount);
    
    /**
     * 根据客户忠诚度计算价格
     * 
     * @param basePrice 基础价格
     * @param loyaltyScore 忠诚度分数（0-100）
     * @param customerAgeMonths 客户时长（月数）
     * @return 计算后的价格
     */
    BigDecimal calculatePriceByCustomerLoyalty(BigDecimal basePrice, int loyaltyScore, int customerAgeMonths);
    
    /**
     * 计算客户等级升级后的价格变化
     * 
     * @param oldPrice 原价格
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @return 新价格
     */
    BigDecimal calculatePriceForLevelUpgrade(BigDecimal oldPrice, CustomerLevel oldLevel, CustomerLevel newLevel);
    
    /**
     * 根据客户等级和历史行为计算综合价格
     * 
     * @param basePrice 基础价格
     * @param customerLevel 客户等级
     * @param historicalPurchaseAmount 历史采购金额
     * @param purchaseCount 采购次数
     * @param loyaltyScore 忠诚度分数
     * @param customerAgeMonths 客户时长
     * @return 综合计算后的价格
     */
    BigDecimal calculateComprehensiveCustomerPrice(BigDecimal basePrice, CustomerLevel customerLevel,
                                                   BigDecimal historicalPurchaseAmount, int purchaseCount,
                                                   int loyaltyScore, int customerAgeMonths);
    
    /**
     * 验证客户等级定价的合理性
     * 
     * @param price 计算出的价格
     * @param customerLevel 客户等级
     * @param basePrice 基础价格
     * @return 验证结果和调整建议
     */
    CustomerPriceValidationResult validateCustomerPrice(BigDecimal price, CustomerLevel customerLevel, BigDecimal basePrice);
    
    /**
     * 获取客户等级定价配置
     */
    Map<CustomerLevel, CustomerLevelPricingConfig> getCustomerLevelPricingConfigs();
    
    /**
     * 客户等级定价配置
     */
    class CustomerLevelPricingConfig {
        private final BigDecimal discountRate;
        private final BigDecimal minPriceFactor;
        private final BigDecimal maxPriceFactor;
        private final String pricingRule;
        
        public CustomerLevelPricingConfig(BigDecimal discountRate, BigDecimal minPriceFactor, 
                                         BigDecimal maxPriceFactor, String pricingRule) {
            this.discountRate = discountRate;
            this.minPriceFactor = minPriceFactor;
            this.maxPriceFactor = maxPriceFactor;
            this.pricingRule = pricingRule;
        }
        
        public BigDecimal getDiscountRate() {
            return discountRate;
        }
        
        public BigDecimal getMinPriceFactor() {
            return minPriceFactor;
        }
        
        public BigDecimal getMaxPriceFactor() {
            return maxPriceFactor;
        }
        
        public String getPricingRule() {
            return pricingRule;
        }
    }
    
    /**
     * 客户价格验证结果
     */
    class CustomerPriceValidationResult {
        private final boolean valid;
        private final String message;
        private final BigDecimal adjustedPrice;
        private final String validationRule;
        
        public CustomerPriceValidationResult(boolean valid, String message, 
                                           BigDecimal adjustedPrice, String validationRule) {
            this.valid = valid;
            this.message = message;
            this.adjustedPrice = adjustedPrice;
            this.validationRule = validationRule;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public BigDecimal getAdjustedPrice() {
            return adjustedPrice;
        }
        
        public String getValidationRule() {
            return validationRule;
        }
    }
}