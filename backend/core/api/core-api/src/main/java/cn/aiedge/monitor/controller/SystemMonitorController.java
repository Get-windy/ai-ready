package cn.aiedge.monitor.controller;

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
    public SystemMetrics getCurrentMetrics() {
        return monitorService.getCurrentMetrics();
    }

    @GetMapping("/metrics/history")
    @Operation(summary = "获取历史指标")
    public List<SystemMetrics> getHistoryMetrics(
            @RequestParam(defaultValue = "1") int hours) {
        return monitorService.getHistoryMetrics(hours);
    }

    @GetMapping("/metrics/trend/{metricName}")
    @Operation(summary = "获取指标趋势")
    public Map<String, Object> getMetricTrend(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "1") int hours) {
        return monitorService.getMetricTrend(metricName, hours);
    }

    @GetMapping("/overview")
    @Operation(summary = "获取系统概览")
    public Map<String, Object> getSystemOverview() {
        return monitorService.getSystemOverview();
    }

    @GetMapping("/health")
    @Operation(summary = "检查系统健康状态")
    public Map<String, Object> checkHealth() {
        return monitorService.checkHealth();
    }

    @GetMapping("/jvm")
    @Operation(summary = "获取JVM信息")
    public Map<String, Object> getJvmInfo() {
        return monitorService.getJvmInfo();
    }

    @GetMapping("/threads")
    @Operation(summary = "获取线程信息")
    public Map<String, Object> getThreadInfo() {
        return monitorService.getThreadInfo();
    }

    @GetMapping("/memory")
    @Operation(summary = "获取内存信息")
    public Map<String, Object> getMemoryInfo() {
        return monitorService.getMemoryInfo();
    }

    @PostMapping("/gc")
    @Operation(summary = "执行垃圾回收")
    public Map<String, Object> performGc() {
        monitorService.performGc();
        return Map.of("success", true, "message", "GC triggered");
    }

    // ==================== 告警规则管理 ====================

    @PostMapping("/alerts/rules")
    @Operation(summary = "创建告警规则")
    public AlertRule createAlertRule(@RequestBody AlertRule rule) {
        return alertRuleService.createRule(rule);
    }

    @PutMapping("/alerts/rules")
    @Operation(summary = "更新告警规则")
    public AlertRule updateAlertRule(@RequestBody AlertRule rule) {
        return alertRuleService.updateRule(rule);
    }

    @DeleteMapping("/alerts/rules/{ruleId}")
    @Operation(summary = "删除告警规则")
    public Map<String, Object> deleteAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.deleteRule(ruleId);
        return Map.of("success", success);
    }

    @GetMapping("/alerts/rules/{ruleId}")
    @Operation(summary = "获取告警规则")
    public AlertRule getAlertRule(@PathVariable Long ruleId) {
        return alertRuleService.getRule(ruleId);
    }

    @GetMapping("/alerts/rules")
    @Operation(summary = "获取告警规则列表")
    public List<AlertRule> getEnabledRules(
            @RequestParam(required = false) Long tenantId) {
        return alertRuleService.getEnabledRules(tenantId);
    }

    @PostMapping("/alerts/rules/{ruleId}/enable")
    @Operation(summary = "启用告警规则")
    public Map<String, Object> enableAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.enableRule(ruleId);
        return Map.of("success", success);
    }

    @PostMapping("/alerts/rules/{ruleId}/disable")
    @Operation(summary = "禁用告警规则")
    public Map<String, Object> disableAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.disableRule(ruleId);
        return Map.of("success", success);
    }

    @GetMapping("/alerts/history")
    @Operation(summary = "获取告警历史")
    public List<Map<String, Object>> getAlertHistory(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "24") int hours) {
        return alertRuleService.getAlertHistory(tenantId, hours);
    }
}
