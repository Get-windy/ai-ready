package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅助核算余额表数据行（对标 12 列）
 *
 * 四段余额：期初余额、本期发生额、本年累计、期末余额，各含借方/贷方两方向。
 * 勾稽公式：期初净额 + 本期借 − 本期贷 = 期末净额。
 */
@Data
public class AuxBalanceRowDTO {

    /** 科目ID（用于下钻明细账） */
    private Long subjectId;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 核算项类型编码 */
    private String auxType;

    /** 核算项编码 */
    private String auxCode;

    /** 核算项名称 */
    private String auxName;

    /** 期初余额-借方 */
    private BigDecimal beginDebit;

    /** 期初余额-贷方 */
    private BigDecimal beginCredit;

    /** 本期发生额-借方 */
    private BigDecimal periodDebit;

    /** 本期发生额-贷方 */
    private BigDecimal periodCredit;

    /** 本年累计-借方 */
    private BigDecimal yearDebit;

    /** 本年累计-贷方 */
    private BigDecimal yearCredit;

    /** 期末余额-借方 */
    private BigDecimal endDebit;

    /** 期末余额-贷方 */
    private BigDecimal endCredit;
}
