package cn.aiedge.erp.purchase.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报价明细实体
 */
@Data
@Entity
@Table(name = "purchase_quote_item")
public class PurchaseQuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_id", nullable = false)
    private Long quoteId;

    @Column(name = "inquiry_item_id", nullable = false)
    private Long inquiryItemId;

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName;

    @Column(columnDefinition = "TEXT")
    private String specification;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.valueOf(13.00);

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(length = 200)
    private String brand;

    @Column(length = 200)
    private String model;

    @Column(name = "quality_level", length = 20)
    private String qualityLevel;

    @Column(name = "origin_country", length = 50)
    private String originCountry;

    @Column(name = "lead_time")
    private Integer leadTime;

    @Column(name = "delivery_location", length = 200)
    private String deliveryLocation;

    @Column(name = "item_note", columnDefinition = "TEXT")
    private String itemNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        // 计算金额和税额
        if (this.quantity != null && this.unitPrice != null) {
            this.amount = this.quantity.multiply(this.unitPrice);
            if (this.taxRate != null) {
                this.taxAmount = this.amount.multiply(this.taxRate).divide(BigDecimal.valueOf(100));
            }
        }
    }
}