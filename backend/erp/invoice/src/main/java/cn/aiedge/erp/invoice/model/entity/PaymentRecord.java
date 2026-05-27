package cn.aiedge.erp.invoice.model.entity;

import cn.aiedge.erp.invoice.model.enums.MatchingType;
import cn.aiedge.erp.invoice.model.enums.PaymentMethod;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录实体类
 * 存储付款信息，用于与发票进行匹配
 */
@Entity
@Table(name = "payment_record")
@Data
@EqualsAndHashCode(callSuper = false)
public class PaymentRecord extends BaseEntity {
    
    /**
     * 付款参考号
     */
    @Column(name = "payment_reference", nullable = false, unique = true, length = 50)
    private String paymentReference;
    
    /**
     * 关联的发票ID
     */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;
    
    /**
     * 付款金额
     */
    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;
    
    /**
     * 匹配金额 - 本次匹配的金额
     */
    @Column(name = "matched_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal matchedAmount;
    
    /**
     * 付款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;
    
    /**
     * 付款方式
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    /**
     * 匹配类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "matching_type", nullable = false, length = 20)
    private MatchingType matchingType;
    
    /**
     * 付款日期
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
    
    /**
     * 匹配日期
     */
    @Column(name = "matched_date")
    private LocalDate matchedDate;
    
    /**
     * 匹配时间
     */
    @Column(name = "matched_at")
    private LocalDateTime matchedAt;
    
    /**
     * 匹配人ID
     */
    @Column(name = "matched_by")
    private Long matchedBy;
    
    /**
     * 匹配人姓名
     */
    @Column(name = "matched_by_name", length = 100)
    private String matchedByName;
    
    /**
     * 银行交易号
     */
    @Column(name = "bank_transaction_id", length = 100)
    private String bankTransactionId;
    
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
     * 付款方账号
     */
    @Column(name = "payer_account", length = 50)
    private String payerAccount;
    
    /**
     * 收款方账号
     */
    @Column(name = "payee_account", length = 50)
    private String payeeAccount;
    
    /**
     * 货币代码
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "CNY";
    
    /**
     * 汇率
     */
    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;
    
    /**
     * 实际付款金额（原币）
     */
    @Column(name = "original_amount", precision = 15, scale = 2)
    private BigDecimal originalAmount;
    
    /**
     * 实际付款货币
     */
    @Column(name = "original_currency", length = 3)
    private String originalCurrency;
    
    /**
     * 匹配说明
     */
    @Column(name = "matching_notes", columnDefinition = "TEXT")
    private String matchingNotes;
    
    /**
     * 验证码/验证信息
     */
    @Column(name = "verification_code", length = 100)
    private String verificationCode;
    
    /**
     * 附件路径
     */
    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;
    
    /**
     * 是否已核对
     */
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    /**
     * 核对人ID
     */
    @Column(name = "verified_by")
    private Long verifiedBy;
    
    /**
     * 核对时间
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * 是否已记账
     */
    @Column(name = "posted", nullable = false)
    private Boolean posted = false;
    
    /**
     * 记账凭证号
     */
    @Column(name = "voucher_number", length = 50)
    private String voucherNumber;
    
    /**
     * 记账时间
     */
    @Column(name = "posted_at")
    private LocalDateTime postedAt;
    
    /**
     * 记账人ID
     */
    @Column(name = "posted_by")
    private Long postedBy;
    
    /**
     * 关联的退款ID
     */
    @Column(name = "refund_id")
    private Long refundId;
    
    /**
     * 是否冲红记录
     */
    @Column(name = "is_credit_note", nullable = false)
    private Boolean isCreditNote = false;
    
    /**
     * 冲红原因
     */
    @Column(name = "credit_reason", length = 500)
    private String creditReason;
    
    /**
     * 匹配来源系统
     */
    @Column(name = "source_system", length = 50)
    private String sourceSystem;
    
    /**
     * 匹配来源系统ID
     */
    @Column(name = "source_system_id", length = 100)
    private String sourceSystemId;
    
    /**
     * 批量匹配批次号
     */
    @Column(name = "batch_number", length = 50)
    private String batchNumber;
    
    /**
     * 自定义字段1
     */
    @Column(name = "custom_field_1", length = 200)
    private String customField1;
    
    /**
     * 自定义字段2
     */
    @Column(name = "custom_field_2", length = 200)
    private String customField2;
    
    /**
     * 自定义字段3
     */
    @Column(name = "custom_field_3", length = 200)
    private String customField3;
    
    /**
     * 构造函数
     */
    public PaymentRecord() {
        this.paymentDate = LocalDate.now();
        this.matchedDate = LocalDate.now();
        this.matchedAt = LocalDateTime.now();
        this.matchedAmount = BigDecimal.ZERO;
    }
    
    /**
     * 创建付款记录
     */
    public static PaymentRecord createForInvoice(Invoice invoice, BigDecimal paymentAmount, 
                                                PaymentMethod paymentMethod, String paymentReference) {
        PaymentRecord record = new PaymentRecord();
        record.setInvoiceId(invoice.getId());
        record.setInvoiceId(invoice.getId());
        record.setPaymentAmount(paymentAmount);
        record.setPaymentMethod(paymentMethod);
        record.setPaymentReference(paymentReference);
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setMatchingType(MatchingType.MANUAL);
        record.setCurrencyCode(invoice.getCurrencyCode());
        return record;
    }
    
    /**
     * 标记为已匹配
     */
    public void markAsMatched(Long matchedBy, String matchedByName, MatchingType matchingType, BigDecimal matchedAmount) {
        this.matchedBy = matchedBy;
        this.matchedByName = matchedByName;
        this.matchingType = matchingType;
        this.matchedAmount = matchedAmount;
        this.matchedDate = LocalDate.now();
        this.matchedAt = LocalDateTime.now();
        
        if (matchedAmount.compareTo(paymentAmount) >= 0) {
            this.paymentStatus = PaymentStatus.MATCHED;
        } else if (matchedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = PaymentStatus.PARTIALLY_MATCHED;
        }
    }
    
    /**
     * 取消匹配
     */
    public void cancelMatching(String reason) {
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.matchingNotes = (this.matchingNotes != null ? this.matchingNotes + "\n" : "") + 
                             "取消匹配: " + reason;
        setDeleted(true);
        setDeletedAt(LocalDateTime.now());
    }
    
    /**
     * 标记为已核对
     */
    public void markAsVerified(Long verifiedBy) {
        this.verified = true;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为已记账
     */
    public void markAsPosted(String voucherNumber, Long postedBy) {
        this.posted = true;
        this.voucherNumber = voucherNumber;
        this.postedBy = postedBy;
        this.postedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否为完全匹配
     */
    public boolean isFullyMatched() {
        return matchedAmount != null && matchedAmount.compareTo(paymentAmount) == 0;
    }
    
    /**
     * 检查是否为部分匹配
     */
    public boolean isPartiallyMatched() {
        return matchedAmount != null && 
               matchedAmount.compareTo(BigDecimal.ZERO) > 0 && 
               matchedAmount.compareTo(paymentAmount) < 0;
    }
    
    /**
     * 获取未匹配金额
     */
    public BigDecimal getUnmatchedAmount() {
        if (paymentAmount == null || matchedAmount == null) {
            return paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        }
        return paymentAmount.subtract(matchedAmount).max(BigDecimal.ZERO);
    }
    
    /**
     * 检查是否可匹配
     */
    public boolean canMatch() {
        return !isDeleted() && 
               !posted && 
               paymentStatus != PaymentStatus.CANCELLED && 
               paymentStatus != PaymentStatus.REFUNDED && 
               paymentDate != null && 
               paymentAmount != null && 
               paymentAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return paymentReference + " - " + paymentAmount + " " + currencyCode + 
               (matchedAmount != null ? " (已匹配: " + matchedAmount + ")" : "");
    }
    
    @Override
    public boolean validate() {
        if (paymentReference == null || paymentReference.trim().isEmpty()) {
            return false;
        }
        if (invoiceId == null) {
            return false;
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (paymentDate == null) {
            return false;
        }
        if (paymentMethod == null) {
            return false;
        }
        if (paymentStatus == null) {
            return false;
        }
        return true;
    }
    
    @Override
    public String getEntityType() {
        return "PAYMENT_RECORD";
    }
}