package cn.aiedge.erp.finance.expensedoc.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsRowVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsSummaryVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsTrendVO;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 费用统计 Controller（只读报表）
 *
 * <p>数据源 = 《费用单》erp_expense_doc + erp_expense_item（P0 单一口径）；
 * 状态口径与《费用审批》状态机一致；本控制器不提供任何写入能力。</p>
 */
@RestController
@RequestMapping("/api/erp/finance/expense-stats")
@RequiredArgsConstructor
@Tag(name = "费用统计", description = "费用单申请/审批/记账口径的汇总分析与部门/类型维度统计（只读）")
public class ExpenseStatsController {

    private final ExpenseStatsService expenseStatsService;

    @Operation(summary = "统计汇总（卡片 + 按类型/部门分组映射 + 月度趋势）")
    @SaCheckPermission("finance:expense-stats:view")
    @GetMapping("/summary")
    public Result<ExpenseStatsSummaryVO> summary(ExpenseDocQuery query) {
        return Result.ok(expenseStatsService.getSummary(query));
    }

    @Operation(summary = "按部门分组统计明细（含占比/单均）")
    @SaCheckPermission("finance:expense-stats:view")
    @GetMapping("/by-department")
    public Result<List<ExpenseStatsRowVO>> byDepartment(ExpenseDocQuery query) {
        return Result.ok(expenseStatsService.listByDepartment(query));
    }

    @Operation(summary = "按费用类型分组统计明细（含占比/单均）")
    @SaCheckPermission("finance:expense-stats:view")
    @GetMapping("/by-type")
    public Result<List<ExpenseStatsRowVO>> byType(ExpenseDocQuery query) {
        return Result.ok(expenseStatsService.listByType(query));
    }

    @Operation(summary = "月度费用趋势")
    @SaCheckPermission("finance:expense-stats:view")
    @GetMapping("/monthly-trend")
    public Result<List<ExpenseStatsTrendVO>> monthlyTrend(ExpenseDocQuery query) {
        return Result.ok(expenseStatsService.listMonthlyTrend(query));
    }
}
