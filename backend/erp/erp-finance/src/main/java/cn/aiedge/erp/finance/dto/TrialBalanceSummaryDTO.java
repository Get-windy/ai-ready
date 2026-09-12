package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目余额表合计行
 *
 * 试算平衡红线（P0）：Σ本期借方 = Σ本期贷方、Σ本年累计借方 = Σ本年累计贷方。
 * 合计口径 = 全部「顶级科目」（父级未在结果集中出现）汇总，避免父子科目重复累加。
 */
@Data
public class TrialBalanceSummaryDTO {

    /** 期初借方余额合计（正余额科目） */
    private BigDecimal openingDebit;

    /** 期初贷方余额合计（负余额科目取绝对值） */
    private BigDecimal openingCredit;

    /** 本期借方发生额合计 */
    private BigDecimal periodDebit;

    /** 本期贷方发生额合计 */
    private BigDecimal periodCredit;

    /** 本年累计借方合计 */
    private BigDecimal yearDebit;

    /** 本年累计贷方合计 */
    private BigDecimal yearCredit;

    /** 期末借方余额合计 */
    private BigDecimal closingDebit;

    /** 期末贷方余额合计 */
    private BigDecimal closingCredit;

    /** 本期发生额是否试算平衡 */
    private Boolean periodBalanced;

    /** 本年累计是否试算平衡 */
    private Boolean yearBalanced;
}
