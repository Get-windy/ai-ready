package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.BalanceSheetDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceDTO;
import cn.aiedge.erp.finance.service.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 财务报表Controller (v2)
 */
@Tag(name = "财务报表(v2)", description = "试算平衡表、资产负债表、利润表、仪表盘KPI")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/report/v2")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    @Operation(summary = "试算平衡表")
    @GetMapping("/trial-balance")
    @PreAuthorize("hasPermission('/api/erp/finance/report/trial-balance', 'finance:report:view')")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询试算平衡表")
    public Result<List<TrialBalanceDTO>> trialBalance(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam Integer fiscalPeriod) {
        return Result.success(financialReportService.generateTrialBalance(fiscalYear, fiscalPeriod));
    }

    @Operation(summary = "资产负债表")
    @GetMapping("/balance-sheet")
    @PreAuthorize("hasPermission('/api/erp/finance/report/balance-sheet', 'finance:report:view')")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询资产负债表")
    public Result<List<BalanceSheetDTO>> balanceSheet(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam Integer fiscalPeriod) {
        return Result.success(financialReportService.generateBalanceSheet(fiscalYear, fiscalPeriod));
    }

    @Operation(summary = "利润表")
    @GetMapping("/income-statement")
    @PreAuthorize("hasPermission('/api/erp/finance/report/income-statement', 'finance:report:view')")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询利润表")
    public Result<List<IncomeStatementDTO>> incomeStatement(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计期间(默认等于开始月份)") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "开始月份") @RequestParam Integer startMonth,
            @Parameter(description = "结束月份") @RequestParam Integer endMonth) {
        if (fiscalPeriod == null) {
            fiscalPeriod = startMonth;
        }
        return Result.success(financialReportService.generateIncomeStatement(fiscalYear, fiscalPeriod, startMonth, endMonth));
    }

    @Operation(summary = "财务仪表盘KPI数据")
    @GetMapping("/dashboard")
    @PreAuthorize("hasPermission('/api/erp/finance/report/dashboard', 'finance:report:view')")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询财务仪表盘KPI")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(financialReportService.getDashboardKPIs());
    }
}
