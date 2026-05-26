package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_bank_unmatched_item")
public class BankUnmatchedItem {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long reconciliationId;
    
    private Long accountId;
    
    private String itemType;
    
    private Long statementId;
    
    private Long transactionId;
    
    private LocalDate itemDate;
    
    private BigDecimal amount;
    
    private String summary;
    
    private String counterpartyName;
    
    private String counterpartyAccount;
    
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