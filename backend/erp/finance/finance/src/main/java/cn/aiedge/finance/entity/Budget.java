package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_budget")
public class Budget {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String budgetCode;
    
    private String budgetName;
    
    private Integer budgetType;
    
    private String period;
    
    private Integer year;
    
    private Integer quarter;
    
    private Integer month;
    
    private Long departmentId;
    
    private String departmentName;
    
    private Long projectId;
    
    private String projectName;
    
    private BigDecimal totalAmount;
    
    private BigDecimal usedAmount;
    
    private BigDecimal remainingAmount;
    
    private BigDecimal usedRate;
    
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
    
    @TableField(exist = false)
    private java.util.List<BudgetItem> items;
}