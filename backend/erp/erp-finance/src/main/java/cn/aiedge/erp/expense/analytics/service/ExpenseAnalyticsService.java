package cn.aiedge.erp.expense.analytics.service;

import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;

import java.util.Map;

/**
 * 查费用（分析 → 财务分析 → 查费用，菜单 80454）· 四视图聚合。
 *
 * <p>口径与既有 {@code /api/erp/expense/statistics/*}（费用统计：总额 / 审批状态金额 / 按部门 / 按类型）
 * <b>互不影响</b>：本服务按对标的四视图出账表口径——按部门（部门矩阵）、按职员（职员矩阵）、
 * 按明细（单据级 15 列）、按往来单位（4 列 + 占比）。</p>
 */
public interface ExpenseAnalyticsService {

    /** 按部门 / 按职员矩阵（tab=dept|staff）：行 = 费用科目，列 = 部门/职员矩阵（矩阵列不进列配置） */
    Map<String, Object> matrix(AnalyticsQuery query);

    /** 按明细：单据级费用流水（15 列） */
    Map<String, Object> detail(AnalyticsQuery query);

    /** 按往来单位：单位费用金额 + 占比（4 列） */
    Map<String, Object> partner(AnalyticsQuery query);
}
