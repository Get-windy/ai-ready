package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalanceVO {
    
    private Long id;
    
    private Long subjectId;
    
    private String subjectCode;
    
    private String subjectName;
    
    private String period;
    
    private BigDecimal initialDebit;
    
    private BigDecimal initialCredit;
    
    private BigDecimal periodDebit;
    
    private BigDecimal periodCredit;
    
    private BigDecimal yearDebit;
    
    private BigDecimal yearCredit;
    
    private BigDecimal endingDebit;
    
    private BigDecimal endingCredit;
    
    private BigDecimal endingBalance;
    
    private Integer balanceDirection;
}