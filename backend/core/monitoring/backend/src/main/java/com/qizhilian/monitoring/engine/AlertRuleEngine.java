package com.qizhilian.monitoring.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qizhilian.monitoring.entity.AlertHistoryEntity;
import com.qizhilian.monitoring.entity.AlertRuleEntity;
import com.qizhilian.monitoring.entity.MetricEntity;
import com.qizhilian.monitoring.notification.NotificationService;
import com.qizhilian.monitoring.repository.AlertHistoryRepository;
import com.qizhilian.monitoring.repository.AlertRuleRepository;
import com.qizhilian.monitoring.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 告警规则引擎
 * 基于YAML DSL的告警规则解析和执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MetricRepository metricRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Yaml yaml = new Yaml();

    /**
     * 定时执行告警规则检查（每30秒）
     */
    @Scheduled(fixedRate = 30000)
    public void evaluateRules() {
        List<AlertRuleEntity> rules = alertRuleRepository.findByEnabledTrue();
        for (AlertRuleEntity rule : rules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("Failed to evaluate rule {}: {}", rule.getRuleName(), e.getMessage());
            }
        }
    }

    public void evaluateRule(AlertRuleEntity rule) {
        if (!Boolean.TRUE.equals(rule.getEnabled())) return;
        if (rule.isInCooldownPeriod()) return;
        if (rule.isMaxAlertCountReached()) return;

        rule.executeCheck();
        alertRuleRepository.save(rule);

        List<MetricEntity> metrics = metricRepository.findTopNByMetricNameOrderByMetricTimeDesc(rule.getMetricName(), 5);
        if (metrics.isEmpty()) return;

        boolean triggered = false;
        switch (rule.getRuleType()) {
            case "threshold" -> triggered = evaluateThreshold(rule, metrics);
            case "trend" -> triggered = evaluateTrend(rule, metrics);
            case "anomaly" -> triggered = evaluateAnomaly(rule, metrics);
            case "composite" -> triggered = evaluateComposite(rule, metrics);
            default -> triggered = evaluateThreshold(rule, metrics);
        }

        if (triggered) {
            triggerAlert(rule, metrics.get(0));
        } else {
            checkRecovery(rule, metrics.get(0));
        }
    }

    private boolean evaluateThreshold(AlertRuleEntity rule, List<MetricEntity> metrics) {
        try {
            JsonNode condition = objectMapper.readTree(rule.getConditionConfig());
            String operator = condition.get("operator").asText("gte");
            BigDecimal threshold = new BigDecimal(condition.get("threshold").asText());
            int times = condition.has("times") ? condition.get("times").asInt(1) : 1;

            int matchCount = 0;
            for (MetricEntity m : metrics) {
                if (m.getMetricValue() == null) continue;
                if (compare(m.getMetricValue(), threshold, operator)) {
                    matchCount++;
                }
            }
            return matchCount >= times;
        } catch (Exception e) {
            log.error("Threshold evaluation error: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateTrend(AlertRuleEntity rule, List<MetricEntity> metrics) {
        if (metrics.size() < 2) return false;
        try {
            JsonNode condition = objectMapper.readTree(rule.getConditionConfig());
            String direction = condition.get("direction").asText("up");
            BigDecimal percentage = new BigDecimal(condition.get("percentage").asText("10"));

            BigDecimal first = metrics.get(metrics.size() - 1).getMetricValue();
            BigDecimal last = metrics.get(0).getMetricValue();
            if (first.compareTo(BigDecimal.ZERO) == 0) return false;

            BigDecimal change = last.subtract(first).multiply(BigDecimal.valueOf(100))
                    .divide(first, 4, java.math.RoundingMode.HALF_UP).abs();

            boolean isUp = last.compareTo(first) > 0;
            return change.compareTo(percentage) >= 0 && ("up".equals(direction) == isUp);
        } catch (Exception e) {
            log.error("Trend evaluation error: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateAnomaly(AlertRuleEntity rule, List<MetricEntity> metrics) {
        if (metrics.size() < 3) return false;
        try {
            JsonNode condition = objectMapper.readTree(rule.getConditionConfig());
            BigDecimal stdDevMultiplier = new BigDecimal(condition.get("stdDevMultiplier").asText("3"));

            BigDecimal sum = BigDecimal.ZERO;
            for (MetricEntity m : metrics) {
                sum = sum.add(m.getMetricValue());
            }
            BigDecimal avg = sum.divide(BigDecimal.valueOf(metrics.size()), 6, java.math.RoundingMode.HALF_UP);

            BigDecimal variance = BigDecimal.ZERO;
            for (MetricEntity m : metrics) {
                BigDecimal diff = m.getMetricValue().subtract(avg);
                variance = variance.add(diff.multiply(diff));
            }
            BigDecimal stdDev = java.math.BigDecimal.valueOf(Math.sqrt(variance.doubleValue() / metrics.size()));

            BigDecimal latest = metrics.get(0).getMetricValue();
            BigDecimal deviation = latest.subtract(avg).abs();
            return deviation.compareTo(stdDevMultiplier.multiply(stdDev)) > 0;
        } catch (Exception e) {
            log.error("Anomaly evaluation error: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateComposite(AlertRuleEntity rule, List<MetricEntity> metrics) {
        try {
            JsonNode condition = objectMapper.readTree(rule.getConditionConfig());
            // Simplified composite: all sub-rules must trigger
            if (condition.has("rules")) {
                for (JsonNode subRule : condition.get("rules")) {
                    String metricName = subRule.get("metricName").asText();
                    String op = subRule.get("operator").asText("gte");
                    BigDecimal threshold = new BigDecimal(subRule.get("threshold").asText());
                    List<MetricEntity> subMetrics = metricRepository.findTopNByMetricNameOrderByMetricTimeDesc(metricName, 1);
                    if (subMetrics.isEmpty() || !compare(subMetrics.get(0).getMetricValue(), threshold, op)) {
                        return false;
                    }
                }
                return true;
            }
        } catch (Exception e) {
            log.error("Composite evaluation error: {}", e.getMessage());
        }
        return false;
    }

    private boolean compare(BigDecimal value, BigDecimal threshold, String operator) {
        return switch (operator) {
            case ">", "gt", "gte", ">=" -> value.compareTo(threshold) >= 0;
            case "<", "lt", "lte", "<=" -> value.compareTo(threshold) <= 0;
            case "==", "eq" -> value.compareTo(threshold) == 0;
            case "!=", "ne" -> value.compareTo(threshold) != 0;
            default -> value.compareTo(threshold) >= 0;
        };
    }

    private void triggerAlert(AlertRuleEntity rule, MetricEntity metric) {
        if (rule.isInSilencePeriod()) return;

        rule.triggerAlert();
        alertRuleRepository.save(rule);

        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setRuleId(rule.getId());
        history.setRuleName(rule.getRuleName());
        history.setMetricName(rule.getMetricName());
        history.setMetricValue(metric.getMetricValue().toString());
        history.setSeverity(rule.getSeverity());
        history.setAlertTitle(renderTemplate(rule.getTitleTemplate(), rule, metric));
        history.setAlertContent(renderTemplate(rule.getContentTemplate(), rule, metric));
        history.setTriggerTime(LocalDateTime.now());
        history.setHostname(metric.getHostname());
        history.setServiceName(metric.getServiceName());
        history.setEnvironment(metric.getEnvironment());
        history.setNotificationChannels(rule.getNotificationChannels());
        history.setSilenceDuration(rule.getSilenceDuration());
        alertHistoryRepository.save(history);

        // Send notifications
        if (rule.getNotificationChannels() != null) {
            notificationService.sendNotification(history, rule.getNotificationChannels(), rule.getNotificationReceivers());
        }
    }

    private void checkRecovery(AlertRuleEntity rule, MetricEntity metric) {
        List<AlertHistoryEntity> activeAlerts = alertHistoryRepository.findActiveAlerts();
        for (AlertHistoryEntity alert : activeAlerts) {
            if (alert.getRuleId().equals(rule.getId())) {
                alert.resolve();
                if (Boolean.TRUE.equals(rule.getRecoveryNotification())) {
                    alert.setAlertTitle("[已恢复] " + alert.getAlertTitle());
                    alert.setAlertContent("告警已恢复\n恢复时间: " + LocalDateTime.now());
                    notificationService.sendNotification(alert, rule.getNotificationChannels(), rule.getNotificationReceivers());
                }
                alertHistoryRepository.save(alert);
                rule.recoverAlert();
                alertRuleRepository.save(rule);
            }
        }
    }

    private String renderTemplate(String template, AlertRuleEntity rule, MetricEntity metric) {
        if (template == null) return "";
        return template
                .replace("${ruleName}", rule.getRuleName())
                .replace("${metricName}", rule.getMetricName())
                .replace("${metricValue}", metric.getMetricValue().toString())
                .replace("${metricUnit}", metric.getMetricUnit() != null ? metric.getMetricUnit() : "")
                .replace("${severity}", rule.getSeverity())
                .replace("${triggerTime}", LocalDateTime.now().toString())
                .replace("${hostname}", metric.getHostname() != null ? metric.getHostname() : "")
                .replace("${serviceName}", metric.getServiceName() != null ? metric.getServiceName() : "")
                .replace("${environment}", metric.getEnvironment() != null ? metric.getEnvironment() : "");
    }

    /**
     * 从YAML字符串加载规则
     */
    public AlertRuleEntity loadRuleFromYaml(String yamlContent) {
        Map<String, Object> data = yaml.load(yamlContent);
        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setRuleName((String) data.get("name"));
        rule.setDescription((String) data.get("description"));
        rule.setMetricName((String) data.get("metric"));
        rule.setRuleType((String) data.getOrDefault("type", "threshold"));
        rule.setSeverity((String) data.getOrDefault("severity", "warning"));
        rule.setYamlDefinition(yamlContent);

        Map<String, Object> condition = (Map<String, Object>) data.get("condition");
        if (condition != null) {
            try {
                rule.setConditionConfig(objectMapper.writeValueAsString(condition));
            } catch (Exception e) {
                log.error("Failed to serialize condition: {}", e.getMessage());
            }
        }

        Map<String, Object> notify = (Map<String, Object>) data.get("notify");
        if (notify != null) {
            List<String> channels = (List<String>) notify.get("channels");
            if (channels != null) {
                try {
                    rule.setNotificationChannels(objectMapper.writeValueAsString(channels));
                } catch (Exception e) {
                    log.warn("序列化通知渠道失败", e);
                }
            }
            List<String> receivers = (List<String>) notify.get("receivers");
            if (receivers != null) {
                try {
                    rule.setNotificationReceivers(objectMapper.writeValueAsString(receivers));
                } catch (Exception e) {
                    log.warn("序列化通知接收者失败", e);
                }
            }
        }

        Map<String, Object> config = (Map<String, Object>) data.get("config");
        if (config != null) {
            if (config.containsKey("interval")) {
                rule.setCheckInterval((Integer) config.get("interval"));
            }
            if (config.containsKey("silence")) {
                rule.setSilenceDuration((Integer) config.get("silence"));
            }
            if (config.containsKey("maxAlerts")) {
                rule.setMaxAlertCount((Integer) config.get("maxAlerts"));
            }
        }
        return rule;
    }
}
