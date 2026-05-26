package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_account_balance")
public class AccountBalance {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long subjectId;
    
    private String subjectCode;
    
    private String period;
    
    private BigDecimal initialDebit;
    
    private BigDecimal initialCredit;
    
    private BigDecimal periodDebit;
    
    private BigDecimal periodCredit;
    
    private BigDecimal yearDebit;
    
    private BigDecimal yearCredit;
    
    private BigDecimal endingDebit;
    
    private BigDecimal endingCredit;
    
    private Long auxiliaryId;
    
    private Integer auxiliaryType;
    
    private String auxiliaryValue;
    
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