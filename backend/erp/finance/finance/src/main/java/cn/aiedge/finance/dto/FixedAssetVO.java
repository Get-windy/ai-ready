package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class FixedAssetVO {
    
    private Long id;
    
    private String assetCode;
    
    private String assetName;
    
    private Long categoryId;
    
    private String categoryName;
    
    private String specification;
    
    private String unit;
    
    private Integer quantity;
    
    private BigDecimal originalValue;
    
    private BigDecimal accumulatedDepreciation;
    
    private BigDecimal netValue;
    
    private BigDecimal residualValue;
    
    private BigDecimal depreciationRate;
    
    private Integer depreciationMethod;
    
    private String depreciationMethodName;
    
    private Integer usefulLife;
    
    private Integer usedMonths;
    
    private Integer remainingMonths;
    
    private LocalDate acquisitionDate;
    
    private LocalDate startDepreciationDate;
    
    private Integer status;
    
    private String statusName;
    
    private Long departmentId;
    
    private String departmentName;
    
    private Long locationId;
    
    private String locationName;
    
    private Long custodianId;
    
    private String custodianName;
    
    private String manufacturer;
    
    private String brand;
    
    private String model;
    
    private String serialNo;
    
    private Long supplierId;
    
    private String supplierName;
    
    private String invoiceNo;
    
    private String remark;
    
    private List<AssetChangeVO> changes;
}