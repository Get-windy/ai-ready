package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FixedAssetCreateDTO {
    
    @NotBlank(message = "资产名称不能为空")
    private String assetName;
    
    private String assetCode;
    
    @NotNull(message = "资产分类不能为空")
    private Long categoryId;
    
    private String specification;
    
    private String unit;
    
    private Integer quantity;
    
    @NotNull(message = "原值不能为空")
    private BigDecimal originalValue;
    
    private BigDecimal residualValue;
    
    @NotNull(message = "折旧方法不能为空")
    private Integer depreciationMethod;
    
    @NotNull(message = "使用年限不能为空")
    private Integer usefulLife;
    
    @NotNull(message = "购置日期不能为空")
    private LocalDate acquisitionDate;
    
    private LocalDate startDepreciationDate;
    
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
}