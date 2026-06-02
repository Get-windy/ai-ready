package cn.aiedge.erp.fixedasset.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产DTO
 */
@Data
public class FixedAssetDTO {
    private Long id;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String categoryName;
    private LocalDate purchaseDate;
    private BigDecimal originalValue;
    private BigDecimal netValue;
    private String depreciationMethod;
    private Integer usefulLife;
    private BigDecimal salvageValue;
    private BigDecimal salvageRate;
    private BigDecimal monthlyDepreciation;
    private BigDecimal accumulatedDepreciation;
    private String status;
    private String location;
    private String departmentId;
    private String departmentName;
    private String custodianId;
    private String custodianName;
    private String specification;
    private String brand;
    private String supplierName;
    private String invoiceNo;
    private LocalDate warrantyEndDate;
    private String description;
    private String useStatus;
    private String assetPhoto;
    private String remark;
    private String createdBy;
    private String updatedBy;
}
