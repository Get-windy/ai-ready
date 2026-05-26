package cn.aiedge.erp.expense.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用明细项实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_item")
public class ExpenseItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_application_id", nullable = false)
    private ExpenseApplication expenseApplication;
    
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;
    
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "unit", length = 20)
    private String unit;
    
    @Column(name = "vendor_name", length = 200)
    private String vendorName;
    
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount_with_tax", precision = 15, scale = 2)
    private BigDecimal totalAmountWithTax;
    
    @Column(name = "has_invoice")
    private Boolean hasInvoice = false;
    
    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;
    
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;
    
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;
    
    @Column(name = "account_code", length = 50)
    private String accountCode;
    
    @Column(name = "budget_code", length = 50)
    private String budgetCode;
    
    @Column(name = "project_code", length = 50)
    private String projectCode;
    
    @Column(name = "cost_center", length = 50)
    private String costCenter;
    
    @Column(name = "is_personal")
    private Boolean isPersonal = false;
    
    @Column(name = "is_reimbursable")
    private Boolean isReimbursable = true;
    
    @Column(name = "receipt_required")
    private Boolean receiptRequired = true;
    
    @Column(name = "receipt_attached")
    private Boolean receiptAttached = false;
    
    @Column(name = "attachment_id", length = 100)
    private String attachmentId;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "verified_by", length = 50)
    private String verifiedBy;
    
    @Column(name = "verified_date")
    private LocalDate verifiedDate;
    
    @Column(name = "verification_comment", length = 500)
    private String verificationComment;
    
    @Column(name = "sequence_number")
    private Integer sequenceNumber = 0;
    
    @PrePersist
    @PreUpdate
    public void calculateAmounts() {
        // 计算含税金额
        if (amount != null && taxRate != null) {
            this.taxAmount = amount.multiply(taxRate).divide(BigDecimal.valueOf(100));
            this.totalAmountWithTax = amount.add(taxAmount);
        } else {
            this.taxAmount = BigDecimal.ZERO;
            this.totalAmountWithTax = amount != null ? amount : BigDecimal.ZERO;
        }
        
        // 计算单价
        if (amount != null && quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.unitPrice = amount.divide(quantity, 2, BigDecimal.ROUND_HALF_UP);
        }
    }
}