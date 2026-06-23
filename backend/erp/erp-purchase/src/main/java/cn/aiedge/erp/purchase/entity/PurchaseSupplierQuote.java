package cn.aiedge.erp.purchase.entity;

import cn.aiedge.erp.purchase.enums.QuoteStatus;
import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商报价单实体
 */
@Data
@Entity
@Table(name = "purchase_supplier_quote")
public class PurchaseSupplierQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_no", unique = true, nullable = false, length = 32)
    private String quoteNo;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_code", length = 50)
    private String supplierCode;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_status", nullable = false, length = 20)
    private QuoteStatus quoteStatus = QuoteStatus.SUBMITTED;
    
    /**
     * 为测试代码提供兼容方法
     */
    public QuoteStatus getStatus() {
        return quoteStatus;
    }

    @Column(name = "quote_date", nullable = false)
    private LocalDateTime quoteDate;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.valueOf(13.00);

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "delivery_terms", columnDefinition = "TEXT")
    private String deliveryTerms;

    @Column(name = "warranty_terms", columnDefinition = "TEXT")
    private String warrantyTerms;

    @Column(name = "supplier_note", columnDefinition = "TEXT")
    private String supplierNote;

    @Column(name = "competitive_advantage", columnDefinition = "TEXT")
    private String competitiveAdvantage;

    @Column(name = "attachment_urls", columnDefinition = "TEXT")
    private String attachmentUrls;

    @Column(name = "price_score", precision = 5, scale = 2)
    private BigDecimal priceScore;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "service_score", precision = 5, scale = 2)
    private BigDecimal serviceScore;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "is_recommended")
    private Boolean isRecommended = false;

    @Column(name = "recommend_reason", columnDefinition = "TEXT")
    private String recommendReason;

    @Column(name = "review_status", length = 20)
    private String reviewStatus;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.quoteStatus == null) {
            this.quoteStatus = QuoteStatus.SUBMITTED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}