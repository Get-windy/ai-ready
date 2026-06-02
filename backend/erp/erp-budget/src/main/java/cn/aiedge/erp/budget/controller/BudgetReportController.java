package cn.aiedge.erp.budget.controller;

import cn.aiedge.erp.budget.dto.ApiResponse;
import cn.aiedge.erp.budget.dto.BudgetStatisticsDTO;
import cn.aiedge.erp.budget.service.BudgetReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/budget/report")
@Tag(name = "预算报表", description = "预算报表与统计分析")
@RequiredArgsConstructor
public class BudgetReportController {

    private final BudgetReportService budgetReportService;

    @Operation(summary = "执行概览")
    @GetMapping("/execution-summary")
    public ApiResponse<BudgetStatisticsDTO> executionSummary(
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear) {
        if (fiscalYear == null) {
            fiscalYear = java.time.Year.now().getValue();
        }
        BudgetStatisticsDTO result = budgetReportService.getExecutionSummary(fiscalYear);
        return ApiResponse.success(result);
    }

    @Operation(summary = "部门预算汇总")
    @GetMapping("/department-summary")
    public ApiResponse<List<Map<String, Object>>> departmentSummary(
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear) {
        if (fiscalYear == null) {
            fiscalYear = java.time.Year.now().getValue();
        }
        List<Map<String, Object>> result = budgetReportService.getDepartmentSummary(fiscalYear);
        return ApiResponse.success(result);
    }

    @Operation(summary = "科目预算汇总")
    @GetMapping("/subject-summary")
    public ApiResponse<List<Map<String, Object>>> subjectSummary(
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "预算ID") @RequestParam(required = false) Long budgetId) {
        if (fiscalYear == null) {
            fiscalYear = java.time.Year.now().getValue();
        }
        List<Map<String, Object>> result = budgetReportService.getSubjectSummary(fiscalYear, budgetId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "差异分析")
    @GetMapping("/variance-analysis")
    public ApiResponse<List<Map<String, Object>>> varianceAnalysis(
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear) {
        if (fiscalYear == null) {
            fiscalYear = java.time.Year.now().getValue();
        }
        List<Map<String, Object>> result = budgetReportService.getVarianceAnalysis(fiscalYear);
        return ApiResponse.success(result);
    }

    @Operation(summary = "趋势数据")
    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
            @Parameter(description = "财政年度") @RequestParam(required = false) Integer fiscalYear) {
        if (fiscalYear == null) {
            fiscalYear = java.time.Year.now().getValue();
        }
        List<Map<String, Object>> result = budgetReportService.getTrend(fiscalYear);
        return ApiResponse.success(result);
    }
}
