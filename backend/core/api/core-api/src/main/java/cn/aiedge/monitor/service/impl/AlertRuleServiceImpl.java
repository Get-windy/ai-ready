package cn.aiedge.monitor.service.impl;

import cn.aiedge.monitor.mapper.AlertHistoryMapper;
import cn.aiedge.monitor.mapper.AlertRuleMapper;
import cn.aiedge.monitor.model.AlertHistory;
import cn.aiedge.monitor.model.AlertRule;
import cn.aiedge.monitor.service.AlertRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertHistoryMapper alertHistoryMapper;

    @Override
    public AlertRule createRule(AlertRule rule) {
        LocalDateTime now = LocalDateTime.now();
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        rule.setEnabled(true);
        rule.setDeleted(0);
        alertRuleMapper.insert(rule);
        log.info("创建告警规则: {}", rule.getRuleName());
        return rule;
    }

    @Override
    public AlertRule updateRule(AlertRule rule) {
        AlertRule existing = alertRuleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Rule not found: " + rule.getId());
        }
        rule.setCreateTime(existing.getCreateTime());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(existing.getDeleted());
        alertRuleMapper.updateById(rule);
        log.info("更新告警规则: {}", rule.getId());
        return rule;
    }

    @Override
    public boolean deleteRule(Long ruleId) {
        AlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule != null) {
            rule.setDeleted(1);
            rule.setUpdateTime(LocalDateTime.now());
            alertRuleMapper.updateById(rule);
            log.info("删除告警规则: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public AlertRule getRule(Long ruleId) {
        return alertRuleMapper.selectById(ruleId);
    }

    @Override
    public List<AlertRule> getEnabledRules(Long tenantId) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getDeleted, 0)
                .eq(AlertRule::getEnabled, true)
                .eq(tenantId != null, AlertRule::getTenantId, tenantId);
        return alertRuleMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> checkAndAlert(String metricName, double value, Long tenantId) {
        List<Map<String, Object>> triggeredAlerts = new ArrayList<>();

        List<AlertRule> rules = getEnabledRules(tenantId).stream()
                .filter(r -> r.getMetricName().equals(metricName))
                .collect(Collectors.toList());

        for (AlertRule rule : rules) {
            if (rule.isTriggered(value)) {
                String message = String.format("%s: %s = %.2f (threshold: %s %.2f)",
                        rule.getSeverity(), metricName, value, rule.getOperator(), rule.getThreshold());

                Map<String, Object> alert = new HashMap<>();
                alert.put("ruleId", rule.getId());
                alert.put("ruleName", rule.getRuleName());
                alert.put("metricName", metricName);
                alert.put("value", value);
                alert.put("threshold", rule.getThreshold());
                alert.put("severity", rule.getSeverity());
                alert.put("timestamp", LocalDateTime.now());
                alert.put("message", message);
                triggeredAlerts.add(alert);

                // 保存到数据库
                AlertHistory history = new AlertHistory();
                history.setRuleId(rule.getId());
                history.setRuleName(rule.getRuleName());
                history.setMetricName(metricName);
                history.setMetricValue(value);
                history.setThreshold(rule.getThreshold());
                history.setOperator(rule.getOperator());
                history.setSeverity(rule.getSeverity());
                history.setMessage(message);
                history.setTenantId(tenantId);
                history.setAlertTime(LocalDateTime.now());
                history.setAcknowledged(false);
                history.setResolved(false);
                alertHistoryMapper.insert(history);

                // 模拟发送通知
                sendNotification(rule, alert);
            }
        }

        return triggeredAlerts;
    }

    @Override
    public boolean enableRule(Long ruleId) {
        AlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule != null) {
            rule.setEnabled(true);
            rule.setUpdateTime(LocalDateTime.now());
            alertRuleMapper.updateById(rule);
            log.info("启用告警规则: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public boolean disableRule(Long ruleId) {
        AlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule != null) {
            rule.setEnabled(false);
            rule.setUpdateTime(LocalDateTime.now());
            alertRuleMapper.updateById(rule);
            log.info("禁用告警规则: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> getAlertHistory(Long tenantId, int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        LambdaQueryWrapper<AlertHistory> wrapper = new LambdaQueryWrapper<AlertHistory>()
                .eq(tenantId != null, AlertHistory::getTenantId, tenantId)
                .ge(AlertHistory::getAlertTime, cutoff)
                .orderByDesc(AlertHistory::getAlertTime);

        List<AlertHistory> histories = alertHistoryMapper.selectList(wrapper);

        return histories.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("ruleId", h.getRuleId());
            map.put("ruleName", h.getRuleName());
            map.put("metricName", h.getMetricName());
            map.put("value", h.getMetricValue());
            map.put("threshold", h.getThreshold());
            map.put("severity", h.getSeverity());
            map.put("timestamp", h.getAlertTime());
            map.put("message", h.getMessage());
            return map;
        }).collect(Collectors.toList());
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
