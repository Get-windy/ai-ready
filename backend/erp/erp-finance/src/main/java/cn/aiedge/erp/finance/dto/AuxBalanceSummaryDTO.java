package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅助核算余额表合计行（表尾，取查询区间全量口径，非当前页）
 */
@Data
public class AuxBalanceSummaryDTO {

    /** 期初余额-借方 */
    private BigDecimal beginDebit = BigDecimal.ZERO;

    /** 期初余额-贷方 */
    private BigDecimal beginCredit = BigDecimal.ZERO;

    /** 本期发生额-借方 */
    private BigDecimal periodDebit = BigDecimal.ZERO;

    /** 本期发生额-贷方 */
    private BigDecimal periodCredit = BigDecimal.ZERO;

    /** 本年累计-借方 */
    private BigDecimal yearDebit = BigDecimal.ZERO;

    /** 本年累计-贷方 */
    private BigDecimal yearCredit = BigDecimal.ZERO;

    /** 期末余额-借方 */
    private BigDecimal endDebit = BigDecimal.ZERO;

    /** 期末余额-贷方 */
    private BigDecimal endCredit = BigDecimal.ZERO;
}
