package cn.aiedge.erp.price.engine.strategy.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 价格计算请求实体类
 * 包含计算价格所需的所有输入参数
 */
public class PriceCalculationRequest {
    
    /**
     * 请求ID（唯一标识）
     */
    private String requestId;
    
    /**
     * 产品ID
     */
    private String productId;
    
    /**
     * 产品SKU
     */
    private String sku;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品类别
     */
    private String category;
    
    /**
     * 产品成本价
     */
    private BigDecimal costPrice;
    
    /**
     * 产品基准价
     */
    private BigDecimal basePrice;
    
    /**
     * 产品市场参考价
     */
    private BigDecimal marketReferencePrice;
    
    /**
     * 客户ID
     */
    private String customerId;
    
    /**
     * 客户名称
     */
    private String customerName;
    
    /**
     * 客户等级
     */
    private String customerLevel;
    
    /**
     * 客户信用等级
     */
    private String customerCreditLevel;
    
    /**
     * 客户历史购买总额
     */
    private BigDecimal customerTotalPurchaseAmount;
    
    /**
     * 客户历史购买次数
     */
    private Integer customerPurchaseCount;
    
    /**
     * 客户所属区域
     */
    private String customerRegion;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 总购买数量（用于累计折扣）
     */
    private Integer totalQuantity;
    
    /**
     * 购买金额
     */
    private BigDecimal purchaseAmount;
    
    /**
     * 总购买金额（用于累计折扣）
     */
    private BigDecimal totalPurchaseAmount;
    
    /**
     * 销售渠道
     */
    private String salesChannel;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 国家
     */
    private String country;
    
    /**
     * 计算时间
     */
    private LocalDateTime calculationTime;
    
    /**
     * 订单类型
     */
    private String orderType;
    
    /**
     * 订单ID（关联现有订单）
     */
    private String orderId;
    
    /**
     * 是否包含运费
     */
    private Boolean includeShipping;
    
    /**
     * 运费金额
     */
    private BigDecimal shippingAmount;
    
    /**
     * 是否包含税费
     */
    private Boolean includeTax;
    
    /**
     * 税率
     */
    private BigDecimal taxRate;
    
    /**
     * 竞争对手价格列表
     */
    private Map<String, BigDecimal> competitorPrices;
    
    /**
     * 市场供需情况
     */
    private String marketSupplyDemand;
    
    /**
     * 季节因素
     */
    private String seasonFactor;
    
    /**
     * 节假日标记
     */
    private Boolean isHoliday;
    
    /**
     * 促销活动ID列表
     */
    private List<String> promotionIds;
    
    /**
     * 附加参数（用于扩展）
     */
    private Map<String, Object> additionalParams;
    
    /**
     * 是否立即计算（true: 实时计算, false: 预计算）
     */
    private Boolean immediateCalculation;
    
    /**
     * 计算模式
     */
    private CalculationMode calculationMode;
    
    /**
     * 是否返回详细计算过程
     */
    private Boolean returnDetailedProcess;
    
    /**
     * 请求时间
     */
    private LocalDateTime requestTime;
    
    /**
     * 请求来源（系统/用户）
     */
    private String requestSource;

    // 构造函数
    public PriceCalculationRequest() {
        this.calculationTime = LocalDateTime.now();
        this.requestTime = LocalDateTime.now();
        this.quantity = 1;
        this.totalQuantity = 1;
        this.purchaseAmount = BigDecimal.ZERO;
        this.totalPurchaseAmount = BigDecimal.ZERO;
        this.includeShipping = false;
        this.shippingAmount = BigDecimal.ZERO;
        this.includeTax = false;
        this.taxRate = BigDecimal.ZERO;
        this.isHoliday = false;
        this.immediateCalculation = true;
        this.calculationMode = CalculationMode.STANDARD;
        this.returnDetailedProcess = false;
        this.requestSource = "system";
    }

    public PriceCalculationRequest(String requestId, String productId, String customerId) {
        this();
        this.requestId = requestId;
        this.productId = productId;
        this.customerId = customerId;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getMarketReferencePrice() {
        return marketReferencePrice;
    }

    public void setMarketReferencePrice(BigDecimal marketReferencePrice) {
        this.marketReferencePrice = marketReferencePrice;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerLevel() {
        return customerLevel;
    }

    public void setCustomerLevel(String customerLevel) {
        this.customerLevel = customerLevel;
    }

    public String getCustomerCreditLevel() {
        return customerCreditLevel;
    }

    public void setCustomerCreditLevel(String customerCreditLevel) {
        this.customerCreditLevel = customerCreditLevel;
    }

    public BigDecimal getCustomerTotalPurchaseAmount() {
        return customerTotalPurchaseAmount;
    }

    public void setCustomerTotalPurchaseAmount(BigDecimal customerTotalPurchaseAmount) {
        this.customerTotalPurchaseAmount = customerTotalPurchaseAmount;
    }

    public Integer getCustomerPurchaseCount() {
        return customerPurchaseCount;
    }

    public void setCustomerPurchaseCount(Integer customerPurchaseCount) {
        this.customerPurchaseCount = customerPurchaseCount;
    }

    public String getCustomerRegion() {
        return customerRegion;
    }

    public void setCustomerRegion(String customerRegion) {
        this.customerRegion = customerRegion;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(BigDecimal purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public BigDecimal getTotalPurchaseAmount() {
        return totalPurchaseAmount;
    }

    public void setTotalPurchaseAmount(BigDecimal totalPurchaseAmount) {
        this.totalPurchaseAmount = totalPurchaseAmount;
    }

    public String getSalesChannel() {
        return salesChannel;
    }

    public void setSalesChannel(String salesChannel) {
        this.salesChannel = salesChannel;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDateTime getCalculationTime() {
        return calculationTime;
    }

    public void setCalculationTime(LocalDateTime calculationTime) {
        this.calculationTime = calculationTime;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Boolean getIncludeShipping() {
        return includeShipping;
    }

    public void setIncludeShipping(Boolean includeShipping) {
        this.includeShipping = includeShipping;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }

    public Boolean getIncludeTax() {
        return includeTax;
    }

    public void setIncludeTax(Boolean includeTax) {
        this.includeTax = includeTax;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public Map<String, BigDecimal> getCompetitorPrices() {
        return competitorPrices;
    }

    public void setCompetitorPrices(Map<String, BigDecimal> competitorPrices) {
        this.competitorPrices = competitorPrices;
    }

    public String getMarketSupplyDemand() {
        return marketSupplyDemand;
    }

    public void setMarketSupplyDemand(String marketSupplyDemand) {
        this.marketSupplyDemand = marketSupplyDemand;
    }

    public String getSeasonFactor() {
        return seasonFactor;
    }

    public void setSeasonFactor(String seasonFactor) {
        this.seasonFactor = seasonFactor;
    }

    public Boolean getIsHoliday() {
        return isHoliday;
    }

    public void setIsHoliday(Boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public List<String> getPromotionIds() {
        return promotionIds;
    }

    public void setPromotionIds(List<String> promotionIds) {
        this.promotionIds = promotionIds;
    }

    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
    }

    public Boolean getImmediateCalculation() {
        return immediateCalculation;
    }

    public void setImmediateCalculation(Boolean immediateCalculation) {
        this.immediateCalculation = immediateCalculation;
    }

    public CalculationMode getCalculationMode() {
        return calculationMode;
    }

    public void setCalculationMode(CalculationMode calculationMode) {
        this.calculationMode = calculationMode;
    }

    public Boolean getReturnDetailedProcess() {
        return returnDetailedProcess;
    }

    public void setReturnDetailedProcess(Boolean returnDetailedProcess) {
        this.returnDetailedProcess = returnDetailedProcess;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }
    
    public LocalDateTime getTimestamp() {
        return requestTime != null ? requestTime : LocalDateTime.now();
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getRequestSource() {
        return requestSource;
    }

    public void setRequestSource(String requestSource) {
        this.requestSource = requestSource;
    }

    /**
     * 获取有效的起始价格
     * 按照优先级选择价格源：基准价 > 成本价 > 市场参考价
     */
    public BigDecimal getEffectiveBasePrice() {
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
            return basePrice;
        } else if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) > 0) {
            return costPrice;
        } else if (marketReferencePrice != null && marketReferencePrice.compareTo(BigDecimal.ZERO) > 0) {
            return marketReferencePrice;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 验证请求参数是否有效
     */
    public boolean isValid() {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        
        if (quantity == null || quantity <= 0) {
            return false;
        }
        
        BigDecimal effectivePrice = getEffectiveBasePrice();
        if (effectivePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取请求的字符串表示
     */
    @Override
    public String toString() {
        return String.format(
            "PriceCalculationRequest{requestId='%s', productId='%s', customerId='%s', quantity=%d, basePrice=%s}",
            requestId, productId, customerId, quantity, basePrice
        );
    }

    /**
     * 计算模式枚举
     */
    public enum CalculationMode {
        STANDARD("标准计算", "使用标准定价策略和折扣规则"),
        OPTIMIZED("优化计算", "考虑更多因素的优化计算"),
        FAST("快速计算", "快速计算，忽略部分复杂规则"),
        SIMULATION("模拟计算", "用于价格策略模拟"),
        BATCH("批量计算", "批量价格计算"),
        REAL_TIME("实时计算", "实时响应价格计算请求");

        private final String displayName;
        private final String description;

        CalculationMode(String displayName, String description) {
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