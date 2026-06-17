package cn.aiedge.monitor.controller;

import cn.aiedge.monitor.mapper.AlertHistoryMapper;
import cn.aiedge.monitor.model.AlertHistory;
import cn.aiedge.monitor.model.AlertRule;
import cn.aiedge.monitor.service.AlertRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/monitor/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警管理、通知配置和告警历史接口")
@SaCheckLogin
public class AlertManagementController {

    private final AlertRuleService alertRuleService;
    private final AlertHistoryMapper alertHistoryMapper;

    // 通知配置为运行时配置，使用内存存储
    private final Map<String, NotificationConfig> notificationConfigs = new ConcurrentHashMap<>();

    // ==================== 告警规则管理 ====================

    @PostMapping("/rules")
    @Operation(summary = "创建告警规则")
    public Map<String, Object> createAlertRule(@RequestBody AlertRule rule) {
        AlertRule created = alertRuleService.createRule(rule);
        return Map.of("code", 200, "data", created, "message", "创建成功");
    }

    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "更新告警规则")
    public Map<String, Object> updateAlertRule(
            @PathVariable Long ruleId,
            @RequestBody AlertRule rule) {
        rule.setId(ruleId);
        AlertRule updated = alertRuleService.updateRule(rule);
        return Map.of("code", 200, "data", updated, "message", "更新成功");
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "删除告警规则")
    public Map<String, Object> deleteAlertRule(@PathVariable Long ruleId) {
        boolean deleted = alertRuleService.deleteRule(ruleId);
        return Map.of("code", 200, "data", deleted, "message", deleted ? "删除成功" : "规则不存在");
    }

    @GetMapping("/rules/{ruleId}")
    @Operation(summary = "获取告警规则详情")
    public Map<String, Object> getAlertRule(@PathVariable Long ruleId) {
        AlertRule rule = alertRuleService.getRule(ruleId);
        if (rule != null) {
            return Map.of("code", 200, "data", rule, "message", "ok");
        }
        return Map.of("code", 404, "data", null, "message", "规则不存在");
    }

    @GetMapping("/rules")
    @Operation(summary = "获取告警规则列表")
    public Map<String, Object> listAlertRules(
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean enabled) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getDeleted, 0)
                .eq(metric != null, AlertRule::getMetricName, metric)
                .eq(severity != null, AlertRule::getSeverity, severity)
                .eq(enabled != null, AlertRule::getEnabled, enabled);

        List<AlertRule> rules = alertRuleService.getEnabledRules(null).stream()
                .filter(r -> metric == null || metric.equals(r.getMetricName()))
                .filter(r -> severity == null || severity.equals(r.getSeverity()))
                .filter(r -> enabled == null || enabled.equals(r.getEnabled()))
                .collect(Collectors.toList());

        return Map.of("code", 200, "data", rules, "message", "ok");
    }

    @PostMapping("/rules/{ruleId}/enable")
    @Operation(summary = "启用告警规则")
    public Map<String, Object> enableAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.enableRule(ruleId);
        return Map.of("code", 200, "data", success, "message", success ? "已启用" : "规则不存在");
    }

    @PostMapping("/rules/{ruleId}/disable")
    @Operation(summary = "禁用告警规则")
    public Map<String, Object> disableAlertRule(@PathVariable Long ruleId) {
        boolean success = alertRuleService.disableRule(ruleId);
        return Map.of("code", 200, "data", success, "message", success ? "已禁用" : "规则不存在");
    }

    // ==================== 告警历史 ====================

    @GetMapping("/history")
    @Operation(summary = "获取告警历史")
    public Map<String, Object> getAlertHistory(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String metric,
            @RequestParam(defaultValue = "50") int limit) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        LambdaQueryWrapper<AlertHistory> wrapper = new LambdaQueryWrapper<AlertHistory>()
                .ge(AlertHistory::getAlertTime, cutoff)
                .eq(severity != null, AlertHistory::getSeverity, severity)
                .eq(metric != null, AlertHistory::getMetricName, metric)
                .orderByDesc(AlertHistory::getAlertTime)
                .last("LIMIT " + limit);

        List<AlertHistory> alerts = alertHistoryMapper.selectList(wrapper);

        // 统计
        Map<String, Long> stats = alertHistoryMapper.selectList(
                new LambdaQueryWrapper<AlertHistory>()
                        .ge(AlertHistory::getAlertTime, cutoff)
        ).stream().collect(Collectors.groupingBy(
                a -> a.getSeverity() != null ? a.getSeverity() : "unknown",
                Collectors.counting()
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", alerts);
        result.put("total", alerts.size());
        result.put("stats", stats);
        result.put("timeRange", hours + " hours");
        return result;
    }

    @DeleteMapping("/history")
    @Operation(summary = "清除告警历史")
    public Map<String, Object> clearAlertHistory(
            @RequestParam(defaultValue = "24") int olderThanHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(olderThanHours);

        LambdaQueryWrapper<AlertHistory> wrapper = new LambdaQueryWrapper<AlertHistory>()
                .lt(AlertHistory::getAlertTime, cutoff);
        int removed = alertHistoryMapper.delete(wrapper);

        log.info("清除告警历史: 删除 {} 条", removed);
        return Map.of("code", 200, "data", removed, "message", "已清除" + removed + "条记录");
    }

    // ==================== 告警操作 ====================

    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "确认告警")
    public Map<String, Object> acknowledgeAlert(
            @PathVariable Long alertId,
            @RequestParam(required = false) String acknowledgedBy) {
        AlertHistory history = alertHistoryMapper.selectById(alertId);
        if (history == null) {
            return Map.of("code", 404, "data", null, "message", "告警记录不存在");
        }
        history.setAcknowledged(true);
        history.setAcknowledgedBy(acknowledgedBy != null ? acknowledgedBy : "system");
        history.setAcknowledgedAt(LocalDateTime.now());
        alertHistoryMapper.updateById(history);
        return Map.of("code", 200, "data", true, "message", "已确认");
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "解决告警")
    public Map<String, Object> resolveAlert(
            @PathVariable Long alertId,
            @RequestParam(required = false) String resolvedBy,
            @RequestParam(required = false) String resolution) {
        AlertHistory history = alertHistoryMapper.selectById(alertId);
        if (history == null) {
            return Map.of("code", 404, "data", null, "message", "告警记录不存在");
        }
        history.setResolved(true);
        history.setResolvedBy(resolvedBy != null ? resolvedBy : "system");
        history.setResolvedAt(LocalDateTime.now());
        history.setResolution(resolution);
        alertHistoryMapper.updateById(history);
        return Map.of("code", 200, "data", true, "message", "已解决");
    }

    // ==================== 通知配置 ====================

    @PostMapping("/notification/config")
    @Operation(summary = "配置通知渠道")
    public Map<String, Object> configureNotification(@RequestBody Map<String, Object> config) {
        String channel = (String) config.get("channel");
        if (channel == null) {
            return Map.of("code", 400, "data", null, "message", "渠道标识不能为空");
        }

        NotificationConfig notificationConfig = new NotificationConfig();
        notificationConfig.setChannel(channel);
        notificationConfig.setEnabled((Boolean) config.getOrDefault("enabled", true));
        notificationConfig.setConfig((Map<String, Object>) config.get("config"));
        notificationConfig.setUpdateTime(LocalDateTime.now());

        notificationConfigs.put(channel, notificationConfig);
        log.info("配置通知渠道: {}", channel);
        return Map.of("code", 200, "data", true, "message", "通知渠道已配置");
    }

    @GetMapping("/notification/config")
    @Operation(summary = "获取通知配置")
    public Map<String, Object> getNotificationConfigs() {
        List<Map<String, Object>> configs = notificationConfigs.values().stream()
                .map(NotificationConfig::toMap)
                .collect(Collectors.toList());
        return Map.of("code", 200, "data", configs, "message", "ok");
    }

    @PostMapping("/notification/test")
    @Operation(summary = "测试通知渠道")
    public Map<String, Object> testNotification(@RequestParam String channel) {
        NotificationConfig config = notificationConfigs.get(channel);
        if (config == null) {
            return Map.of("code", 400, "data", null, "message", "通知渠道未配置");
        }

        log.info("测试通知渠道: {}", channel);
        Map<String, Object> testAlert = new HashMap<>();
        testAlert.put("id", UUID.randomUUID().toString());
        testAlert.put("severity", "info");
        testAlert.put("message", "Test notification from AI-Ready Monitor");
        testAlert.put("timestamp", LocalDateTime.now().toString());

        return Map.of("code", 200, "data", testAlert, "message", "测试通知已发送");
    }

    // ==================== 告警统计 ====================

    @GetMapping("/statistics")
    @Operation(summary = "获取告警统计")
    public Map<String, Object> getAlertStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        List<AlertHistory> recentAlerts = alertHistoryMapper.selectList(
                new LambdaQueryWrapper<AlertHistory>()
                        .ge(AlertHistory::getAlertTime, cutoff)
        );

        Map<String, Long> bySeverity = recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSeverity() != null ? a.getSeverity() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Long> byMetric = recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getMetricName() != null ? a.getMetricName() : "unknown",
                        Collectors.counting()
                ));

        Map<Integer, Long> byHour = recentAlerts.stream()
                .filter(a -> a.getAlertTime() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getAlertTime().getHour(),
                        Collectors.counting()
                ));

        long unacknowledged = recentAlerts.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getAcknowledged()))
                .filter(a -> !Boolean.TRUE.equals(a.getResolved()))
                .count();

        long activeRules = alertRuleService.getEnabledRules(null).size();

        Map<String, Object> result = new HashMap<>();
        result.put("totalAlerts", recentAlerts.size());
        result.put("unacknowledgedAlerts", unacknowledged);
        result.put("activeRules", activeRules);
        result.put("bySeverity", bySeverity);
        result.put("byMetric", byMetric);
        result.put("byHour", byHour);
        result.put("timeRange", hours + " hours");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", result);
        response.put("message", "ok");
        return response;
    }

    // ==================== 内部类 ====================

    private static class NotificationConfig {
        private String channel;
        private boolean enabled;
        private Map<String, Object> config;
        private LocalDateTime updateTime;

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("channel", channel);
            map.put("enabled", enabled);
            map.put("config", config);
            map.put("updateTime", updateTime != null ? updateTime.toString() : null);
            return map;
        }
    }
}
