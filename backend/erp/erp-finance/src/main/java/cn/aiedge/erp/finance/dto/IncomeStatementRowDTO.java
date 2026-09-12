package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 利润表项目行
 *
 * 报表项目行非固定模板：由公司实际启用的成本类/损益类科目按「科目层级」展开生成，
 * 一/二/三/四 段汇总行由服务端按国内报表惯例插入（加粗）。
 */
@Data
public class IncomeStatementRowDTO {

    /** 行号（从 1 开始） */
    private Integer rowNo;

    /** 项目编码：科目行 = 科目编码；汇总行 = SUMMARY_REVENUE/SUMMARY_OPERATING_PROFIT/... */
    private String itemCode;

    /** 项目名称（含「加：/减：」前缀，汇总行含段序号） */
    private String itemName;

    /** 缩进层级：0 = 汇总行/一级科目，1 = 二级科目，2 = 三级科目 */
    private Integer indentLevel = 0;

    /** 是否汇总行（一/二/三/四 段，加粗显示） */
    private Boolean summaryRow = Boolean.FALSE;

    /** 科目ID（穿透明细账用，汇总行为空） */
    private Long subjectId;

    /** 科目编码（穿透明细账用，汇总行为空） */
    private String subjectCode;

    /** 科目层级（1-4，汇总行 = 0） */
    private Integer subjectLevel;

    /** 本期发生额 */
    private BigDecimal currentAmount = BigDecimal.ZERO;

    /** 本年累计（年初至止月） */
    private BigDecimal cumulativeAmount = BigDecimal.ZERO;
}
