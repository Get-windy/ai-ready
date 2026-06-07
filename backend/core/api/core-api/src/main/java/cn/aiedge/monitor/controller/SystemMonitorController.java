package cn.aiedge.monitor.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.monitor.model.AlertRule;
import cn.aiedge.monitor.model.SystemMetrics;
import cn.aiedge.monitor.service.AlertRuleService;
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
public class SystemMonitorController {

    private final SystemMonitorService monitorService;
    private final AlertRuleService alertRuleService;

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

    @PostMapping("/alerts/rules")
    @Operation(summary = "创建告警规则")
    public Result<AlertRule> createAlertRule(@RequestBody AlertRule rule) {
        return Result.ok(alertRuleService.createRule(rule));
    }

    @PutMapping("/alerts/rules")
    @Operation(summary = "更新告警规则")
    public Result<AlertRule> updateAlertRule(@RequestBody AlertRule rule) {
        return Result.ok(alertRuleService.updateRule(rule));
    }

    @DeleteMapping("/alerts/rules/{ruleId}")
    @Operation(summary = "删除告警规则")
    public Result<Map<String, Object>> deleteAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.deleteRule(ruleId);
        return Result.ok(Map.of("success", success));
    }

    @GetMapping("/alerts/rules/{ruleId}")
    @Operation(summary = "获取告警规则")
    public Result<AlertRule> getAlertRule(@PathVariable Long ruleId) {
        return Result.ok(alertRuleService.getRule(ruleId));
    }

    @GetMapping("/alerts/rules")
    @Operation(summary = "获取告警规则列表")
    public Result<List<AlertRule>> getEnabledRules(
            @RequestParam(required = false) Long tenantId) {
        return Result.ok(alertRuleService.getEnabledRules(tenantId));
    }

    @PostMapping("/alerts/rules/{ruleId}/enable")
    @Operation(summary = "启用告警规则")
    public Result<Map<String, Object>> enableAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.enableRule(ruleId);
        return Result.ok(Map.of("success", success));
    }

    @PostMapping("/alerts/rules/{ruleId}/disable")
    @Operation(summary = "禁用告警规则")
    public Result<Map<String, Object>> disableAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.disableRule(ruleId);
        return Result.ok(Map.of("success", success));
    }

    @GetMapping("/alerts/history")
    @Operation(summary = "获取告警历史")
    public Result<List<Map<String, Object>>> getAlertHistory(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "24") int hours) {
        return Result.ok(alertRuleService.getAlertHistory(tenantId, hours));
    }
}
