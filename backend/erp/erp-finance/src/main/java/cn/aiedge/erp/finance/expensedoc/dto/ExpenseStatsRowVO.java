package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用统计分组行 VO（按部门 / 按费用类型两个维度共用）
 *
 * <p>P1 口径：金额均取「费用项金额」合计；费用单 P1 守恒（本单金额 = Σ费用项金额），
 * 故分组金额之和与汇总「费用总额」一致。</p>
 */
@Data
public class ExpenseStatsRowVO {

    /** 分组键（部门名称 / 费用名称） */
    private String groupKey;

    /** 分组编码（费用编号，部门维度为空） */
    private String groupCode;

    /** 费用科目名称（费用类型维度） */
    private String subjectName;

    /** 费用科目编码 */
    private String subjectCode;

    /** 单据数（去重） */
    private Integer docCount;

    /** 费用笔数（费用项条数） */
    private Integer itemCount;

    /** 费用金额 */
    private BigDecimal totalAmount;

    /** 已审批金额（审批状态=2） */
    private BigDecimal approvedAmount;

    /** 待审批金额（审批状态=1） */
    private BigDecimal pendingAmount;

    /** 已驳回金额（审批状态=3） */
    private BigDecimal rejectedAmount;

    /** 已记账金额（实际支付口径） */
    private BigDecimal paidAmount;

    /** 单均金额 */
    private BigDecimal avgAmount;

    /** 费用占比（%，金额 / 费用总额） */
    private BigDecimal ratio;

    /** 费用类型 0-往来单位费用 1-内部费用（费用类型维度） */
    private Integer expenseType;
}
