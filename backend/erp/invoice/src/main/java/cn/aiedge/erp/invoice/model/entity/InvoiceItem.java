package cn.aiedge.erp.invoice.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 发票行项实体类
 * 表示发票中的具体商品或服务明细
 */
@Entity
@Table(name = "invoice_item")
@Data
@EqualsAndHashCode(callSuper = false)
public class InvoiceItem extends BaseEntity {
    
    /**
     * 关联发票ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    /**
     * 行号
     */
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    /**
     * 产品/服务代码
     */
    @Column(name = "item_code", length = 50)
    private String itemCode;
    
    /**
     * 产品/服务名称
     */
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    
    /**
     * 规格型号
     */
    @Column(name = "specification", length = 200)
    private String specification;
    
    /**
     * 计量单位
     */
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;
    
    /**
     * 数量
     */
    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;
    
    /**
     * 单价（不含税）
     */
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    
    /**
     * 税率（百分比）
     */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    /**
     * 税额
     */
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    /**
     * 金额（不含税）
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    
    /**
     * 折扣率（百分比）
     */
    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;
    
    /**
     * 折扣金额
     */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    /**
     * 备注
     */
    @Column(name = "notes", length = 500)
    private String notes;
    
    /**
     * 项目编号
     */
    @Column(name = "project_code", length = 50)
    private String projectCode;
    
    /**
     * 成本中心
     */
    @Column(name = "cost_center", length = 50)
    private String costCenter;
    
    /**
     * 计算行项总额（含税）
     */
    public BigDecimal calculateTotal() {
        BigDecimal netAmount = amount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        return netAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }
    
    @Override
    public boolean validate() {
        return lineNumber != null && itemName != null && !itemName.trim().isEmpty();
    }
    
    @Override
    public String getEntityType() {
        return "INVOICE_ITEM";
    }
    
    @Override
    public String getDisplayName() {
        return itemName;
    }
}