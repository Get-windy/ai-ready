package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankStatementImportDTO {
    
    private Long accountId;
    
    private String bankName;
    
    private String bankAccount;
    
    private LocalDate transactionDate;
    
    private Integer transactionType;
    
    private BigDecimal amount;
    
    private BigDecimal balance;
    
    private String counterpartyName;
    
    private String counterpartyAccount;
    
    private String counterpartyBank;
    
    private String summary;
    
    private String remark;
}