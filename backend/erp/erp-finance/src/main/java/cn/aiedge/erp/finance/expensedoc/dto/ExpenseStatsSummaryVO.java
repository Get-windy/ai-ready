package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用统计汇总 VO
 *
 * <p>P0 单一口径：数据源唯一为《费用单》erp_expense_doc + erp_expense_item，
 * 状态口径与《费用审批》状态机一致（approval_status 0未提交/1审批中/2审批通过/3审批驳回）。</p>
 *
 * <p>两套金额口径（P1 与支付/报销区分）：
 * 申请口径 = 全部非取消费用单的本单金额；支付口径 = 已记账（status=1，即已生成凭证并动账户）。</p>
 */
@Data
public class ExpenseStatsSummaryVO {

    /** 费用总额（申请口径） */
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 已审批金额（审批通过） */
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    /** 待审批金额（审批中） */
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    /** 已拒绝金额（审批驳回） */
    private BigDecimal rejectedAmount = BigDecimal.ZERO;

    /** 未提交金额（未提交审批） */
    private BigDecimal draftAmount = BigDecimal.ZERO;

    /** 已记账金额（支付口径，已生成凭证 + 动账户） */
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** 未记账金额（待支付口径） */
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    /** 往来单位费用金额 */
    private BigDecimal partnerAmount = BigDecimal.ZERO;

    /** 内部费用金额 */
    private BigDecimal internalAmount = BigDecimal.ZERO;

    /** 单据数 */
    private Integer expenseCount = 0;

    /** 费用笔数（费用项条数） */
    private Integer itemCount = 0;

    /** 平均单额 = 费用总额 / 单据数 */
    private BigDecimal averageAmount = BigDecimal.ZERO;

    /** 按费用类型（费用名称 → 金额） */
    private Map<String, BigDecimal> byType = new LinkedHashMap<>();

    /** 按部门（部门名称 → 金额） */
    private Map<String, BigDecimal> byDepartment = new LinkedHashMap<>();

    /** 月度趋势 */
    private List<ExpenseStatsTrendVO> monthlyTrend = new ArrayList<>();
}
