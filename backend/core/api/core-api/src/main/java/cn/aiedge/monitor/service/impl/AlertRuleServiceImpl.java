package cn.aiedge.monitor.service.impl;

import cn.aiedge.monitor.model.AlertRule;
import cn.aiedge.monitor.service.AlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 告警规则服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class AlertRuleServiceImpl implements AlertRuleService {

    // 内存存储（实际应使用数据库）
    private final Map<Long, AlertRule> ruleStore = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> alertHistory = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public AlertRule createRule(AlertRule rule) {
        rule.setId(idGenerator.getAndIncrement());
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setEnabled(true);
        rule.setDeleted(0);
        
        ruleStore.put(rule.getId(), rule);
        log.info("Created alert rule: {}", rule.getRuleName());
        return rule;
    }

    @Override
    public AlertRule updateRule(AlertRule rule) {
        if (rule.getId() == null || !ruleStore.containsKey(rule.getId())) {
            throw new IllegalArgumentException("Rule not found: " + rule.getId());
        }
        
        AlertRule existing = ruleStore.get(rule.getId());
        existing.setRuleName(rule.getRuleName());
        existing.setMetricName(rule.getMetricName());
        existing.setOperator(rule.getOperator());
        existing.setThreshold(rule.getThreshold());
        existing.setDuration(rule.getDuration());
        existing.setSeverity(rule.getSeverity());
        existing.setNotifyType(rule.getNotifyType());
        existing.setNotifyTargets(rule.getNotifyTargets());
        existing.setUpdateTime(LocalDateTime.now());
        
        log.info("Updated alert rule: {}", rule.getId());
        return existing;
    }

    @Override
    public boolean deleteRule(Long ruleId) {
        AlertRule rule = ruleStore.get(ruleId);
        if (rule != null) {
            rule.setDeleted(1);
            log.info("Deleted alert rule: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public AlertRule getRule(Long ruleId) {
        AlertRule rule = ruleStore.get(ruleId);
        return rule != null && rule.getDeleted() == 0 ? rule : null;
    }

    @Override
    public List<AlertRule> getEnabledRules(Long tenantId) {
        return ruleStore.values().stream()
                .filter(r -> r.getDeleted() == 0)
                .filter(r -> r.getEnabled() != null && r.getEnabled())
                .filter(r -> tenantId == null || tenantId.equals(r.getTenantId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> checkAndAlert(String metricName, double value, Long tenantId) {
        List<Map<String, Object>> triggeredAlerts = new ArrayList<>();
        
        List<AlertRule> rules = getEnabledRules(tenantId).stream()
                .filter(r -> r.getMetricName().equals(metricName))
                .collect(Collectors.toList());
        
        for (AlertRule rule : rules) {
            if (rule.isTriggered(value)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("ruleId", rule.getId());
                alert.put("ruleName", rule.getRuleName());
                alert.put("metricName", metricName);
                alert.put("value", value);
                alert.put("threshold", rule.getThreshold());
                alert.put("severity", rule.getSeverity());
                alert.put("timestamp", LocalDateTime.now());
                alert.put("message", String.format("%s: %s = %.2f (threshold: %s %.2f)",
                        rule.getSeverity(), metricName, value, rule.getOperator(), rule.getThreshold()));
                
                triggeredAlerts.add(alert);
                alertHistory.add(alert);
                
                // 模拟发送通知
                sendNotification(rule, alert);
            }
        }
        
        return triggeredAlerts;
    }

    @Override
    public boolean enableRule(Long ruleId) {
        AlertRule rule = ruleStore.get(ruleId);
        if (rule != null) {
            rule.setEnabled(true);
            log.info("Enabled alert rule: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public boolean disableRule(Long ruleId) {
        AlertRule rule = ruleStore.get(ruleId);
        if (rule != null) {
            rule.setEnabled(false);
            log.info("Disabled alert rule: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> getAlertHistory(Long tenantId, int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        return alertHistory.stream()
                .filter(a -> {
                    LocalDateTime timestamp = (LocalDateTime) a.get("timestamp");
                    return timestamp.isAfter(cutoff);
                })
                .sorted((a, b) -> {
                    LocalDateTime ta = (LocalDateTime) a.get("timestamp");
                    LocalDateTime tb = (LocalDateTime) b.get("timestamp");
                    return tb.compareTo(ta); // 降序
                })
                .collect(Collectors.toList());
    }

    private void sendNotification(AlertRule rule, Map<String, Object> alert) {
        String notifyType = rule.getNotifyType();
        if (notifyType == null || notifyType.isEmpty()) {
            return;
        }
        
        String message = (String) alert.get("message");
        
        switch (notifyType.toLowerCase()) {
            case "email":
                log.info("[EMAIL] Sending alert to {}: {}", rule.getNotifyTargets(), message);
                break;
            case "sms":
                log.info("[SMS] Sending alert to {}: {}", rule.getNotifyTargets(), message);
                break;
            case "webhook":
                log.info("[WEBHOOK] Sending alert to {}: {}", rule.getNotifyTargets(), message);
                break;
            default:
                log.info("[{}] Sending alert to {}: {}", notifyType, rule.getNotifyTargets(), message);
        }
    }
}
