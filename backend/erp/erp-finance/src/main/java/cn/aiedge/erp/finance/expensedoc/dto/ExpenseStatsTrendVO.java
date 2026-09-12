package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用统计月度趋势行 VO
 * 按单据日期所在月份（yyyy-MM）汇总费用金额与单据数。
 */
@Data
public class ExpenseStatsTrendVO {

    /** 月份 yyyy-MM */
    private String month;

    /** 单据数 */
    private Integer docCount;

    /** 费用金额 */
    private BigDecimal totalAmount;

    /** 已审批金额 */
    private BigDecimal approvedAmount;

    /** 已记账金额（实际支付口径） */
    private BigDecimal paidAmount;
}
