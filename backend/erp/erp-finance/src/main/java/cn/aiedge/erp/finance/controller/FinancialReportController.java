package cn.aiedge.erp.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.BalanceSheetDTO;
import cn.aiedge.erp.finance.dto.BalanceSheetQuery;
import cn.aiedge.erp.finance.dto.BalanceSheetReportDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementQuery;
import cn.aiedge.erp.finance.dto.IncomeStatementReportDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceDTO;
import cn.aiedge.erp.finance.dto.TrialBalancePageDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceQuery;
import cn.aiedge.erp.finance.service.FinancialReportService;
import cn.aiedge.erp.finance.service.TrialBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final TrialBalanceService trialBalanceService;

    @Operation(summary = "科目余额表（四段余额试算平衡表）")
    @GetMapping("/trial-balance-page")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询科目余额表")
    public Result<TrialBalancePageDTO> trialBalancePage(TrialBalanceQuery query) {
        return Result.success(trialBalanceService.queryTrialBalance(query));
    }

    @Operation(summary = "试算平衡表")
    @GetMapping("/trial-balance")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询试算平衡表")
    public Result<List<TrialBalanceDTO>> trialBalance(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam Integer fiscalPeriod) {
        return Result.success(financialReportService.generateTrialBalance(fiscalYear, fiscalPeriod));
    }

    @Operation(summary = "资产负债表(左右对照)")
    @GetMapping("/balance-sheet-report")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询资产负债表")
    public Result<BalanceSheetReportDTO> balanceSheetReport(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计月(止)") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "期间模式 single/multi") @RequestParam(required = false, defaultValue = "single") String periodMode,
            @Parameter(description = "多会计月起止月") @RequestParam(required = false) Integer startPeriod,
            @Parameter(description = "科目层级") @RequestParam(required = false, defaultValue = "1") Integer subjectLevel,
            @Parameter(description = "显示为0科目") @RequestParam(required = false, defaultValue = "true") Boolean showZero) {
        BalanceSheetQuery query = new BalanceSheetQuery();
        query.setFiscalYear(fiscalYear);
        query.setFiscalPeriod(fiscalPeriod);
        query.setPeriodMode(periodMode);
        query.setStartPeriod(startPeriod);
        query.setSubjectLevel(subjectLevel);
        query.setShowZero(showZero);
        return Result.success(financialReportService.generateBalanceSheetReport(query));
    }

    @Operation(summary = "资产负债表(扁平列表，兼容旧报表页)")
    @GetMapping("/balance-sheet")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询资产负债表")
    public Result<List<BalanceSheetDTO>> balanceSheet(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam Integer fiscalPeriod) {
        return Result.success(financialReportService.generateBalanceSheet(fiscalYear, fiscalPeriod));
    }

    @Operation(summary = "利润表(科目层级驱动)")
    @GetMapping("/income-statement-report")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询利润表")
    public Result<IncomeStatementReportDTO> incomeStatementReport(
            @Parameter(description = "会计年度") @RequestParam Integer fiscalYear,
            @Parameter(description = "会计月(止)") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "期间模式 single/multi") @RequestParam(required = false, defaultValue = "single") String periodMode,
            @Parameter(description = "多会计月起止月") @RequestParam(required = false) Integer startPeriod,
            @Parameter(description = "科目层级") @RequestParam(required = false, defaultValue = "2") Integer subjectLevel,
            @Parameter(description = "显示为0科目") @RequestParam(required = false, defaultValue = "false") Boolean showZero) {
        IncomeStatementQuery query = new IncomeStatementQuery();
        query.setFiscalYear(fiscalYear);
        query.setFiscalPeriod(fiscalPeriod);
        query.setPeriodMode(periodMode);
        query.setStartPeriod(startPeriod);
        query.setSubjectLevel(subjectLevel);
        query.setShowZero(showZero);
        return Result.success(financialReportService.generateIncomeStatementReport(query));
    }

    @Operation(summary = "利润表(扁平列表，兼容旧报表页)")
    @GetMapping("/income-statement")
    @SaCheckPermission("finance:report:view")
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
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "财务报表", type = "QUERY", desc = "查询财务仪表盘KPI")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(financialReportService.getDashboardKPIs());
    }
}
