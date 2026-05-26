package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankReconciliationVO {
    
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
    
    private String statusName;
    
    private Boolean isBalanced;
}