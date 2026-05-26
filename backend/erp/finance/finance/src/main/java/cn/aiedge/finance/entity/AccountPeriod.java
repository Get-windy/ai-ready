package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_account_period")
public class AccountPeriod {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String periodCode;
    
    private Integer year;
    
    private Integer month;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Integer status;
    
    private Integer isCurrent;
    
    private LocalDate closedDate;
    
    private Long closedBy;
    
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