package cn.aiedge.erp.metrics.service;

import cn.aiedge.erp.metrics.dto.MetricAlertDTO;
import cn.aiedge.erp.metrics.dto.MetricValueDTO;

import java.util.List;

/**
 * 指标告警服务接口
 * 集成告警规则引擎，支持指标阈值监控和告警
 */
public interface MetricsAlertService {
    
    /**
     * 检查指标是否触发告警
     */
    List<MetricAlertDTO> checkMetricAlerts(MetricValueDTO metricValue);
    
    /**
     * 批量检查指标告警
     */
    List<MetricAlertDTO> checkBatchMetricAlerts(List<MetricValueDTO> metricValues);
    
    /**
     * 获取当前活跃告警
     */
    List<MetricAlertDTO> getActiveAlerts();
    
    /**
     * 获取指标告警历史
     */
    List<MetricAlertDTO> getAlertHistory(String metricCode, int days);
    
    /**
     * 确认告警
     */
    void acknowledgeAlert(Long alertId, String operator);
    
    /**
     * 清除告警
     */
    void clearAlert(Long alertId, String operator);
    
    /**
     * 启动告警监控
     */
    void startAlertMonitoring();
    
    /**
     * 停止告警监控
     */
    void stopAlertMonitoring();
}
