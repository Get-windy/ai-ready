package cn.aiedge.erp.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 业务指标占位Controller
 * <p>
 * 当 erp-metrics 模块不可用时提供降级响应。
 * 前端已包含mock数据兜底，此Controller仅确保端点不返回404。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/metrics")
@Tag(name = "业务指标（降级）", description = "erp-metrics不可用时的空响应占位")
public class MetricsFallbackController {

    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘指标（降级-空数据）")
    public Result<Map<String, Object>> getDashboardMetrics() {
        log.debug("[MetricsFallback] dashboard called, returning empty data");
        return Result.ok(Map.of(
                "totalRevenue", 0,
                "totalOrders", 0,
                "totalCustomers", 0,
                "totalProducts", 0,
                "revenueChange", 0,
                "orderChange", 0,
                "customerChange", 0,
                "recentOrders", List.of(),
                "topProducts", List.of()
        ));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "按类型获取指标（降级-空列表）")
    public Result<List<Object>> getMetricsByType(@PathVariable String type) {
        log.debug("[MetricsFallback] metrics/type/{} called, returning empty list", type);
        return Result.ok(List.of());
    }

    @GetMapping("/current/{metricCode}")
    @Operation(summary = "获取指标当前值（降级-空响应）")
    public Result<Map<String, Object>> getCurrentMetric(@PathVariable String metricCode) {
        log.debug("[MetricsFallback] current/{} called, returning empty", metricCode);
        return Result.ok(Map.of(
                "code", metricCode,
                "value", 0,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/current/batch")
    @Operation(summary = "批量获取指标当前值（降级-空列表）")
    public Result<List<Object>> getCurrentMetricsBatch(@RequestBody List<String> metricCodes) {
        log.debug("[MetricsFallback] current/batch called, returning empty list");
        return Result.ok(List.of());
    }

    @GetMapping("/history")
    @Operation(summary = "获取指标历史/排名（降级-空列表）")
    public Result<List<Object>> getRankingHistory(
            @RequestParam String type,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("[MetricsFallback] history called, type={}, returning empty list", type);
        return Result.ok(List.of());
    }

    @PostMapping("/history")
    @Operation(summary = "获取指标历史详情（降级-空响应）")
    public Result<Map<String, Object>> getMetricHistory() {
        log.debug("[MetricsFallback] POST history called, returning empty");
        return Result.ok(Map.of(
                "metricCode", "",
                "dataPoints", List.of()
        ));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有指标列表（降级-空列表）")
    public Result<List<Object>> getAllActiveMetrics() {
        log.debug("[MetricsFallback] list called, returning empty list");
        return Result.ok(List.of());
    }

    @GetMapping("/types")
    @Operation(summary = "获取指标类型（降级-空映射）")
    public Result<Map<String, String>> getMetricTypes() {
        log.debug("[MetricsFallback] types called, returning empty map");
        return Result.ok(Map.of());
    }

    @PostMapping("/refresh")
    @Operation(summary = "手动刷新指标（降级-空操作）")
    public Result<Map<String, String>> refreshMetrics() {
        log.debug("[MetricsFallback] refresh called, no-op");
        return Result.ok(Map.of(
                "status", "success",
                "message", "No-op (erp-metrics module not available)"
        ));
    }

    @GetMapping("/health")
    @Operation(summary = "指标模块健康检查（降级）")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "DEGRADED");
        health.put("service", "erp-metrics-fallback");
        health.put("message", "erp-metrics module not available, using fallback");
        health.put("timestamp", System.currentTimeMillis());
        return Result.ok(health);
    }
}
