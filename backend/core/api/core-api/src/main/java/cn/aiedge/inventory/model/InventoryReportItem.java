package cn.aiedge.inventory.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存报告明细项模型类
 * 表示库存报告中的每个商品明细
 */
@Entity
@Table(name = "inventory_report_items")
public class InventoryReportItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的库存报告
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private InventoryReport inventoryReport;
    
    /**
     * 商品ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    /**
     * 商品编码
     */
    @Column(name = "product_code", length = 50)
    private String productCode;
    
    /**
     * 商品名称
     */
    @Column(name = "product_name", length = 200)
    private String productName;
    
    /**
     * 规格型号
     */
    @Column(name = "specification", length = 100)
    private String specification;
    
    /**
     * 单位
     */
    @Column(name = "unit", length = 20)
    private String unit;
    
    /**
     * 账面数量
     */
    @Column(name = "book_quantity", precision = 18, scale = 4)
    private BigDecimal bookQuantity;
    
    /**
     * 实际数量
     */
    @Column(name = "actual_quantity", precision = 18, scale = 4)
    private BigDecimal actualQuantity;
    
    /**
     * 差异数量
     */
    @Column(name = "diff_quantity", precision = 18, scale = 4)
    private BigDecimal diffQuantity;
    
    /**
     * 单价
     */
    @Column(name = "unit_price", precision = 18, scale = 4)
    private BigDecimal unitPrice;
    
    /**
     * 差异金额
     */
    @Column(name = "diff_amount", precision = 18, scale = 4)
    private BigDecimal diffAmount;
    
    /**
     * 差异原因
     */
    @Column(name = "diff_reason", length = 500)
    private String diffReason;
    
    /**
     * 状态（0-正常，1-差异）
     */
    @Column(name = "status")
    private Integer status;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    // 构造函数
    public InventoryReportItem() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public InventoryReport getInventoryReport() {
        return inventoryReport;
    }
    
    public void setInventoryReport(InventoryReport inventoryReport) {
        this.inventoryReport = inventoryReport;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductCode() {
        return productCode;
    }
    
    public void setProductCode(String productCode) {
        this.productCode = productCode;
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
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public BigDecimal getBookQuantity() {
        return bookQuantity;
    }
    
    public void setBookQuantity(BigDecimal bookQuantity) {
        this.bookQuantity = bookQuantity;
    }
    
    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }
    
    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }
    
    public BigDecimal getDiffQuantity() {
        return diffQuantity;
    }
    
    public void setDiffQuantity(BigDecimal diffQuantity) {
        this.diffQuantity = diffQuantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public BigDecimal getDiffAmount() {
        return diffAmount;
    }
    
    public void setDiffAmount(BigDecimal diffAmount) {
        this.diffAmount = diffAmount;
    }
    
    public String getDiffReason() {
        return diffReason;
    }
    
    public void setDiffReason(String diffReason) {
        this.diffReason = diffReason;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
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
}