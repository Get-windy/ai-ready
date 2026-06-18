package cn.aiedge.erp.sales.pricing.service.algorithm;

import cn.aiedge.erp.sales.pricing.enums.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动定价算法接口
 * 临时性促销活动的价格规则实现
 */
public interface IPromotionPricingAlgorithm {
    
    /**
     * 计算促销活动价格
     * 
     * @param basePrice 基础价格
     * @param promotionType 促销类型
     * @param promotionParams 促销参数
     * @param promotionRules 促销规则
     * @return 促销后的价格
     */
    BigDecimal calculatePromotionPrice(BigDecimal basePrice, PromotionType promotionType,
                                      PromotionParameters promotionParams, PromotionRules promotionRules);
    
    /**
     * 验证促销活动有效性
     * 
     * @param promotionType 促销类型
     * @param promotionParams 促销参数
     * @param promotionRules 促销规则
     * @param currentTime 当前时间
     * @return 验证结果
     */
    PromotionValidationResult validatePromotion(PromotionType promotionType,
                                               PromotionParameters promotionParams,
                                               PromotionRules promotionRules,
                                               LocalDateTime currentTime);
    
    /**
     * 计算多个促销叠加价格
     * 
     * @param basePrice 基础价格
     * @param promotions 促销列表
     * @return 最终促销价格
     */
    BigDecimal calculateMultiplePromotionsPrice(BigDecimal basePrice, List<PromotionItem> promotions);
    
    /**
     * 检查促销活动冲突
     * 
     * @param existingPromotions 现有促销活动
     * @param newPromotion 新促销活动
     * @return 冲突检查结果
     */
    PromotionConflictResult checkPromotionConflict(List<PromotionItem> existingPromotions, PromotionItem newPromotion);
    
    /**
     * 计算最优促销组合
     * 
     * @param basePrice 基础价格
     * @param availablePromotions 可用促销活动
     * @param constraints 约束条件
     * @return 最优促销组合结果
     */
    OptimalPromotionResult calculateOptimalPromotion(BigDecimal basePrice,
                                                    List<PromotionItem> availablePromotions,
                                                    PromotionConstraints constraints);
    
    /**
     * 获取促销类型的默认配置
     */
    PromotionTypeConfig getPromotionTypeConfig(PromotionType promotionType);
    
    /**
     * 促销参数
     */
    class PromotionParameters {
        private final BigDecimal discountRate;          // 折扣率（0-1）
        private final BigDecimal conditionAmount;       // 条件金额
        private final BigDecimal rebateAmount;          // 减免金额
        private final Integer conditionQuantity;        // 条件数量
        private final Integer freeQuantity;             // 赠送数量
        private final BigDecimal bundlePrice;           // 组合价格
        private final BigDecimal couponValue;           // 优惠券面值
        private final Integer pointsRequired;           // 所需积分
        private final BigDecimal pointsToMoneyRatio;    // 积分兑现金比例
        private final LocalDateTime startTime;          // 开始时间
        private final LocalDateTime endTime;            // 结束时间
        
        public PromotionParameters(BigDecimal discountRate, BigDecimal conditionAmount, 
                                  BigDecimal rebateAmount, Integer conditionQuantity, 
                                  Integer freeQuantity, BigDecimal bundlePrice, 
                                  BigDecimal couponValue, Integer pointsRequired,
                                  BigDecimal pointsToMoneyRatio, LocalDateTime startTime,
                                  LocalDateTime endTime) {
            this.discountRate = discountRate;
            this.conditionAmount = conditionAmount;
            this.rebateAmount = rebateAmount;
            this.conditionQuantity = conditionQuantity;
            this.freeQuantity = freeQuantity;
            this.bundlePrice = bundlePrice;
            this.couponValue = couponValue;
            this.pointsRequired = pointsRequired;
            this.pointsToMoneyRatio = pointsToMoneyRatio;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getter方法
        public BigDecimal getDiscountRate() { return discountRate; }
        public BigDecimal getConditionAmount() { return conditionAmount; }
        public BigDecimal getRebateAmount() { return rebateAmount; }
        public Integer getConditionQuantity() { return conditionQuantity; }
        public Integer getFreeQuantity() { return freeQuantity; }
        public BigDecimal getBundlePrice() { return bundlePrice; }
        public BigDecimal getCouponValue() { return couponValue; }
        public Integer getPointsRequired() { return pointsRequired; }
        public BigDecimal getPointsToMoneyRatio() { return pointsToMoneyRatio; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
    }
    
    /**
     * 促销规则
     */
    class PromotionRules {
        private final boolean allowMultiplePromotions;  // 允许多个促销叠加
        private final boolean memberOnly;               // 仅限会员
        private final boolean firstOrderOnly;           // 仅限首单
        private final BigDecimal maxDiscountAmount;     // 最大优惠金额
        private final BigDecimal minOrderAmount;        // 最小订单金额
        private final List<String> applicableProducts;  // 适用商品
        private final List<String> applicableChannels;  // 适用渠道
        
        public PromotionRules(boolean allowMultiplePromotions, boolean memberOnly,
                             boolean firstOrderOnly, BigDecimal maxDiscountAmount,
                             BigDecimal minOrderAmount, List<String> applicableProducts,
                             List<String> applicableChannels) {
            this.allowMultiplePromotions = allowMultiplePromotions;
            this.memberOnly = memberOnly;
            this.firstOrderOnly = firstOrderOnly;
            this.maxDiscountAmount = maxDiscountAmount;
            this.minOrderAmount = minOrderAmount;
            this.applicableProducts = applicableProducts;
            this.applicableChannels = applicableChannels;
        }
        
        // Getter方法
        public boolean isAllowMultiplePromotions() { return allowMultiplePromotions; }
        public boolean isMemberOnly() { return memberOnly; }
        public boolean isFirstOrderOnly() { return firstOrderOnly; }
        public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
        public BigDecimal getMinOrderAmount() { return minOrderAmount; }
        public List<String> getApplicableProducts() { return applicableProducts; }
        public List<String> getApplicableChannels() { return applicableChannels; }
    }
    
    /**
     * 促销验证结果
     */
    class PromotionValidationResult {
        private final boolean valid;
        private final String message;
        private final String validationRule;
        
        public PromotionValidationResult(boolean valid, String message, String validationRule) {
            this.valid = valid;
            this.message = message;
            this.validationRule = validationRule;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getValidationRule() { return validationRule; }
    }
    
    /**
     * 促销冲突检查结果
     */
    class PromotionConflictResult {
        private final boolean hasConflict;
        private final String conflictType;
        private final String conflictDescription;
        private final List<String> conflictingPromotionIds;
        
        public PromotionConflictResult(boolean hasConflict, String conflictType,
                                      String conflictDescription, List<String> conflictingPromotionIds) {
            this.hasConflict = hasConflict;
            this.conflictType = conflictType;
            this.conflictDescription = conflictDescription;
            this.conflictingPromotionIds = conflictingPromotionIds;
        }
        
        public boolean hasConflict() { return hasConflict; }
        public String getConflictType() { return conflictType; }
        public String getConflictDescription() { return conflictDescription; }
        public List<String> getConflictingPromotionIds() { return conflictingPromotionIds; }
    }
    
    /**
     * 最优促销组合结果
     */
    class OptimalPromotionResult {
        private final List<PromotionItem> selectedPromotions;
        private final BigDecimal finalPrice;
        private final BigDecimal totalDiscount;
        private final String selectionStrategy;
        
        public OptimalPromotionResult(List<PromotionItem> selectedPromotions,
                                     BigDecimal finalPrice, BigDecimal totalDiscount,
                                     String selectionStrategy) {
            this.selectedPromotions = selectedPromotions;
            this.finalPrice = finalPrice;
            this.totalDiscount = totalDiscount;
            this.selectionStrategy = selectionStrategy;
        }
        
        public List<PromotionItem> getSelectedPromotions() { return selectedPromotions; }
        public BigDecimal getFinalPrice() { return finalPrice; }
        public BigDecimal getTotalDiscount() { return totalDiscount; }
        public String getSelectionStrategy() { return selectionStrategy; }
    }
    
    /**
     * 促销类型配置
     */
    class PromotionTypeConfig {
        private final PromotionType promotionType;
        private final String algorithmName;
        private final int priority;
        private final boolean requiresValidation;
        private final String defaultParameters;
        
        public PromotionTypeConfig(PromotionType promotionType, String algorithmName,
                                  int priority, boolean requiresValidation, String defaultParameters) {
            this.promotionType = promotionType;
            this.algorithmName = algorithmName;
            this.priority = priority;
            this.requiresValidation = requiresValidation;
            this.defaultParameters = defaultParameters;
        }
        
        public PromotionType getPromotionType() { return promotionType; }
        public String getAlgorithmName() { return algorithmName; }
        public int getPriority() { return priority; }
        public boolean requiresValidation() { return requiresValidation; }
        public String getDefaultParameters() { return defaultParameters; }
    }
    
    /**
     * 促销约束条件
     */
    class PromotionConstraints {
        private final BigDecimal maxTotalDiscount;
        private final BigDecimal minFinalPrice;
        private final int maxPromotionCount;
        private final List<String> excludedPromotionTypes;
        
        public PromotionConstraints(BigDecimal maxTotalDiscount, BigDecimal minFinalPrice,
                                   int maxPromotionCount, List<String> excludedPromotionTypes) {
            this.maxTotalDiscount = maxTotalDiscount;
            this.minFinalPrice = minFinalPrice;
            this.maxPromotionCount = maxPromotionCount;
            this.excludedPromotionTypes = excludedPromotionTypes;
        }
        
        public BigDecimal getMaxTotalDiscount() { return maxTotalDiscount; }
        public BigDecimal getMinFinalPrice() { return minFinalPrice; }
        public int getMaxPromotionCount() { return maxPromotionCount; }
        public List<String> getExcludedPromotionTypes() { return excludedPromotionTypes; }
    }
    
    /**
     * 促销项目
     */
    class PromotionItem {
        private final String promotionId;
        private final PromotionType promotionType;
        private final PromotionParameters parameters;
        private final PromotionRules rules;
        private final String promotionName;
        
        public PromotionItem(String promotionId, PromotionType promotionType,
                            PromotionParameters parameters, PromotionRules rules,
                            String promotionName) {
            this.promotionId = promotionId;
            this.promotionType = promotionType;
            this.parameters = parameters;
            this.rules = rules;
            this.promotionName = promotionName;
        }
        
        public String getPromotionId() { return promotionId; }
        public PromotionType getPromotionType() { return promotionType; }
        public PromotionParameters getParameters() { return parameters; }
        public PromotionRules getRules() { return rules; }
        public String getPromotionName() { return promotionName; }
    }
}