package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_cost_allocation")
public class CostAllocation {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String allocationNo;
    
    private String period;
    
    private LocalDate allocationDate;
    
    private Long fromCenterId;
    
    private String fromCenterCode;
    
    private String fromCenterName;
    
    private Long toCenterId;
    
    private String toCenterCode;
    
    private String toCenterName;
    
    private Long costItemId;
    
    private String costItemCode;
    
    private String costItemName;
    
    private Integer costType;
    
    private BigDecimal originalAmount;
    
    private BigDecimal allocationBase;
    
    private BigDecimal allocationRate;
    
    private BigDecimal allocatedAmount;
    
    private Integer allocationMethod;
    
    private Integer status;
    
    private Long voucherId;
    
    private String voucherNo;
    
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