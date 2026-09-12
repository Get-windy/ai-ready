package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 总账查询参数
 */
@Data
public class GeneralLedgerQueryDTO {

    /** 会计月(起) YYYY-MM，缺省取会计月(止) */
    private String periodStart;

    /** 会计月(止) YYYY-MM，缺省取会计月(起) */
    private String periodEnd;

    /** 科目层级：汇总到第 N 级，默认 1（一级科目） */
    private Integer subjectLevel;

    /** 无发生额不显示本期合计 */
    private Boolean hideNoAmount;

    /** 显示本年累计 */
    private Boolean showYearAccum;

    /** 显示当前总计 */
    private Boolean showCurrentTotal;

    /** 科目编码/名称 模糊过滤 */
    private String subjectCode;
}
