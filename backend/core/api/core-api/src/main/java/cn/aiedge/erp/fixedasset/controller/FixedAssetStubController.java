package cn.aiedge.erp.fixedasset.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 固定资产占位 Controller
 * 全模块尚未完整实现，返回空数据避免前端 404
 */
@Tag(name = "固定资产（占位）")
@RestController
@RequestMapping("/api/erp/fixed-asset")
public class FixedAssetStubController {

    @Operation(summary = "资产分页（占位）")
    @GetMapping("/asset/page")
    public Result<Map<String, Object>> assetPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "资产统计（占位）")
    @GetMapping("/asset/statistics")
    public Result<Map<String, Object>> assetStatistics() {
        return Result.ok(Map.of("total", 0, "inUse", 0, "idle", 0, "scrapped", 0));
    }

    @Operation(summary = "分类列表（占位）")
    @GetMapping("/category/list")
    public Result<List<?>> categoryList() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "分类树（占位）")
    @GetMapping("/category/tree")
    public Result<List<?>> categoryTree() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "折旧分页（占位）")
    @GetMapping("/depreciation/page")
    public Result<Map<String, Object>> depreciationPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "调拨分页（占位）")
    @GetMapping("/transfer/page")
    public Result<Map<String, Object>> transferPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "处置分页（占位）")
    @GetMapping("/disposal/page")
    public Result<Map<String, Object>> disposalPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    // ========== 采购端点（占位） ==========

    @Operation(summary = "采购分页（占位）")
    @GetMapping("/purchase/page")
    public Result<Map<String, Object>> purchasePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "盘点分页（占位）")
    @GetMapping("/inventory/page")
    public Result<Map<String, Object>> inventoryPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    // ========== 报表端点（占位） ==========

    @Operation(summary = "折旧汇总报表（占位）")
    @GetMapping("/report/depreciation-summary")
    public Result<Map<String, Object>> depreciationSummary() {
        return Result.ok(Map.of(
            "totalDepreciation", 0,
            "monthlyDepreciation", 0,
            "assetsCount", 0,
            "byCategory", Collections.emptyList()
        ));
    }

    @Operation(summary = "分类汇总报表（占位）")
    @GetMapping("/report/category-summary")
    public Result<List<?>> categorySummary() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "年限分析报表（占位）")
    @GetMapping("/report/age-analysis")
    public Result<Map<String, Object>> ageAnalysis() {
        return Result.ok(Map.of(
            "newAssets", 0,
            "midAssets", 0,
            "oldAssets", 0,
            "totalValue", 0,
            "byAgeRange", Collections.emptyList()
        ));
    }
}
