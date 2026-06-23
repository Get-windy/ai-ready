package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 试算平衡表DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceDTO {
    private String subjectCode;
    private String subjectName;
    private BigDecimal openingDebit;
    private BigDecimal openingCredit;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    private BigDecimal closingDebit;
    private BigDecimal closingCredit;
}
