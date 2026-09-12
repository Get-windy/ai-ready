package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 科目余额表查询条件
 *
 * 对应页面「科目余额表」查询区 6 字段：查询方案（前端本地保存）/ 会计月(起) /
 * 会计月(止) / 科目层级 / 科目 / 显示无数据科目。
 */
@Data
public class TrialBalanceQuery {

    /** 会计年度（默认取止月所属年度） */
    private Integer fiscalYear;

    /** 会计月(起) 1-12，默认与止月相同 */
    private Integer startPeriod;

    /** 会计月(止) 1-12，默认当前月 */
    private Integer endPeriod;

    /** 科目层级：显示到第几级（默认 1，仅显示一级科目；null/0 = 全部层级） */
    private Integer subjectLevel;

    /** 科目：科目编码或科目名称模糊匹配 */
    private String subjectKeyword;

    /** 显示无数据科目（默认 false，仅显示有发生额/余额的科目） */
    private Boolean showNoData = false;
}
