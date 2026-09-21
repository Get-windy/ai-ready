package cn.aiedge.erp.expense.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.expense.analytics.service.ExpenseAnalyticsService;
import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 查费用（分析 → 财务分析 → 查费用，菜单 80454）。
 *
 * <p>四视图（按部门 / 按职员 / 按明细 / 按往来单位）账表口径。
 * 与既有 {@code /api/erp/expense/statistics/page|summary|by-department|by-type}
 * （费用统计：总额 / 审批状态金额）<b>互不影响</b>，仅新增路径。</p>
 *
 * <p><b>鉴权（2026-09-21 补，E-02 批次 4）：</b>三个端点原先只有 {@code @SaCheckLogin}，
 * 任何登录用户（含别的租户、别的模块角色）都能读费用账表。现统一挂
 * {@code erp:expense:statistics:list} —— 与《费用统计》页
 * （{@code ExpenseController#/statistics/page}）同一个"看费用账表"码，不另造新码。
 * ⚠️ 遗留：分析模块（{@code /views/analytics/**}）整体还没有自己的权限码family，
 * 那是一次独立的模块级设计，登记在 MASTER_TODO 里。</p>
 */
@Tag(name = "查费用（分析模块）")
@RestController
@RequestMapping("/api/erp/expense/statistics")
@RequiredArgsConstructor
public class ExpenseAnalyticsController {

    private final ExpenseAnalyticsService expenseAnalyticsService;

    @Operation(summary = "按部门 / 按职员费用矩阵（tab=dept|staff）",
            description = "行 = 费用科目（费用名称/费用编号/费用金额），列 = 部门/职员动态矩阵（矩阵列不进列配置弹窗）")
    @GetMapping("/matrix")
    @SaCheckLogin
    @SaCheckPermission("erp:expense:statistics:list")
    public ApiResponse<Map<String, Object>> matrix(AnalyticsQuery query) {
        return ApiResponse.ok(expenseAnalyticsService.matrix(query));
    }

    @Operation(summary = "按明细费用流水（15 列）",
            description = "单据日期/单据编号/单据类型/往来单位/往来单位编号/科目/科目编号/金额/经手人/部门/制单人/摘要链/记账时间")
    @GetMapping("/detail")
    @SaCheckLogin
    @SaCheckPermission("erp:expense:statistics:list")
    public ApiResponse<Map<String, Object>> detail(AnalyticsQuery query) {
        return ApiResponse.ok(expenseAnalyticsService.detail(query));
    }

    @Operation(summary = "按往来单位费用归集（4 列）", description = "单位编号/单位名称/费用金额/占比(%)，占比分母 = 本视图费用总额")
    @GetMapping("/partner")
    @SaCheckLogin
    @SaCheckPermission("erp:expense:statistics:list")
    public ApiResponse<Map<String, Object>> partner(AnalyticsQuery query) {
        return ApiResponse.ok(expenseAnalyticsService.partner(query));
    }
}
