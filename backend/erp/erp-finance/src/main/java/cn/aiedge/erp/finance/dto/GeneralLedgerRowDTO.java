package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 总账行DTO（账簿行结构：期初余额 / 本期合计 / 本年累计 / 当前总计）
 * <p>
 * 对应页面 8 列：科目编码 / 科目名称 / 期间 / 摘要 / 借方 / 贷方 / 方向 / 余额。
 */
@Data
public class GeneralLedgerRowDTO {

    /** 科目ID（汇总行为主科目ID） */
    private Long subjectId;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 期间（YYYY-MM） */
    private String period;

    /** 摘要：期初余额 / 本期合计 / 本期发生 / 本年累计 / 当前总计 */
    private String summary;

    /** 借方发生额（期初/累计行可能为空） */
    private BigDecimal debit;

    /** 贷方发生额 */
    private BigDecimal credit;

    /** 方向：借 / 贷 / 平 */
    private String direction;

    /** 余额（绝对值） */
    private BigDecimal balance;

    /** 行类型：opening-期初 period-本期发生 yearTotal-本年累计 grandTotal-当前总计 */
    private String rowType;

    /** 科目层级 */
    private Integer level;

    /** 是否可下钻明细账 */
    private Boolean drillable;
}
