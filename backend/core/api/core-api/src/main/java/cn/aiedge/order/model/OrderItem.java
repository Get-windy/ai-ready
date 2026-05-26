package cn.aiedge.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项模型类
 * 表示订单中的一个具体商品或服务项
 */
@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 订单ID，关联到主订单
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    /**
     * 商品ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    /**
     * 商品SKU编码
     */
    @Column(name = "sku_code", length = 100)
    private String skuCode;
    
    /**
     * 商品名称
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    
    /**
     * 商品规格
     */
    @Column(name = "specification", length = 500)
    private String specification;
    
    /**
     * 单价
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
    
    /**
     * 数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    /**
     * 总金额
     */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;
    
    /**
     * 折扣金额
     */
    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    /**
     * 实际支付金额
     */
    @Column(name = "actual_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualAmount;
    
    /**
     * 税率百分比
     */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    /**
     * 税额
     */
    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    /**
     * 重量（kg）
     */
    @Column(name = "weight", precision = 10, scale = 3)
    private BigDecimal weight;
    
    /**
     * 体积（m³）
     */
    @Column(name = "volume", precision = 10, scale = 4)
    private BigDecimal volume;
    
    /**
     * 批次号
     */
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    
    /**
     * 生产日期
     */
    @Column(name = "production_date")
    private LocalDateTime productionDate;
    
    /**
     * 保质期（天）
     */
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;
    
    /**
     * 状态：1-待发货 2-已发货 3-已收货 4-已完成 5-已取消
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
    
    /**
     * 发货时间
     */
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;
    
    /**
     * 收货时间
     */
    @Column(name = "receipt_time")
    private LocalDateTime receiptTime;
    
    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID
     */
    @Column(name = "create_user_id")
    private Long createUserId;
    
    /**
     * 更新人ID
     */
    @Column(name = "update_user_id")
    private Long updateUserId;
    
    // 构造函数
    public OrderItem() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    public OrderItem(Long orderId, Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
        this();
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        this.actualAmount = this.totalAmount.subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getSkuCode() {
        return skuCode;
    }
    
    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getSpecification() {
        return specification;
    }
    
    public void setSpecification(String specification) {
        this.specification = specification;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public BigDecimal getActualAmount() {
        return actualAmount;
    }
    
    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }
    
    public BigDecimal getTaxRate() {
        return taxRate;
    }
    
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
    
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }
    
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    
    public String getBatchNumber() {
        return batchNumber;
    }
    
    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }
    
    public LocalDateTime getProductionDate() {
        return productionDate;
    }
    
    public void setProductionDate(LocalDateTime productionDate) {
        this.productionDate = productionDate;
    }
    
    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }
    
    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }
    
    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }
    
    public LocalDateTime getReceiptTime() {
        return receiptTime;
    }
    
    public void setReceiptTime(LocalDateTime receiptTime) {
        this.receiptTime = receiptTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Long getCreateUserId() {
        return createUserId;
    }
    
    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }
    
    public Long getUpdateUserId() {
        return updateUserId;
    }
    
    public void setUpdateUserId(Long updateUserId) {
        this.updateUserId = updateUserId;
    }
    
    /**
     * 计算总金额
     */
    public void calculateTotalAmount() {
        if (unitPrice != null && quantity != null) {
            this.totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        }
    }
    
    /**
     * 计算实际金额
     */
    public void calculateActualAmount() {
        if (totalAmount != null) {
            this.actualAmount = totalAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        }
    }
    
    /**
     * 计算税额
     */
    public void calculateTaxAmount() {
        if (totalAmount != null && taxRate != null) {
            this.taxAmount = totalAmount.multiply(taxRate).divide(new BigDecimal(100));
        }
    }
    
    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", skuCode='" + skuCode + '\'' +
                ", productName='" + productName + '\'' +
                ", specification='" + specification + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", discountAmount=" + discountAmount +
                ", actualAmount=" + actualAmount +
                ", status=" + status +
                '}';
    }
}