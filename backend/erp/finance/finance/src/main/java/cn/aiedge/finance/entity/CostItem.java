package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_cost_item")
public class CostItem {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String itemCode;
    
    private String itemName;
    
    private Integer costType;
    
    private Long costCenterId;
    
    private String costCenterCode;
    
    private String costCenterName;
    
    private String period;
    
    private LocalDate costDate;
    
    private BigDecimal amount;
    
    private BigDecimal allocatedAmount;
    
    private BigDecimal unallocatedAmount;
    
    private Integer allocationStatus;
    
    private Long sourceId;
    
    private String sourceType;
    
    private String sourceNo;
    
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