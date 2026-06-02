package cn.aiedge.erp.price.engine.strategy.entity;

import java.math.BigDecimal;

/**
 * 价格层级实体
 * 定义不同客户等级/数量阶梯对应的价格
 */
public class PriceTier {

    /**
     * 层级ID
     */
    private String tierId;

    /**
     * 层级名称（如：批发价、经销商价、会员价、促销价）
     */
    private String tierName;

    /**
     * 层级编码
     */
    private String tierCode;

    /**
     * 适用客户等级（strategic/core/normal/new）
     */
    private String customerLevel;

    /**
     * 产品ID
     */
    private String productId;

    /**
     * 产品SKU
     */
    private String sku;

    /**
     * 产品类别ID
     */
    private String categoryId;

    /**
     * 最低数量（达到此数量才适用此层级）
     */
    private Integer minQuantity;

    /**
     * 最高数量（超过此数量进入下一层级）
     */
    private Integer maxQuantity;

    /**
     * 层级单价（固定价模式）
     */
    private BigDecimal tierPrice;

    /**
     * 折扣率（相对基准价的折扣，0-100）
     */
    private BigDecimal discountRate;

    /**
     * 价格系数（相对基准价的乘数）
     */
    private BigDecimal priceFactor;

    /**
     * 定价模式：fixed-固定价 / discount-折扣 / factor-系数
     */
    private String pricingMode;

    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 状态：active/inactive
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    // 构造函数
    public PriceTier() {
        this.priority = 100;
        this.status = "active";
        this.pricingMode = "discount";
        this.priceFactor = BigDecimal.ONE;
        this.discountRate = BigDecimal.ZERO;
    }

    /**
     * 根据基准价计算层级价格
     */
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        switch (pricingMode) {
            case "fixed":
                return tierPrice != null ? tierPrice : basePrice;
            case "discount":
                if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
                    return basePrice.multiply(BigDecimal.ONE.subtract(
                            discountRate.divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP)))
                            .setScale(2, BigDecimal.ROUND_HALF_UP);
                }
                return basePrice;
            case "factor":
                if (priceFactor != null && priceFactor.compareTo(BigDecimal.ZERO) > 0) {
                    return basePrice.multiply(priceFactor).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
                return basePrice;
            default:
                return basePrice;
        }
    }

    /**
     * 检查指定数量是否在此层级范围内
     */
    public boolean isQuantityInRange(Integer quantity) {
        if (quantity == null) return false;
        if (minQuantity != null && quantity < minQuantity) return false;
        if (maxQuantity != null && quantity > maxQuantity) return false;
        return true;
    }

    /**
     * 检查是否适用于指定客户等级
     */
    public boolean isCustomerLevelApplicable(String level) {
        if (customerLevel == null || customerLevel.isEmpty()) return true;
        if (level == null) return false;
        return customerLevel.equals(level) || "all".equals(customerLevel);
    }

    // Getters and Setters
    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public String getTierCode() { return tierCode; }
    public void setTierCode(String tierCode) { this.tierCode = tierCode; }

    public String getCustomerLevel() { return customerLevel; }
    public void setCustomerLevel(String customerLevel) { this.customerLevel = customerLevel; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }

    public BigDecimal getTierPrice() { return tierPrice; }
    public void setTierPrice(BigDecimal tierPrice) { this.tierPrice = tierPrice; }

    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }

    public BigDecimal getPriceFactor() { return priceFactor; }
    public void setPriceFactor(BigDecimal priceFactor) { this.priceFactor = priceFactor; }

    public String getPricingMode() { return pricingMode; }
    public void setPricingMode(String pricingMode) { this.pricingMode = pricingMode; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
