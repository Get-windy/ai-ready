package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_fixed_asset")
public class FixedAsset {
    
    @TableId(type = IdType.ASSIGN_ID)
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
    
    private Integer usefulLife;
    
    private Integer usedMonths;
    
    private LocalDate acquisitionDate;
    
    private LocalDate startDepreciationDate;
    
    private Integer status;
    
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
    
    private Long voucherId;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    private Long tenantId;
    
    @Version
    private Integer version;
}