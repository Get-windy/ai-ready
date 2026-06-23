package cn.aiedge.erp.invoice.model.entity;

import cn.aiedge.erp.invoice.model.enums.TaxType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 发票税项明细实体类
 * 表示发票中的税务明细信息
 */
@Entity
@Table(name = "invoice_tax")
@Data
@EqualsAndHashCode(callSuper = false)
public class InvoiceTax extends BaseEntity {
    
    /**
     * 关联发票ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    /**
     * 税项类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;
    
    /**
     * 税率（百分比）
     */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    /**
     * 计税基数（不含税金额）
     */
    @Column(name = "tax_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxBase = BigDecimal.ZERO;
    
    /**
     * 税额
     */
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    /**
     * 税目名称
     */
    @Column(name = "tax_name", length = 100)
    private String taxName;
    
    /**
     * 税目代码
     */
    @Column(name = "tax_code", length = 50)
    private String taxCode;
    
    /**
     * 是否可抵扣
     */
    @Column(name = "is_deductible")
    private Boolean isDeductible = false;
    
    /**
     * 抵扣状态 - DEDUCTED / PENDING / EXPIRED
     */
    @Column(name = "deduct_status", length = 20)
    private String deductStatus;
    
    /**
     * 获取税项类型显示名称
     */
    public String getTaxTypeDisplay() {
        return taxType != null ? taxType.getChineseName() : "未知";
    }
    
    @Override
    public boolean validate() {
        return taxType != null && taxRate != null && taxBase != null && taxAmount != null;
    }
    
    @Override
    public String getEntityType() {
        return "INVOICE_TAX";
    }
    
    @Override
    public String getDisplayName() {
        return taxName != null ? taxName : taxType != null ? taxType.getChineseName() : "Tax";
    }
}