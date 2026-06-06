package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.erp.fixedasset.dto.ApiResponse;
import cn.aiedge.erp.fixedasset.service.FixedAssetReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 固定资产报表控制器
 */
@RestController
@RequestMapping("/api/erp/fixed-asset/report")
@Tag(name = "固定资产报表", description = "固定资产统计报表、台账、账龄分析等")
@RequiredArgsConstructor
public class FixedAssetReportController {

    private final FixedAssetReportService reportService;

    @Operation(summary = "折旧汇总")
    @GetMapping("/depreciation-summary")
    public ApiResponse<List<Map<String, Object>>> getDepreciationSummary(
            @Parameter(description = "年份") @RequestParam(required = false) String year) {
        List<Map<String, Object>> result = reportService.getDepreciationSummary(year);
        return ApiResponse.success(result);
    }

    @Operation(summary = "资产台账")
    @GetMapping("/asset-ledger")
    public ApiResponse<List<Map<String, Object>>> getAssetLedger(
            @Parameter(description = "资产编码") @RequestParam(required = false) String assetCode,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId) {
        List<Map<String, Object>> result = reportService.getAssetLedger(assetCode, departmentId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "账龄分析")
    @GetMapping("/age-analysis")
    public ApiResponse<List<Map<String, Object>>> getAgeAnalysis() {
        List<Map<String, Object>> result = reportService.getAgeAnalysis();
        return ApiResponse.success(result);
    }

    @Operation(summary = "分类汇总")
    @GetMapping("/category-summary")
    public ApiResponse<List<Map<String, Object>>> getCategorySummary() {
        List<Map<String, Object>> result = reportService.getCategorySummary();
        return ApiResponse.success(result);
    }
}
