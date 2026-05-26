package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_budget_item")
public class BudgetItem {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long budgetId;
    
    private String budgetCode;
    
    private Integer itemType;
    
    private Long subjectId;
    
    private String subjectCode;
    
    private String subjectName;
    
    private Long departmentId;
    
    private String departmentName;
    
    private Long projectId;
    
    private String projectName;
    
    private BigDecimal budgetAmount;
    
    private BigDecimal usedAmount;
    
    private BigDecimal remainingAmount;
    
    private BigDecimal usedRate;
    
    private BigDecimal controlRate;
    
    private Integer controlLevel;
    
    private Integer alertThreshold;
    
    private Integer alertFlag;
    
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