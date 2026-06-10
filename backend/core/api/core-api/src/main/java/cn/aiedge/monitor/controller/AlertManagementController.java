package cn.aiedge.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 告警管理和通知控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警管理、通知配置和告警历史接口")
@SaCheckLogin
public class AlertManagementController {

    // 内存存储（实际应使用数据库）
    private final Map<Long, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> alertHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, NotificationConfig> notificationConfigs = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // ==================== 告警规则管理 ====================

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    @Operation(summary = "创建告警规则")
    public Map<String, Object> createAlertRule(@RequestBody Map<String, Object> ruleData) {
        Map<String, Object> result = new HashMap<>();
        
        Long id = idGenerator.getAndIncrement();
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setName((String) ruleData.get("name"));
        rule.setDescription((String) ruleData.get("description"));
        rule.setMetric((String) ruleData.get("metric"));
        rule.setOperator((String) ruleData.get("operator"));
        rule.setThreshold(parseDouble(ruleData.get("threshold")));
        rule.setDuration(parseInt(ruleData.get("duration"), 60));
        rule.setSeverity((String) ruleData.getOrDefault("severity", "warning"));
        rule.setEnabled(true);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        
        // 通知配置
        @SuppressWarnings("unchecked")
        Map<String, Object> notifyConfig = (Map<String, Object>) ruleData.get("notification");
        if (notifyConfig != null) {
            rule.setNotifyChannels((List<String>) notifyConfig.get("channels"));
            rule.setNotifyTargets((List<String>) notifyConfig.get("targets"));
        }
        
        alertRules.put(id, rule);
        
        result.put("success", true);
        result.put("rule", rule.toMap());
        result.put("message", "Alert rule created successfully");
        
        log.info("Created alert rule: {} (ID: {})", rule.getName(), id);
        
        return result;
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "更新告警规则")
    public Map<String, Object> updateAlertRule(
            @PathVariable Long ruleId,
            @RequestBody Map<String, Object> ruleData) {
        Map<String, Object> result = new HashMap<>();
        
        AlertRule rule = alertRules.get(ruleId);
        if (rule == null) {
            result.put("success", false);
            result.put("message", "Alert rule not found: " + ruleId);
            return result;
        }
        
        if (ruleData.containsKey("name")) rule.setName((String) ruleData.get("name"));
        if (ruleData.containsKey("description")) rule.setDescription((String) ruleData.get("description"));
        if (ruleData.containsKey("metric")) rule.setMetric((String) ruleData.get("metric"));
        if (ruleData.containsKey("operator")) rule.setOperator((String) ruleData.get("operator"));
        if (ruleData.containsKey("threshold")) rule.setThreshold(parseDouble(ruleData.get("threshold")));
        if (ruleData.containsKey("duration")) rule.setDuration(parseInt(ruleData.get("duration"), rule.getDuration()));
        if (ruleData.containsKey("severity")) rule.setSeverity((String) ruleData.get("severity"));
        if (ruleData.containsKey("enabled")) rule.setEnabled((Boolean) ruleData.get("enabled"));
        
        rule.setUpdateTime(LocalDateTime.now());
        
        result.put("success", true);
        result.put("rule", rule.toMap());
        result.put("message", "Alert rule updated successfully");
        
        log.info("Updated alert rule: {} (ID: {})", rule.getName(), ruleId);
        
        return result;
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "删除告警规则")
    public Map<String, Object> deleteAlertRule(@PathVariable Long ruleId) {
        Map<String, Object> result = new HashMap<>();
        
        AlertRule rule = alertRules.remove(ruleId);
        if (rule != null) {
            result.put("success", true);
            result.put("message", "Alert rule deleted successfully");
            log.info("Deleted alert rule: {} (ID: {})", rule.getName(), ruleId);
        } else {
            result.put("success", false);
            result.put("message", "Alert rule not found: " + ruleId);
        }
        
        return result;
    }

    /**
     * 获取告警规则
     */
    @GetMapping("/rules/{ruleId}")
    @Operation(summary = "获取告警规则详情")
    public Map<String, Object> getAlertRule(@PathVariable Long ruleId) {
        Map<String, Object> result = new HashMap<>();
        
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            result.put("success", true);
            result.put("rule", rule.toMap());
        } else {
            result.put("success", false);
            result.put("message", "Alert rule not found: " + ruleId);
        }
        
        return result;
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    @Operation(summary = "获取告警规则列表")
    public Map<String, Object> listAlertRules(
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean enabled) {
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> rules = alertRules.values().stream()
                .filter(r -> metric == null || metric.equals(r.getMetric()))
                .filter(r -> severity == null || severity.equals(r.getSeverity()))
                .filter(r -> enabled == null || enabled.equals(r.isEnabled()))
                .map(AlertRule::toMap)
                .collect(Collectors.toList());
        
        result.put("success", true);
        result.put("rules", rules);
        result.put("total", rules.size());
        
        return result;
    }

    /**
     * 启用告警规则
     */
    @PostMapping("/rules/{ruleId}/enable")
    @Operation(summary = "启用告警规则")
    public Map<String, Object> enableAlertRule(@PathVariable Long ruleId) {
        Map<String, Object> result = new HashMap<>();
        
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(true);
            rule.setUpdateTime(LocalDateTime.now());
            result.put("success", true);
            result.put("message", "Alert rule enabled");
        } else {
            result.put("success", false);
            result.put("message", "Alert rule not found: " + ruleId);
        }
        
        return result;
    }

    /**
     * 禁用告警规则
     */
    @PostMapping("/rules/{ruleId}/disable")
    @Operation(summary = "禁用告警规则")
    public Map<String, Object> disableAlertRule(@PathVariable Long ruleId) {
        Map<String, Object> result = new HashMap<>();
        
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(false);
            rule.setUpdateTime(LocalDateTime.now());
            result.put("success", true);
            result.put("message", "Alert rule disabled");
        } else {
            result.put("success", false);
            result.put("message", "Alert rule not found: " + ruleId);
        }
        
        return result;
    }

    // ==================== 告警历史 ====================

    /**
     * 获取告警历史
     */
    @GetMapping("/history")
    @Operation(summary = "获取告警历史")
    public Map<String, Object> getAlertHistory(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String metric,
            @RequestParam(defaultValue = "50") int limit) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        List<Map<String, Object>> filtered = alertHistory.stream()
                .filter(a -> {
                    LocalDateTime timestamp = (LocalDateTime) a.get("timestamp");
                    return timestamp != null && timestamp.isAfter(cutoff);
                })
                .filter(a -> severity == null || severity.equals(a.get("severity")))
                .filter(a -> metric == null || metric.equals(a.get("metric")))
                .sorted((a, b) -> {
                    LocalDateTime ta = (LocalDateTime) a.get("timestamp");
                    LocalDateTime tb = (LocalDateTime) b.get("timestamp");
                    return tb.compareTo(ta); // 降序
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        // 统计
        Map<String, Long> stats = alertHistory.stream()
                .filter(a -> {
                    LocalDateTime timestamp = (LocalDateTime) a.get("timestamp");
                    return timestamp != null && timestamp.isAfter(cutoff);
                })
                .collect(Collectors.groupingBy(
                        a -> (String) a.getOrDefault("severity", "unknown"),
                        Collectors.counting()
                ));
        
        result.put("success", true);
        result.put("alerts", filtered);
        result.put("total", filtered.size());
        result.put("stats", stats);
        result.put("timeRange", hours + " hours");
        
        return result;
    }

    /**
     * 清除告警历史
     */
    @DeleteMapping("/history")
    @Operation(summary = "清除告警历史")
    public Map<String, Object> clearAlertHistory(
            @RequestParam(defaultValue = "24") int olderThanHours) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(olderThanHours);
        
        int removed = 0;
        synchronized (alertHistory) {
            removed = (int) alertHistory.stream()
                    .filter(a -> {
                        LocalDateTime timestamp = (LocalDateTime) a.get("timestamp");
                        return timestamp != null && timestamp.isBefore(cutoff);
                    })
                    .count();
            alertHistory.removeIf(a -> {
                LocalDateTime timestamp = (LocalDateTime) a.get("timestamp");
                return timestamp != null && timestamp.isBefore(cutoff);
            });
        }
        
        result.put("success", true);
        result.put("removed", removed);
        result.put("message", "Cleared " + removed + " alerts older than " + olderThanHours + " hours");
        
        log.info("Cleared {} alerts older than {} hours", removed, olderThanHours);
        
        return result;
    }

    // ==================== 告警操作 ====================

    /**
     * 确认告警
     */
    @PostMapping("/{alertId}/acknowledge")
    @Operation(summary = "确认告警")
    public Map<String, Object> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestParam(required = false) String acknowledgedBy) {
        Map<String, Object> result = new HashMap<>();
        
        synchronized (alertHistory) {
            for (Map<String, Object> alert : alertHistory) {
                if (alertId.equals(alert.get("id"))) {
                    alert.put("acknowledged", true);
                    alert.put("acknowledgedBy", acknowledgedBy != null ? acknowledgedBy : "system");
                    alert.put("acknowledgedAt", LocalDateTime.now());
                    
                    result.put("success", true);
                    result.put("message", "Alert acknowledged");
                    return result;
                }
            }
        }
        
        result.put("success", false);
        result.put("message", "Alert not found: " + alertId);
        return result;
    }

    /**
     * 解决告警
     */
    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "解决告警")
    public Map<String, Object> resolveAlert(
            @PathVariable String alertId,
            @RequestParam(required = false) String resolvedBy,
            @RequestParam(required = false) String resolution) {
        Map<String, Object> result = new HashMap<>();
        
        synchronized (alertHistory) {
            for (Map<String, Object> alert : alertHistory) {
                if (alertId.equals(alert.get("id"))) {
                    alert.put("resolved", true);
                    alert.put("resolvedBy", resolvedBy != null ? resolvedBy : "system");
                    alert.put("resolvedAt", LocalDateTime.now());
                    alert.put("resolution", resolution);
                    
                    result.put("success", true);
                    result.put("message", "Alert resolved");
                    return result;
                }
            }
        }
        
        result.put("success", false);
        result.put("message", "Alert not found: " + alertId);
        return result;
    }

    // ==================== 通知配置 ====================

    /**
     * 配置通知渠道
     */
    @PostMapping("/notification/config")
    @Operation(summary = "配置通知渠道")
    public Map<String, Object> configureNotification(@RequestBody Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        
        String channel = (String) config.get("channel");
        if (channel == null) {
            result.put("success", false);
            result.put("message", "Channel is required");
            return result;
        }
        
        NotificationConfig notificationConfig = new NotificationConfig();
        notificationConfig.setChannel(channel);
        notificationConfig.setEnabled((Boolean) config.getOrDefault("enabled", true));
        notificationConfig.setConfig((Map<String, Object>) config.get("config"));
        notificationConfig.setUpdateTime(LocalDateTime.now());
        
        notificationConfigs.put(channel, notificationConfig);
        
        result.put("success", true);
        result.put("message", "Notification channel configured: " + channel);
        
        log.info("Configured notification channel: {}", channel);
        
        return result;
    }

    /**
     * 获取通知配置
     */
    @GetMapping("/notification/config")
    @Operation(summary = "获取通知配置")
    public Map<String, Object> getNotificationConfigs() {
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> configs = notificationConfigs.values().stream()
                .map(NotificationConfig::toMap)
                .collect(Collectors.toList());
        
        result.put("success", true);
        result.put("configs", configs);
        
        return result;
    }

    /**
     * 测试通知
     */
    @PostMapping("/notification/test")
    @Operation(summary = "测试通知渠道")
    public Map<String, Object> testNotification(@RequestParam String channel) {
        Map<String, Object> result = new HashMap<>();
        
        NotificationConfig config = notificationConfigs.get(channel);
        if (config == null) {
            result.put("success", false);
            result.put("message", "Notification channel not configured: " + channel);
            return result;
        }
        
        // 模拟发送测试通知
        log.info("Sending test notification via channel: {}", channel);
        
        Map<String, Object> testAlert = new HashMap<>();
        testAlert.put("id", UUID.randomUUID().toString());
        testAlert.put("severity", "info");
        testAlert.put("message", "Test notification from AI-Ready Monitor");
        testAlert.put("timestamp", LocalDateTime.now());
        
        result.put("success", true);
        result.put("message", "Test notification sent via " + channel);
        result.put("testAlert", testAlert);
        
        return result;
    }

    // ==================== 告警统计 ====================

    /**
     * 获取告警统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取告警统计")
    public Map<String, Object> getAlertStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        List<Map<String, Object>> recentAlerts = alertHistory.stream()
                .filter(a -> {
                    LocalDateTime timestamp = (LocalDateTime) a.get("timestamp");
                    return timestamp != null && timestamp.isAfter(cutoff);
                })
                .collect(Collectors.toList());
        
        // 按严重程度统计
        Map<String, Long> bySeverity = recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        a -> (String) a.getOrDefault("severity", "unknown"),
                        Collectors.counting()
                ));
        
        // 按指标统计
        Map<String, Long> byMetric = recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        a -> (String) a.getOrDefault("metric", "unknown"),
                        Collectors.counting()
                ));
        
        // 按小时统计
        Map<Integer, Long> byHour = recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        a -> ((LocalDateTime) a.get("timestamp")).getHour(),
                        Collectors.counting()
                ));
        
        // 未确认告警
        long unacknowledged = recentAlerts.stream()
                .filter(a -> !Boolean.TRUE.equals(a.get("acknowledged")))
                .filter(a -> !Boolean.TRUE.equals(a.get("resolved")))
                .count();
        
        // 活跃告警规则
        long activeRules = alertRules.values().stream()
                .filter(AlertRule::isEnabled)
                .count();
        
        result.put("success", true);
        result.put("totalAlerts", recentAlerts.size());
        result.put("unacknowledgedAlerts", unacknowledged);
        result.put("activeRules", activeRules);
        result.put("bySeverity", bySeverity);
        result.put("byMetric", byMetric);
        result.put("byHour", byHour);
        result.put("timeRange", hours + " hours");
        
        return result;
    }

    // ==================== 私有方法 ====================

    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // ==================== 内部类 ====================

    private static class AlertRule {
        private Long id;
        private String name;
        private String description;
        private String metric;
        private String operator;
        private double threshold;
        private int duration = 60;
        private String severity = "warning";
        private boolean enabled = true;
        private List<String> notifyChannels;
        private List<String> notifyTargets;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getNotifyChannels() { return notifyChannels; }
        public void setNotifyChannels(List<String> notifyChannels) { this.notifyChannels = notifyChannels; }
        public List<String> getNotifyTargets() { return notifyTargets; }
        public void setNotifyTargets(List<String> notifyTargets) { this.notifyTargets = notifyTargets; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("description", description);
            map.put("metric", metric);
            map.put("operator", operator);
            map.put("threshold", threshold);
            map.put("duration", duration);
            map.put("severity", severity);
            map.put("enabled", enabled);
            map.put("notifyChannels", notifyChannels);
            map.put("notifyTargets", notifyTargets);
            map.put("createTime", createTime != null ? createTime.toString() : null);
            map.put("updateTime", updateTime != null ? updateTime.toString() : null);
            return map;
        }
    }

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