package cn.aiedge.erp.price.engine.strategy.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 价格计算结果实体类
 * 包含价格计算的所有输出结果和详细过程
 */
public class PriceCalculationResult {
    
    /**
     * 结果ID（与请求ID对应）
     */
    private String resultId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 原始基准价
     */
    private BigDecimal originalBasePrice;
    
    /**
     * 基础价格（应用定价策略后）
     */
    private BigDecimal basePrice;
    
    /**
     * 应用的主要定价策略ID
     */
    private String primaryPricingStrategyId;
    
    /**
     * 应用的主要定价策略名称
     */
    private String primaryPricingStrategyName;
    
    /**
     * 总折扣金额
     */
    private BigDecimal totalDiscountAmount;
    
    /**
     * 总折扣率（百分比）
     */
    private BigDecimal totalDiscountRate;
    
    /**
     * 折扣后价格
     */
    private BigDecimal discountedPrice;
    
    /**
     * 最终价格（含税和运费等）
     */
    private BigDecimal finalPrice;
    
    /**
     * 单价（最终价格除以数量）
     */
    private BigDecimal unitPrice;
    
    /**
     * 总价（最终价格乘以数量）
     */
    private BigDecimal totalPrice;
    
    /**
     * 成本价
     */
    private BigDecimal costPrice;
    
    /**
     * 毛利率（百分比）
     */
    private BigDecimal grossProfitMargin;
    
    /**
     * 折扣明细列表
     */
    private List<DiscountDetail> discountDetails;
    
    /**
     * 应用的价格策略列表
     */
    private List<AppliedStrategy> appliedStrategies;
    
    /**
     * 计算过程说明
     */
    private String calculationExplanation;
    
    /**
     * 计算是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息（如果计算失败）
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 计算开始时间
     */
    private LocalDateTime calculationStartTime;
    
    /**
     * 计算结束时间
     */
    private LocalDateTime calculationEndTime;
    
    /**
     * 计算耗时（毫秒）
     */
    private Long calculationDurationMs;
    
    /**
     * 计算引擎版本
     */
    private String engineVersion;
    
    /**
     * 是否经过缓存
     */
    private Boolean cached;
    
    /**
     * 缓存命中键
     */
    private String cacheKey;
    
    /**
     * 建议信息
     */
    private String suggestion;
    
    /**
     * 警告信息列表
     */
    private List<String> warnings;
    
    /**
     * 附加数据（用于扩展）
     */
    private Object additionalData;

    // 构造函数
    public PriceCalculationResult() {
        this.discountDetails = new ArrayList<>();
        this.appliedStrategies = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.success = true;
        this.cached = false;
        this.originalBasePrice = BigDecimal.ZERO;
        this.basePrice = BigDecimal.ZERO;
        this.totalDiscountAmount = BigDecimal.ZERO;
        this.totalDiscountRate = BigDecimal.ZERO;
        this.discountedPrice = BigDecimal.ZERO;
        this.finalPrice = BigDecimal.ZERO;
        this.unitPrice = BigDecimal.ZERO;
        this.totalPrice = BigDecimal.ZERO;
        this.costPrice = BigDecimal.ZERO;
        this.grossProfitMargin = BigDecimal.ZERO;
        this.calculationStartTime = LocalDateTime.now();
    }

    public PriceCalculationResult(String resultId, String requestId) {
        this();
        this.resultId = resultId;
        this.requestId = requestId;
    }

    // Getters and Setters
    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public BigDecimal getOriginalBasePrice() {
        return originalBasePrice;
    }

    public void setOriginalBasePrice(BigDecimal originalBasePrice) {
        this.originalBasePrice = originalBasePrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getPrimaryPricingStrategyId() {
        return primaryPricingStrategyId;
    }

    public void setPrimaryPricingStrategyId(String primaryPricingStrategyId) {
        this.primaryPricingStrategyId = primaryPricingStrategyId;
    }

    public String getPrimaryPricingStrategyName() {
        return primaryPricingStrategyName;
    }

    public void setPrimaryPricingStrategyName(String primaryPricingStrategyName) {
        this.primaryPricingStrategyName = primaryPricingStrategyName;
    }

    public BigDecimal getTotalDiscountAmount() {
        return totalDiscountAmount;
    }

    public void setTotalDiscountAmount(BigDecimal totalDiscountAmount) {
        this.totalDiscountAmount = totalDiscountAmount;
    }

    public BigDecimal getTotalDiscountRate() {
        return totalDiscountRate;
    }

    public void setTotalDiscountRate(BigDecimal totalDiscountRate) {
        this.totalDiscountRate = totalDiscountRate;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getGrossProfitMargin() {
        return grossProfitMargin;
    }

    public void setGrossProfitMargin(BigDecimal grossProfitMargin) {
        this.grossProfitMargin = grossProfitMargin;
    }

    public List<DiscountDetail> getDiscountDetails() {
        return discountDetails;
    }

    public void setDiscountDetails(List<DiscountDetail> discountDetails) {
        this.discountDetails = discountDetails;
    }

    public List<AppliedStrategy> getAppliedStrategies() {
        return appliedStrategies;
    }

    public void setAppliedStrategies(List<AppliedStrategy> appliedStrategies) {
        this.appliedStrategies = appliedStrategies;
    }

    public String getCalculationExplanation() {
        return calculationExplanation;
    }

    public void setCalculationExplanation(String calculationExplanation) {
        this.calculationExplanation = calculationExplanation;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getCalculationStartTime() {
        return calculationStartTime;
    }

    public void setCalculationStartTime(LocalDateTime calculationStartTime) {
        this.calculationStartTime = calculationStartTime;
    }

    public LocalDateTime getCalculationEndTime() {
        return calculationEndTime;
    }

    public void setCalculationEndTime(LocalDateTime calculationEndTime) {
        this.calculationEndTime = calculationEndTime;
        if (calculationStartTime != null && calculationEndTime != null) {
            this.calculationDurationMs = java.time.Duration.between(calculationStartTime, calculationEndTime).toMillis();
        }
    }

    public Long getCalculationDurationMs() {
        return calculationDurationMs;
    }

    public void setCalculationDurationMs(Long calculationDurationMs) {
        this.calculationDurationMs = calculationDurationMs;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public Boolean getCached() {
        return cached;
    }

    public void setCached(Boolean cached) {
        this.cached = cached;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Object getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Object additionalData) {
        this.additionalData = additionalData;
    }

    /**
     * 添加折扣明细
     */
    public void addDiscountDetail(DiscountDetail detail) {
        if (discountDetails == null) {
            discountDetails = new ArrayList<>();
        }
        discountDetails.add(detail);
    }
    
    public void addDiscountDetail(String ruleId, DiscountRule.DiscountType discountType, 
                                  BigDecimal discountAmount, String description) {
        DiscountDetail detail = new DiscountDetail();
        detail.setRuleId(ruleId);
        detail.setDiscountType(discountType);
        detail.setDiscountAmount(discountAmount);
        detail.setConditionDescription(description);
        detail.setApplied(true);
        addDiscountDetail(detail);
    }

    /**
     * 添加应用策略
     */
    public void addAppliedStrategy(AppliedStrategy strategy) {
        if (appliedStrategies == null) {
            appliedStrategies = new ArrayList<>();
        }
        appliedStrategies.add(strategy);
    }

    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    /**
     * 完成计算并设置结束时间
     */
    public void completeCalculation() {
        this.calculationEndTime = LocalDateTime.now();
        if (calculationStartTime != null) {
            this.calculationDurationMs = java.time.Duration.between(calculationStartTime, calculationEndTime).toMillis();
        }
    }

    /**
     * 设置错误状态
     */
    public void setError(String errorCode, String errorMessage) {
        this.success = false;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        completeCalculation();
    }

    /**
     * 计算并设置最终价格（含折扣）
     */
    public void calculateFinalPrice(Integer quantity) {
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            setError("INVALID_BASE_PRICE", "基础价格无效");
            return;
        }
        
        // 计算折扣后价格
        if (totalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            discountedPrice = basePrice.subtract(totalDiscountAmount);
            if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
                discountedPrice = BigDecimal.ZERO;
                addWarning("折扣金额超过基础价格，已调整折扣后价格为0");
            }
        } else {
            discountedPrice = basePrice;
        }
        
        // 计算总价
        if (quantity != null && quantity > 0) {
            totalPrice = discountedPrice.multiply(BigDecimal.valueOf(quantity));
            unitPrice = discountedPrice;
        } else {
            totalPrice = discountedPrice;
            unitPrice = discountedPrice;
        }
        
        // 计算最终价格（默认与折扣后价格相同，可扩展为含税和运费）
        finalPrice = discountedPrice;
        
        // 计算折扣率
        if (basePrice.compareTo(BigDecimal.ZERO) > 0) {
            totalDiscountRate = totalDiscountAmount.multiply(BigDecimal.valueOf(100)).divide(basePrice, 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // 计算毛利率
        if (costPrice.compareTo(BigDecimal.ZERO) > 0 && discountedPrice.compareTo(BigDecimal.ZERO) > 0) {
            grossProfitMargin = discountedPrice.subtract(costPrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(discountedPrice, 2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * 获取结果的字符串表示
     */
    @Override
    public String toString() {
        if (!Boolean.TRUE.equals(success)) {
            return String.format(
                "PriceCalculationResult{resultId='%s', success=false, error='%s'}",
                resultId, errorMessage
            );
        }
        
        return String.format(
            "PriceCalculationResult{resultId='%s', basePrice=%s, totalDiscount=%s, finalPrice=%s, duration=%dms}",
            resultId, basePrice, totalDiscountAmount, finalPrice, calculationDurationMs
        );
    }

    /**
     * 折扣明细内部类
     */
    public static class DiscountDetail {
        private String ruleId;
        private String ruleName;
        private DiscountRule.DiscountType discountType;
        private BigDecimal discountAmount;
        private BigDecimal discountRate;
        private String conditionDescription;
        private Boolean applied;
        private String reasonIfNotApplied;

        public DiscountDetail() {}

        public DiscountDetail(String ruleId, String ruleName, DiscountRule.DiscountType discountType, 
                             BigDecimal discountAmount, BigDecimal discountRate) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.discountType = discountType;
            this.discountAmount = discountAmount;
            this.discountRate = discountRate;
            this.applied = true;
        }

        // Getters and Setters
        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        
        public DiscountRule.DiscountType getDiscountType() { return discountType; }
        public void setDiscountType(DiscountRule.DiscountType discountType) { this.discountType = discountType; }
        
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        
        public BigDecimal getDiscountRate() { return discountRate; }
        public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
        
        public String getConditionDescription() { return conditionDescription; }
        public void setConditionDescription(String conditionDescription) { this.conditionDescription = conditionDescription; }
        
        public Boolean getApplied() { return applied; }
        public void setApplied(Boolean applied) { this.applied = applied; }
        
        public String getReasonIfNotApplied() { return reasonIfNotApplied; }
        public void setReasonIfNotApplied(String reasonIfNotApplied) { this.reasonIfNotApplied = reasonIfNotApplied; }
    }

    /**
     * 应用策略内部类
     */
    public static class AppliedStrategy {
        private String strategyId;
        private String strategyName;
        private PricingStrategy.StrategyType strategyType;
        private BigDecimal adjustmentFactor;
        private String adjustmentDescription;
        private Integer priority;

        public AppliedStrategy() {}

        public AppliedStrategy(String strategyId, String strategyName, 
                              PricingStrategy.StrategyType strategyType, BigDecimal adjustmentFactor) {
            this.strategyId = strategyId;
            this.strategyName = strategyName;
            this.strategyType = strategyType;
            this.adjustmentFactor = adjustmentFactor;
        }

        // Getters and Setters
        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
        
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        
        public PricingStrategy.StrategyType getStrategyType() { return strategyType; }
        public void setStrategyType(PricingStrategy.StrategyType strategyType) { this.strategyType = strategyType; }
        
        public BigDecimal getAdjustmentFactor() { return adjustmentFactor; }
        public void setAdjustmentFactor(BigDecimal adjustmentFactor) { this.adjustmentFactor = adjustmentFactor; }
        
        public String getAdjustmentDescription() { return adjustmentDescription; }
        public void setAdjustmentDescription(String adjustmentDescription) { this.adjustmentDescription = adjustmentDescription; }
        
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }
}