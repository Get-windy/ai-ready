package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset", indexes = {
    @Index(name = "idx_fixed_asset_code", columnList = "asset_code", unique = true),
    @Index(name = "idx_fixed_asset_status", columnList = "status"),
    @Index(name = "idx_fixed_asset_category_id", columnList = "category_id"),
    @Index(name = "idx_fixed_asset_department_id", columnList = "department_id"),
    @Index(name = "idx_fixed_asset_custodian_id", columnList = "custodian_id")
})
public class FixedAsset extends BaseEntity {

    @Column(name = "asset_code", nullable = false, unique = true, length = 50)
    private String assetCode;

    @Column(name = "asset_name", nullable = false, length = 200)
    private String assetName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "original_value", precision = 15, scale = 2)
    private BigDecimal originalValue;

    @Column(name = "net_value", precision = 15, scale = 2)
    private BigDecimal netValue;

    @Column(name = "depreciation_method", length = 30)
    private String depreciationMethod;

    @Column(name = "useful_life")
    private Integer usefulLife;

    @Column(name = "salvage_value", precision = 15, scale = 2)
    private BigDecimal salvageValue;

    @Column(name = "salvage_rate", precision = 5, scale = 2)
    private BigDecimal salvageRate;

    @Column(name = "monthly_depreciation", precision = 15, scale = 2)
    private BigDecimal monthlyDepreciation;

    @Column(name = "accumulated_depreciation", precision = 15, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "draft";

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "custodian_id", length = 50)
    private String custodianId;

    @Column(name = "custodian_name", length = 100)
    private String custodianName;

    @Column(name = "specification", length = 200)
    private String specification;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "use_status", length = 20)
    private String useStatus;

    @Column(name = "asset_photo", length = 500)
    private String assetPhoto;
}
