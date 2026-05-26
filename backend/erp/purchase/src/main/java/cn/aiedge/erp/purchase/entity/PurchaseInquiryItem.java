package cn.aiedge.erp.purchase.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 询价物料明细实体
 */
@Data
@Entity
@Table(name = "purchase_inquiry_item")
public class PurchaseInquiryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    @Column(name = "item_seq", nullable = false)
    private Integer itemSeq;

    @Column(name = "material_id")
    private Long materialId;

    @Column(name = "material_code", length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 200)
    private String materialName;

    @Column(columnDefinition = "TEXT")
    private String specification;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "min_quantity", precision = 18, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "quality_requirement", columnDefinition = "TEXT")
    private String qualityRequirement;

    @Column(name = "delivery_requirement", columnDefinition = "TEXT")
    private String deliveryRequirement;

    @Column(name = "brand_requirement", length = 200)
    private String brandRequirement;

    @Column(name = "estimated_price", precision = 18, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "estimated_amount", precision = 18, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        // 计算预估金额
        if (this.quantity != null && this.estimatedPrice != null) {
            this.estimatedAmount = this.quantity.multiply(this.estimatedPrice);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        // 重新计算预估金额
        if (this.quantity != null && this.estimatedPrice != null) {
            this.estimatedAmount = this.quantity.multiply(this.estimatedPrice);
        }
    }
}