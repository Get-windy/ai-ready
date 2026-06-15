package cn.aiedge.api.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 预算管理占位 Controller
 * 预算模块使用 JPA，主应用禁用了 JPA，故通过占位控制器返回空数据避免前端 404
 */
@Tag(name = "预算管理（占位）")
@RestController
@RequestMapping("/api/erp/budget")
public class BudgetStubController {

    @Operation(summary = "预算模板分页（占位）")
    @GetMapping("/template/page")
    public Result<Map<String, Object>> templatePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "年度预算分页（占位）")
    @GetMapping("/annual/page")
    public Result<Map<String, Object>> annualPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "预算调整分页（占位）")
    @GetMapping("/adjustment/page")
    public Result<Map<String, Object>> adjustmentPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(Map.of("records", Collections.emptyList(), "total", 0L, "current", pageNum, "size", pageSize, "pages", 0L));
    }

    @Operation(summary = "预算执行汇总报表（占位）")
    @GetMapping("/report/execution-summary")
    public Result<Map<String, Object>> executionSummary() {
        return Result.ok(Map.of(
            "totalBudget", 0, "totalExecuted", 0, "executionRate", 0,
            "byDepartment", Collections.emptyList()
        ));
    }

    @Operation(summary = "部门预算汇总报表（占位）")
    @GetMapping("/report/department-summary")
    public Result<List<?>> departmentSummary() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "科目预算汇总（占位）")
    @GetMapping("/report/subject-summary")
    public Result<List<?>> subjectSummary() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "预算趋势（占位）")
    @GetMapping("/report/trend")
    public Result<List<?>> trend() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "预算差异分析（占位）")
    @GetMapping("/report/variance-analysis")
    public Result<Map<String, Object>> varianceAnalysis() {
        return Result.ok(Map.of(
            "favorableVariance", 0, "adverseVariance", 0,
            "totalVariance", 0, "details", Collections.emptyList()
        ));
    }
}
