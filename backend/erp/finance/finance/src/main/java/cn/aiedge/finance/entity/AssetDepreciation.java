package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_asset_depreciation")
public class AssetDepreciation {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long assetId;
    
    private String assetCode;
    
    private String assetName;
    
    private String period;
    
    private LocalDate depreciationDate;
    
    private BigDecimal originalValue;
    
    private BigDecimal accumulatedDepreciation;
    
    private BigDecimal periodDepreciation;
    
    private BigDecimal netValue;
    
    private BigDecimal depreciationRate;
    
    private Integer usedMonths;
    
    private Integer remainingMonths;
    
    private Long voucherId;
    
    private String voucherNo;
    
    private Integer status;
    
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