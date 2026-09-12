package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用统计单据级聚合结果（单行）
 * 汇总卡片的数据来源；全部金额均为费用单「本单金额」口径。
 */
@Data
public class ExpenseStatsAggVO {

    /** 单据数 */
    private Integer expenseCount;

    /** 费用笔数（费用项条数） */
    private Integer itemCount;

    /** 费用总额 */
    private BigDecimal totalAmount;

    /** 已审批金额（审批通过） */
    private BigDecimal approvedAmount;

    /** 待审批金额（审批中） */
    private BigDecimal pendingAmount;

    /** 已驳回金额（审批驳回） */
    private BigDecimal rejectedAmount;

    /** 未提交金额 */
    private BigDecimal draftAmount;

    /** 已记账金额（支付口径） */
    private BigDecimal paidAmount;

    /** 未记账金额（待支付口径） */
    private BigDecimal unpaidAmount;

    /** 往来单位费用金额 */
    private BigDecimal partnerAmount;

    /** 内部费用金额 */
    private BigDecimal internalAmount;
}
