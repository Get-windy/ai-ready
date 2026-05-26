package com.aiready.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账簿记录DTO
 */
@Data
public class LedgerDTO {
    
    private Long id;
    
    /**
     * 账簿类型（1：明细账 2：总账）
     */
    private Integer ledgerType;
    
    /**
     * 账簿类型描述
     */
    private String ledgerTypeDesc;
    
    /**
     * 科目ID
     */
    private Long subjectId;
    
    /**
     * 科目编码
     */
    private String subjectCode;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
    /**
     * 会计期间
     */
    private String accountingPeriod;
    
    /**
     * 业务日期
     */
    private LocalDate businessDate;
    
    /**
     * 凭证ID
     */
    private Long voucherId;
    
    /**
     * 凭证字号
     */
    private String voucherNo;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 借方金额
     */
    private BigDecimal debitAmount;
    
    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;
    
    /**
     * 余额
     */
    private BigDecimal balance;
    
    /**
     * 借贷方向（1：借 2：贷）
     */
    private Integer balanceDirection;
    
    /**
     * 借贷方向描述
     */
    private String balanceDirectionDesc;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
