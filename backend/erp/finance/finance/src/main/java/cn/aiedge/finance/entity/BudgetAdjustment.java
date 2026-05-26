package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_budget_adjustment")
public class BudgetAdjustment {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String adjustmentNo;
    
    private Long budgetId;
    
    private String budgetCode;
    
    private Long budgetItemId;
    
    private String budgetItemName;
    
    private LocalDate adjustmentDate;
    
    private BigDecimal beforeAmount;
    
    private BigDecimal afterAmount;
    
    private BigDecimal adjustAmount;
    
    private Integer adjustType;
    
    private String reason;
    
    private Integer status;
    
    private Long preparedBy;
    
    private String preparedByName;
    
    private LocalDateTime preparedTime;
    
    private Long approvedBy;
    
    private String approvedByName;
    
    private LocalDateTime approvedTime;
    
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