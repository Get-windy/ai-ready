package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.IFinancialReportService;
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
@RequestMapping("/api/finance/report")
@Tag(name = "财务报告管理", description = "财务报告相关操作接口")
public class FinancialReportController {

    private final IFinancialReportService financialReportService;

    public FinancialReportController(IFinancialReportService financialReportService) {
        this.financialReportService = financialReportService;
    }

    // 财务报表相关接口
    @PostMapping("/generate")
    @Operation(summary = "生成财务报表")
    public ApiResponse<Long> generateFinancialReport(@Valid @RequestBody FinancialReportCreateRequest request,
                                                    @RequestParam(required = false) Long tenantId) {
        Long id = financialReportService.generateFinancialReport(request, tenantId != null ? tenantId : 1L);
        return ApiResponse.success(id);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询财务报表")
    public ApiResponse<PageResult<FinancialReportVO>> pageFinancialReports(@RequestBody FinancialReportQueryRequest request) {
        Page<FinancialReportVO> pageResult = financialReportService.pageFinancialReports(request);
        PageResult<FinancialReportVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取财务报表详情")
    public ApiResponse<FinancialReportVO> getFinancialReportById(@PathVariable Long id) {
        FinancialReportVO financialReportVO = financialReportService.getFinancialReportById(id);
        return ApiResponse.success(financialReportVO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新财务报表")
    public ApiResponse<Void> updateFinancialReport(@PathVariable Long id,
                                                  @Valid @RequestBody FinancialReportCreateRequest request) {
        financialReportService.updateFinancialReport(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除财务报表")
    public ApiResponse<Void> deleteFinancialReport(@PathVariable Long id) {
        financialReportService.deleteFinancialReport(id);
        return ApiResponse.success();
    }

    // 审核相关接口
    @PostMapping("/{id}/audit")
    @Operation(summary = "审核财务报表")
    public ApiResponse<Void> auditFinancialReport(@PathVariable Long id,
                                                 @RequestParam String auditor) {
        financialReportService.auditFinancialReport(id, auditor);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "批准财务报表")
    public ApiResponse<Void> approveFinancialReport(@PathVariable Long id,
                                                   @RequestParam String approver) {
        financialReportService.approveFinancialReport(id, approver);
        return ApiResponse.success();
    }

    // 专门的报表生成接口
    @PostMapping("/balance-sheet/generate")
    @Operation(summary = "生成资产负债表报告")
    public ApiResponse<Long> generateBalanceSheetReport(@Valid @RequestBody FinancialReportCreateRequest request,
                                                       @RequestParam(required = false) Long tenantId) {
        Long id = financialReportService.generateBalanceSheetReport(request, tenantId != null ? tenantId : 1L);
        return ApiResponse.success(id);
    }

    @PostMapping("/profit-statement/generate")
    @Operation(summary = "生成利润表报告")
    public ApiResponse<Long> generateProfitStatementReport(@Valid @RequestBody FinancialReportCreateRequest request,
                                                          @RequestParam(required = false) Long tenantId) {
        Long id = financialReportService.generateProfitStatementReport(request, tenantId != null ? tenantId : 1L);
        return ApiResponse.success(id);
    }

    @PostMapping("/cash-flow-statement/generate")
    @Operation(summary = "生成现金流量表报告")
    public ApiResponse<Long> generateCashFlowStatementReport(@Valid @RequestBody FinancialReportCreateRequest request,
                                                            @RequestParam(required = false) Long tenantId) {
        Long id = financialReportService.generateCashFlowStatementReport(request, tenantId != null ? tenantId : 1L);
        return ApiResponse.success(id);
    }
}
