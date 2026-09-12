package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目余额表数据行
 *
 * 四段口径（开发文档 P1）：期初余额（会计月起时点）/ 本期发生额（起止月间借贷）/
 * 本年累计（年初至止月）/ 期末余额（止月时点）。
 * 余额字段为净额（借正贷负），方向由 direction 字段显式给出。
 */
@Data
public class TrialBalanceRowDTO {

    /** 科目类型 1-资产 2-负债 3-权益 4-成本 5-损益 */
    private Integer subjectType;

    /** 科目类型名称（资产类/负债类/权益类/成本类/损益类） */
    private String subjectTypeName;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 层级 */
    private Integer level;

    /** 期初余额（借正贷负，含下级汇总） */
    private BigDecimal openingBalance;

    /** 期初余额方向 借/贷 */
    private String openingDirection;

    /** 本期发生额-借方 */
    private BigDecimal periodDebit;

    /** 本期发生额-贷方 */
    private BigDecimal periodCredit;

    /** 本年累计-借方 */
    private BigDecimal yearDebit;

    /** 本年累计-贷方 */
    private BigDecimal yearCredit;

    /** 期末余额（借正贷负，含下级汇总） */
    private BigDecimal closingBalance;

    /** 期末余额方向 借/贷 */
    private String closingDirection;

    /** 是否有下级科目（前端可据此提示汇总口径） */
    private Boolean hasChildren;
}
