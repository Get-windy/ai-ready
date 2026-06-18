package cn.aiedge.erp.invoice.model.entity;

import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票付款记录实体类
 * 记录发票的付款明细
 */
@Entity
@Table(name = "invoice_payment")
@Data
@EqualsAndHashCode(callSuper = false)
public class InvoicePayment extends BaseEntity {
    
    /**
     * 关联发票ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    /**
     * 付款单号
     */
    @Column(name = "payment_number", unique = true, length = 50)
    private String paymentNumber;
    
    /**
     * 付款金额
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
    
    /**
     * 付款日期
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    /**
     * 付款方式 - BANK_TRANSFER / CASH / CHECK / CREDIT_CARD / ONLINE_PAYMENT
     */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;
    
    /**
     * 付款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    /**
     * 银行名称
     */
    @Column(name = "bank_name", length = 100)
    private String bankName;
    
    /**
     * 银行账号
     */
    @Column(name = "bank_account", length = 50)
    private String bankAccount;
    
    /**
     * 交易流水号
     */
    @Column(name = "transaction_number", length = 100)
    private String transactionNumber;
    
    /**
     * 付款人ID
     */
    @Column(name = "paid_by")
    private Long paidBy;
    
    /**
     * 付款人姓名
     */
    @Column(name = "paid_by_name", length = 100)
    private String paidByName;
    
    /**
     * 备注
     */
    @Column(name = "notes", length = 500)
    private String notes;
    
    /**
     * 关联的凭证ID
     */
    @Column(name = "voucher_id")
    private Long voucherId;
    
    /**
     * 获取付款状态显示名称
     */
    public String getStatusDisplay() {
        return status != null ? status.getChineseName() : "未知";
    }
    
    @Override
    public boolean validate() {
        return amount != null && paymentDate != null && status != null;
    }
    
    @Override
    public String getEntityType() {
        return "INVOICE_PAYMENT";
    }
    
    @Override
    public String getDisplayName() {
        return paymentNumber != null ? paymentNumber : String.format("Payment %.2f", amount);
    }
}