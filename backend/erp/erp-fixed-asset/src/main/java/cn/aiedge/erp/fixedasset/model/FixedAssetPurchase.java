package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产购置申请
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset_purchase", indexes = {
    @Index(name = "idx_fap_purchase_no", columnList = "purchase_no", unique = true),
    @Index(name = "idx_fap_status", columnList = "status"),
    @Index(name = "idx_fap_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_fap_department_id", columnList = "department_id")
})
public class FixedAssetPurchase extends BaseEntity {

    @Column(name = "purchase_no", nullable = false, unique = true, length = 50)
    private String purchaseNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "applicant_id", length = 50)
    private String applicantId;

    @Column(name = "applicant_name", length = 100)
    private String applicantName;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "asset_name", length = 200)
    private String assetName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "estimated_amount", precision = 15, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "actual_amount", precision = 15, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "apply_date")
    private LocalDate applyDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "purchase_reason", length = 500)
    private String purchaseReason;

    @Column(name = "status", length = 30)
    private String status = "draft";

    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    @Column(name = "generated_asset_id")
    private Long generatedAssetId;

    @Column(name = "specification", length = 200)
    private String specification;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "remark", length = 500)
    private String remark;
}
