package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_bank_reconciliation")
public class BankReconciliation {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long accountId;
    
    private String accountName;
    
    private String bankName;
    
    private String bankAccount;
    
    private String period;
    
    private LocalDate reconciliationDate;
    
    private BigDecimal bankBalance;
    
    private BigDecimal bookBalance;
    
    private BigDecimal difference;
    
    private BigDecimal depositInTransit;
    
    private BigDecimal outstandingChecks;
    
    private BigDecimal bankErrors;
    
    private BigDecimal bookErrors;
    
    private BigDecimal adjustedBankBalance;
    
    private BigDecimal adjustedBookBalance;
    
    private Integer status;
    
    private Long preparedBy;
    
    private LocalDateTime preparedTime;
    
    private Long reviewedBy;
    
    private LocalDateTime reviewedTime;
    
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