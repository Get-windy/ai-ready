package cn.aiedge.erp.price.engine.strategy.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 折扣规则实体类
 * 记录折扣规则的具体配置和生效条件
 */
public class DiscountRule {
    
    /**
     * 规则ID（主键）
     */
    private String ruleId;
    
    /**
     * 规则名称
     */
    private String ruleName;
    
    /**
     * 规则类型
     */
    private DiscountType discountType;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * 折扣率（百分比，如10表示10%折扣）
     */
    private BigDecimal discountRate;
    
    /**
     * 固定折扣金额（如果同时有折扣率和固定金额，优先使用折扣率）
     */
    private BigDecimal fixedDiscountAmount;
    
    /**
     * 最低折扣金额
     */
    private BigDecimal minDiscountAmount;
    
    /**
     * 最高折扣金额
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * 条件表达式（Groovy表达式）
     */
    private String conditionExpression;
    
    /**
     * 规则优先级（数值越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 生效开始时间
     */
    private LocalDateTime effectiveFrom;
    
    /**
     * 生效结束时间
     */
    private LocalDateTime effectiveTo;
    
    /**
     * 适用产品ID列表（为空表示所有产品）
     */
    private List<String> applicableProductIds;
    
    /**
     * 适用产品类别列表（为空表示所有类别）
     */
    private List<String> applicableCategories;
    
    /**
     * 适用客户ID列表（为空表示所有客户）
     */
    private List<String> applicableCustomerIds;
    
    /**
     * 适用客户等级列表（为空表示所有等级）
     */
    private List<String> applicableCustomerLevels;
    
    /**
     * 适用区域列表（为空表示所有区域）
     */
    private List<String> applicableRegions;
    
    /**
     * 适用渠道列表（为空表示所有渠道）
     */
    private List<String> applicableChannels;
    
    /**
     * 最小购买数量
     */
    private Integer minPurchaseQuantity;
    
    /**
     * 最小购买金额
     */
    private BigDecimal minPurchaseAmount;
    
    /**
     * 是否可叠加（可以与其他折扣同时使用）
     */
    private Boolean stackable;
    
    /**
     * 最大叠加次数（0表示无限制）
     */
    private Integer maxStackCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    private String createdBy;
    
    /**
     * 更新人ID
     */
    private String updatedBy;
    
    /**
     * 版本号（用于乐观锁）
     */
    private Integer version;

    // 构造函数
    public DiscountRule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.enabled = true;
        this.priority = 100;
        this.discountRate = BigDecimal.ZERO;
        this.fixedDiscountAmount = BigDecimal.ZERO;
        this.minDiscountAmount = BigDecimal.ZERO;
        this.maxDiscountAmount = BigDecimal.valueOf(999999.99);
        this.minPurchaseAmount = BigDecimal.ZERO;
        this.stackable = true;
        this.maxStackCount = 0; // 0表示无限制
        this.version = 1;
    }

    public DiscountRule(String ruleId, String ruleName, DiscountType discountType) {
        this();
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.discountType = discountType;
    }

    // Getters and Setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getFixedDiscountAmount() {
        return fixedDiscountAmount;
    }

    public void setFixedDiscountAmount(BigDecimal fixedDiscountAmount) {
        this.fixedDiscountAmount = fixedDiscountAmount;
    }

    public BigDecimal getMinDiscountAmount() {
        return minDiscountAmount;
    }

    public void setMinDiscountAmount(BigDecimal minDiscountAmount) {
        this.minDiscountAmount = minDiscountAmount;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public List<String> getApplicableProductIds() {
        return applicableProductIds;
    }

    public void setApplicableProductIds(List<String> applicableProductIds) {
        this.applicableProductIds = applicableProductIds;
    }

    public List<String> getApplicableCategories() {
        return applicableCategories;
    }

    public void setApplicableCategories(List<String> applicableCategories) {
        this.applicableCategories = applicableCategories;
    }

    public List<String> getApplicableCustomerIds() {
        return applicableCustomerIds;
    }

    public void setApplicableCustomerIds(List<String> applicableCustomerIds) {
        this.applicableCustomerIds = applicableCustomerIds;
    }

    public List<String> getApplicableCustomerLevels() {
        return applicableCustomerLevels;
    }

    public void setApplicableCustomerLevels(List<String> applicableCustomerLevels) {
        this.applicableCustomerLevels = applicableCustomerLevels;
    }

    public List<String> getApplicableRegions() {
        return applicableRegions;
    }

    public void setApplicableRegions(List<String> applicableRegions) {
        this.applicableRegions = applicableRegions;
    }

    public List<String> getApplicableChannels() {
        return applicableChannels;
    }

    public void setApplicableChannels(List<String> applicableChannels) {
        this.applicableChannels = applicableChannels;
    }

    public Integer getMinPurchaseQuantity() {
        return minPurchaseQuantity;
    }

    public void setMinPurchaseQuantity(Integer minPurchaseQuantity) {
        this.minPurchaseQuantity = minPurchaseQuantity;
    }

    public BigDecimal getMinPurchaseAmount() {
        return minPurchaseAmount;
    }

    public void setMinPurchaseAmount(BigDecimal minPurchaseAmount) {
        this.minPurchaseAmount = minPurchaseAmount;
    }

    public Boolean getStackable() {
        return stackable;
    }

    public void setStackable(Boolean stackable) {
        this.stackable = stackable;
    }

    public Integer getMaxStackCount() {
        return maxStackCount;
    }

    public void setMaxStackCount(Integer maxStackCount) {
        this.maxStackCount = maxStackCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 判断规则是否在有效期内
     */
    public boolean isEffective() {
        if (!Boolean.TRUE.equals(enabled)) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return false;
        }
        
        return true;
    }

    /**
     * 判断规则是否适用于特定条件
     * 
     * @param productId 产品ID
     * @param customerId 客户ID
     * @param quantity 购买数量
     * @param amount 购买金额
     * @param region 区域
     * @param channel 渠道
     * @return 是否适用
     */
    public boolean isApplicable(String productId, String customerId, Integer quantity, 
                               BigDecimal amount, String region, String channel) {
        
        // 检查产品是否适用
        if (applicableProductIds != null && !applicableProductIds.isEmpty()) {
            if (!applicableProductIds.contains(productId)) {
                return false;
            }
        }
        
        // 检查客户是否适用
        if (applicableCustomerIds != null && !applicableCustomerIds.isEmpty()) {
            if (!applicableCustomerIds.contains(customerId)) {
                return false;
            }
        }
        
        // 检查区域是否适用
        if (applicableRegions != null && !applicableRegions.isEmpty()) {
            if (!applicableRegions.contains(region)) {
                return false;
            }
        }
        
        // 检查渠道是否适用
        if (applicableChannels != null && !applicableChannels.isEmpty()) {
            if (!applicableChannels.contains(channel)) {
                return false;
            }
        }
        
        // 检查最小购买数量
        if (minPurchaseQuantity != null && quantity != null) {
            if (quantity < minPurchaseQuantity) {
                return false;
            }
        }
        
        // 检查最小购买金额
        if (minPurchaseAmount != null && amount != null) {
            if (amount.compareTo(minPurchaseAmount) < 0) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 计算折扣金额
     * 
     * @param originalPrice 原始价格
     * @param quantity 购买数量
     * @return 折扣金额
     */
    public BigDecimal calculateDiscount(BigDecimal originalPrice, Integer quantity) {
        if (!isEffective()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        // 优先使用折扣率
        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            discount = originalPrice.multiply(discountRate).divide(BigDecimal.valueOf(100));
        } 
        // 其次使用固定折扣金额
        else if (fixedDiscountAmount != null && fixedDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            discount = fixedDiscountAmount;
        }
        
        // 应用数量倍数（如果是按数量的折扣）
        if (quantity != null && quantity > 1) {
            discount = discount.multiply(BigDecimal.valueOf(quantity));
        }
        
        // 检查折扣金额限制
        if (minDiscountAmount != null && discount.compareTo(minDiscountAmount) < 0) {
            discount = minDiscountAmount;
        }
        
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }
        
        return discount;
    }

    public boolean isValid() {
        return ruleId != null && !ruleId.isEmpty() && 
               discountType != null && 
               (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0 ||
                fixedDiscountAmount != null && fixedDiscountAmount.compareTo(BigDecimal.ZERO) > 0);
    }
    
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled);
    }
    
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) return false;
        
        if (applicableProductIds != null && !applicableProductIds.isEmpty()) {
            if (!applicableProductIds.contains(request.getProductId())) {
                return false;
            }
        }
        
        if (applicableCustomerIds != null && !applicableCustomerIds.isEmpty()) {
            if (!applicableCustomerIds.contains(request.getCustomerId())) {
                return false;
            }
        }
        
        if (applicableCustomerLevels != null && !applicableCustomerLevels.isEmpty()) {
            if (!applicableCustomerLevels.contains(request.getCustomerLevel())) {
                return false;
            }
        }
        
        if (applicableRegions != null && !applicableRegions.isEmpty()) {
            if (!applicableRegions.contains(request.getRegion())) {
                return false;
            }
        }
        
        if (applicableChannels != null && !applicableChannels.isEmpty()) {
            if (!applicableChannels.contains(request.getSalesChannel())) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isEffective(LocalDateTime timestamp) {
        if (timestamp == null) timestamp = LocalDateTime.now();
        
        if (effectiveFrom != null && timestamp.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveTo != null && timestamp.isAfter(effectiveTo)) {
            return false;
        }
        
        return true;
    }
    
    public boolean checkApplicability(PriceCalculationRequest request) {
        return isApplicable(request);
    }
    
    public boolean checkQuantityThreshold(Integer quantity) {
        if (minPurchaseQuantity == null) return true;
        if (quantity == null) return false;
        return quantity >= minPurchaseQuantity;
    }
    
    public boolean checkPriceThreshold(BigDecimal price) {
        if (minPurchaseAmount == null) return true;
        if (price == null) return false;
        return price.compareTo(minPurchaseAmount) >= 0;
    }
    
    public boolean isExclusive() {
        return !Boolean.TRUE.equals(stackable);
    }
    
    public boolean isSameProductCategory(String productId) {
        if (applicableProductIds == null || applicableProductIds.isEmpty()) return false;
        return applicableProductIds.contains(productId);
    }
    
    public BigDecimal getDiscountValue() {
        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            return discountRate;
        }
        return fixedDiscountAmount;
    }
    
    public Integer getMinQuantity() {
        return minPurchaseQuantity;
    }
    
    public List<String> getBundleProductIds() {
        return applicableProductIds;
    }

    /**
     * 折扣类型枚举
     */
    public enum DiscountType {
        STANDARD("标准折扣", "基于客户等级或购买历史的折扣"),
        PROMOTIONAL("促销折扣", "促销活动期间的折扣"),
        VOLUME("批量折扣", "基于购买数量的阶梯折扣"),
        BUNDLE("组合折扣", "多产品组合购买的折扣"),
        SEASONAL("季节折扣", "季节性产品的折扣"),
        CLEARANCE("清仓折扣", "库存清理的折扣"),
        LOYALTY("忠诚度折扣", "忠诚客户专属折扣"),
        FIRST_PURCHASE("首购折扣", "首次购买的优惠折扣"),
        BIRTHDAY("生日折扣", "客户生日期间的折扣"),
        SPECIAL_EVENT("特殊事件折扣", "特殊事件或节假日的折扣");

        private final String displayName;
        private final String description;

        DiscountType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}