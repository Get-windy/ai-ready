package cn.aiedge.erp.purchase.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同关联实体（报价转合同）
 */
@Data
@Entity
@Table(name = "purchase_quote_contract")
public class PurchaseQuoteContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_id", nullable = false)
    private Long quoteId;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "conversion_date", nullable = false)
    private LocalDateTime conversionDate;

    @Column(name = "converter_id", nullable = false)
    private Long converterId;

    @Column(name = "contract_no", length = 50)
    private String contractNo;

    @Column(name = "contract_amount", precision = 18, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "contract_status", length = 20)
    private String contractStatus;

    @Column(name = "fulfillment_status", length = 20)
    private String fulfillmentStatus = "NOT_STARTED";

    @Column(name = "fulfillment_progress", precision = 5, scale = 2)
    private BigDecimal fulfillmentProgress = BigDecimal.ZERO;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "performance_date")
    private LocalDateTime performanceDate;

    @Column(name = "performance_comment", columnDefinition = "TEXT")
    private String performanceComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.conversionDate == null) {
            this.conversionDate = LocalDateTime.now();
        }
        if (this.fulfillmentStatus == null) {
            this.fulfillmentStatus = "NOT_STARTED";
        }
        if (this.fulfillmentProgress == null) {
            this.fulfillmentProgress = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}