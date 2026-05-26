package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.IFinancialStatementService;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 财务报表控制器
 */
@RestController
@RequestMapping("/api/finance/statement")
@Tag(name = "财务报表管理", description = "财务报表相关操作接口")
public class FinancialStatementController {

    private final IFinancialStatementService financialStatementService;

    public FinancialStatementController(IFinancialStatementService financialStatementService) {
        this.financialStatementService = financialStatementService;
    }

    // 资产负债表相关接口
    @PostMapping("/balance-sheet/generate")
    @Operation(summary = "生成资产负债表")
    public ApiResponse<Long> generateBalanceSheet(@RequestParam String period, @RequestParam Long tenantId) {
        Long id = financialStatementService.generateBalanceSheet(period, tenantId);
        return ApiResponse.success(id);
    }

    @PostMapping("/balance-sheet/list")
    @Operation(summary = "分页查询资产负债表")
    public ApiResponse<PageResult<BalanceSheetVO>> pageBalanceSheets(@RequestBody BalanceSheetQueryRequest request) {
        Page<BalanceSheetVO> pageResult = financialStatementService.pageBalanceSheets(request);
        PageResult<BalanceSheetVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/balance-sheet/{id}")
    @Operation(summary = "根据ID获取资产负债表详情")
    public ApiResponse<BalanceSheetVO> getBalanceSheetById(@PathVariable Long id) {
        BalanceSheetVO balanceSheetVO = financialStatementService.getBalanceSheetById(id);
        return ApiResponse.success(balanceSheetVO);
    }

    @DeleteMapping("/balance-sheet/{id}")
    @Operation(summary = "删除资产负债表")
    public ApiResponse<Void> deleteBalanceSheet(@PathVariable Long id) {
        financialStatementService.deleteBalanceSheet(id);
        return ApiResponse.success();
    }

    // 利润表相关接口
    @PostMapping("/profit-statement/generate")
    @Operation(summary = "生成利润表")
    public ApiResponse<Long> generateProfitStatement(@RequestParam String period, @RequestParam Long tenantId) {
        Long id = financialStatementService.generateProfitStatement(period, tenantId);
        return ApiResponse.success(id);
    }

    @PostMapping("/profit-statement/list")
    @Operation(summary = "分页查询利润表")
    public ApiResponse<PageResult<ProfitStatementVO>> pageProfitStatements(@RequestBody ProfitStatementQueryRequest request) {
        Page<ProfitStatementVO> pageResult = financialStatementService.pageProfitStatements(request);
        PageResult<ProfitStatementVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/profit-statement/{id}")
    @Operation(summary = "根据ID获取利润表详情")
    public ApiResponse<ProfitStatementVO> getProfitStatementById(@PathVariable Long id) {
        ProfitStatementVO profitStatementVO = financialStatementService.getProfitStatementById(id);
        return ApiResponse.success(profitStatementVO);
    }

    @DeleteMapping("/profit-statement/{id}")
    @Operation(summary = "删除利润表")
    public ApiResponse<Void> deleteProfitStatement(@PathVariable Long id) {
        financialStatementService.deleteProfitStatement(id);
        return ApiResponse.success();
    }

    // 现金流量表相关接口
    @PostMapping("/cash-flow-statement/generate")
    @Operation(summary = "生成现金流量表")
    public ApiResponse<Long> generateCashFlowStatement(@RequestParam String period, @RequestParam Long tenantId) {
        Long id = financialStatementService.generateCashFlowStatement(period, tenantId);
        return ApiResponse.success(id);
    }

    @PostMapping("/cash-flow-statement/list")
    @Operation(summary = "分页查询现金流量表")
    public ApiResponse<PageResult<CashFlowStatementVO>> pageCashFlowStatements(@RequestBody CashFlowStatementQueryRequest request) {
        Page<CashFlowStatementVO> pageResult = financialStatementService.pageCashFlowStatements(request);
        PageResult<CashFlowStatementVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/cash-flow-statement/{id}")
    @Operation(summary = "根据ID获取现金流量表详情")
    public ApiResponse<CashFlowStatementVO> getCashFlowStatementById(@PathVariable Long id) {
        CashFlowStatementVO cashFlowStatementVO = financialStatementService.getCashFlowStatementById(id);
        return ApiResponse.success(cashFlowStatementVO);
    }

    @DeleteMapping("/cash-flow-statement/{id}")
    @Operation(summary = "删除现金流量表")
    public ApiResponse<Void> deleteCashFlowStatement(@PathVariable Long id) {
        financialStatementService.deleteCashFlowStatement(id);
        return ApiResponse.success();
    }

    // 通用报表操作接口
    @PostMapping("/audit")
    @Operation(summary = "审核报表")
    public ApiResponse<Void> auditReport(@RequestParam String reportType, 
                                 @RequestParam Long id, 
                                 @RequestParam String auditor) {
        financialStatementService.auditReport(reportType, id, auditor);
        return ApiResponse.success();
    }

    @PostMapping("/batch-generate")
    @Operation(summary = "批量生成报表")
    public ApiResponse<Void> batchGenerateReports(@RequestParam String period, @RequestParam Long tenantId) {
        financialStatementService.batchGenerateReports(period, tenantId);
        return ApiResponse.success();
    }
}
