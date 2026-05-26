package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_bank_statement")
public class BankStatement {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long accountId;
    
    private String accountName;
    
    private String bankName;
    
    private String bankAccount;
    
    private String statementNo;
    
    private LocalDate transactionDate;
    
    private Integer transactionType;
    
    private BigDecimal amount;
    
    private BigDecimal balance;
    
    private String counterpartyName;
    
    private String counterpartyAccount;
    
    private String counterpartyBank;
    
    private String summary;
    
    private String remark;
    
    private Integer status;
    
    private Long matchedTransactionId;
    
    private LocalDate matchedDate;
    
    private Long matchedBy;
    
    private String matchedRemark;
    
    private String sourceFile;
    
    private Integer importBatch;
    
    private Integer rowNo;
    
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