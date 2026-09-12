package cn.aiedge.erp.finance.expensedoc.service;

import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsRowVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsSummaryVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsTrendVO;

import java.util.List;

/**
 * 费用统计 Service（只读）
 *
 * <p>P0 单一口径：复用《费用单》（erp_expense_doc + erp_expense_item）作为唯一数据源，
 * 不另建统计表；状态口径与《费用审批》状态机一致。</p>
 */
public interface ExpenseStatsService {

    /** 统计汇总（卡片 + 按类型/部门分组 + 月度趋势） */
    ExpenseStatsSummaryVO getSummary(ExpenseDocQuery query);

    /** 按部门分组明细（金额降序，含占比） */
    List<ExpenseStatsRowVO> listByDepartment(ExpenseDocQuery query);

    /** 按费用类型（费用名称/科目）分组明细（金额降序，含占比） */
    List<ExpenseStatsRowVO> listByType(ExpenseDocQuery query);

    /** 月度趋势（按单据日期月份升序） */
    List<ExpenseStatsTrendVO> listMonthlyTrend(ExpenseDocQuery query);
}
