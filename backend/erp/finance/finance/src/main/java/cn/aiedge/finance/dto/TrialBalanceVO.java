package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class TrialBalanceVO {
    
    private BigDecimal totalInitialDebit;
    
    private BigDecimal totalInitialCredit;
    
    private BigDecimal initialBalanceDiff;
    
    private BigDecimal totalPeriodDebit;
    
    private BigDecimal totalPeriodCredit;
    
    private BigDecimal periodBalanceDiff;
    
    private BigDecimal totalEndingDebit;
    
    private BigDecimal totalEndingCredit;
    
    private BigDecimal endingBalanceDiff;
    
    private Boolean isBalanced;
    
    private Map<String, BigDecimal> details;
}