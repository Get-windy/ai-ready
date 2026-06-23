package cn.aiedge.erp.purchase.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 比价分析记录实体
 */
@Data
@Entity
@Table(name = "purchase_quote_comparison")
public class PurchaseQuoteComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    @Column(name = "comparison_date", nullable = false)
    private LocalDateTime comparisonDate;

    @Column(name = "comparator_id", nullable = false)
    private Long comparatorId;

    @Column(name = "comparison_method", nullable = false, length = 20)
    private String comparisonMethod; // PRICE, TOTAL_SCORE, MANUAL

    @Column(name = "winning_quote_id")
    private Long winningQuoteId;

    @Column(name = "winning_supplier_id")
    private Long winningSupplierId;

    @Column(name = "winning_amount", precision = 18, scale = 2)
    private BigDecimal winningAmount;

    @Column(name = "savings_amount", precision = 18, scale = 2)
    private BigDecimal savingsAmount;

    @Column(name = "savings_rate", precision = 5, scale = 2)
    private BigDecimal savingsRate;

    @Column(name = "comparison_detail", columnDefinition = "TEXT")
    private String comparisonDetail;

    @Column(name = "price_analysis", columnDefinition = "TEXT")
    private String priceAnalysis;

    @Column(name = "quality_analysis", columnDefinition = "TEXT")
    private String qualityAnalysis;

    @Column(name = "service_analysis", columnDefinition = "TEXT")
    private String serviceAnalysis;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "decision_basis", columnDefinition = "TEXT")
    private String decisionBasis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.comparisonDate == null) {
            this.comparisonDate = LocalDateTime.now();
        }
    }
}