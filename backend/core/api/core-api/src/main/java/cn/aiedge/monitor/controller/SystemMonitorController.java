package cn.aiedge.monitor.controller;

import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.aiedge.monitor.model.SystemMetrics;
import cn.aiedge.monitor.service.SystemMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统监控控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "系统监控", description = "系统监控和告警管理接口")
@SaCheckLogin
public class SystemMonitorController {

    private final SystemMonitorService monitorService;

    // ==================== 系统监控 ====================

    @GetMapping("/metrics")
    @Operation(summary = "获取当前系统指标")
    public Result<SystemMetrics> getCurrentMetrics() {
        return Result.ok(monitorService.getCurrentMetrics());
    }

    @GetMapping("/metrics/history")
    @Operation(summary = "获取历史指标")
    public Result<List<SystemMetrics>> getHistoryMetrics(
            @RequestParam(defaultValue = "1") int hours) {
        return Result.ok(monitorService.getHistoryMetrics(hours));
    }

    @GetMapping("/metrics/trend/{metricName}")
    @Operation(summary = "获取指标趋势")
    public Result<Map<String, Object>> getMetricTrend(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "1") int hours) {
        return Result.ok(monitorService.getMetricTrend(metricName, hours));
    }

    @GetMapping("/overview")
    @Operation(summary = "获取系统概览")
    public Result<Map<String, Object>> getSystemOverview() {
        return Result.ok(monitorService.getSystemOverview());
    }

    @GetMapping("/health")
    @Operation(summary = "检查系统健康状态")
    public Result<Map<String, Object>> checkHealth() {
        return Result.ok(monitorService.checkHealth());
    }

    @GetMapping("/jvm")
    @Operation(summary = "获取JVM信息")
    public Result<Map<String, Object>> getJvmInfo() {
        return Result.ok(monitorService.getJvmInfo());
    }

    @GetMapping("/threads")
    @Operation(summary = "获取线程信息")
    public Result<Map<String, Object>> getThreadInfo() {
        return Result.ok(monitorService.getThreadInfo());
    }

    @GetMapping("/memory")
    @Operation(summary = "获取内存信息")
    public Result<Map<String, Object>> getMemoryInfo() {
        return Result.ok(monitorService.getMemoryInfo());
    }

    @PostMapping("/gc")
    @Operation(summary = "执行垃圾回收")
    public Result<Map<String, Object>> performGc() {
        monitorService.performGc();
        return Result.ok(Map.of("success", true, "message", "GC triggered"));
    }

    // ==================== 告警规则管理 ====================
    // ⚠️ 告警相关端点（/api/monitor/alerts/**）已**整体移除**（2026-09-18）：
    //    本类与 AlertManagementController（@RequestMapping("/api/monitor/alerts")）存在 7 组
    //    完全相同的 method+path（POST|GET /rules、GET|DELETE /rules/{ruleId}、
    //    POST /rules/{ruleId}/enable|disable、GET /history），此前因 cn.aiedge.monitor 包
    //    **未列入 scanBasePackages** 而从未暴露；一旦装配即报
    //    `IllegalStateException: Ambiguous mapping ... /api/monitor/alerts/history`，
    //    直接导致应用启动失败。
    //    保留 AlertManagementController —— 它是功能更全的一份（另有 acknowledge / resolve /
    //    notification config / test / statistics），且**路径完全兼容**本类原有的 7 个端点，
    //    故不存在能力损失；仅 `PUT /rules`（body 带 id）这一处路径形态不同，
    //    现统一为 `PUT /api/monitor/alerts/rules/{ruleId}`（前端实测零调用，见验收脚本）。
}
