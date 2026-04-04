package cn.aiedge.monitor.service;

import cn.aiedge.monitor.model.AlertRule;

import java.util.List;
import java.util.Map;

/**
 * 告警规则服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface AlertRuleService {

    /**
     * 创建告警规则
     *
     * @param rule 告警规则
     * @return 创建的规则
     */
    AlertRule createRule(AlertRule rule);

    /**
     * 更新告警规则
     *
     * @param rule 告警规则
     * @return 更新后的规则
     */
    AlertRule updateRule(AlertRule rule);

    /**
     * 删除告警规则
     *
     * @param ruleId 规则ID
     * @return 是否成功
     */
    boolean deleteRule(Long ruleId);

    /**
     * 获取告警规则
     *
     * @param ruleId 规则ID
     * @return 告警规则
     */
    AlertRule getRule(Long ruleId);

    /**
     * 获取所有启用的规则
     *
     * @param tenantId 租户ID
     * @return 规则列表
     */
    List<AlertRule> getEnabledRules(Long tenantId);

    /**
     * 检查指标并触发告警
     *
     * @param metricName 指标名称
     * @param value      指标值
     * @param tenantId   租户ID
     * @return 触发的告警列表
     */
    List<Map<String, Object>> checkAndAlert(String metricName, double value, Long tenantId);

    /**
     * 启用规则
     *
     * @param ruleId 规则ID
     * @return 是否成功
     */
    boolean enableRule(Long ruleId);

    /**
     * 禁用规则
     *
     * @param ruleId 规则ID
     * @return 是否成功
     */
    boolean disableRule(Long ruleId);

    /**
     * 获取告警历史
     *
     * @param tenantId 租户ID
     * @param hours    小时数
     * @return 告警历史
     */
    List<Map<String, Object>> getAlertHistory(Long tenantId, int hours);
}
