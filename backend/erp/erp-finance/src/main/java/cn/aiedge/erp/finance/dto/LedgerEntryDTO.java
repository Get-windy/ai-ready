package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 分类账条目DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryDTO {
    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private BigDecimal openingDebit;
    private BigDecimal openingCredit;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    private BigDecimal closingDebit;
    private BigDecimal closingCredit;
    private BigDecimal closingBalance;
    private Integer balanceDirection;
    private String remark;
}
