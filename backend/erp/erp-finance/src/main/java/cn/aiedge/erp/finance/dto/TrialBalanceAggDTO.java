package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目余额表取数中间结果：按科目编码聚合的四段金额
 */
@Data
public class TrialBalanceAggDTO {

    /** 科目编码 */
    private String subjectCode;

    /** 期初净额（借正贷负）：会计月起之前的分录净额 + 年初结转余额 */
    private BigDecimal openingNet;

    /** 本期借方发生额 */
    private BigDecimal periodDebit;

    /** 本期贷方发生额 */
    private BigDecimal periodCredit;

    /** 本年累计借方 */
    private BigDecimal yearDebit;

    /** 本年累计贷方 */
    private BigDecimal yearCredit;
}
