package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_cost_allocation_rule")
public class CostAllocationRule {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String ruleCode;
    
    private String ruleName;
    
    private Long fromCenterId;
    
    private String fromCenterCode;
    
    private String fromCenterName;
    
    private Integer allocationMethod;
    
    private String allocationBase;
    
    private BigDecimal allocationRate;
    
    private Integer priority;
    
    private Integer enabled;
    
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