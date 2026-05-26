package cn.aiedge.erp.metrics.service;

import cn.aiedge.erp.metrics.dto.*;
import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.enums.MetricType;

import java.util.List;

/**
 * 业务指标服务接口
 */
public interface MetricsService {
    
    /**
     * 获取仪表盘指标数据
     */
    DashboardMetricsDTO getDashboardMetrics();
    
    /**
     * 按类型获取指标
     */
    List<MetricValueDTO> getMetricsByType(MetricType type);
    
    /**
     * 获取指标历史数据
     */
    MetricHistoryDTO getMetricHistory(MetricQueryRequest request);
    
    /**
     * 获取指标当前值
     */
    MetricValueDTO getCurrentMetric(String metricCode);
    
    /**
     * 批量获取指标值
     */
    List<MetricValueDTO> getCurrentMetrics(List<String> metricCodes);
    
    /**
     * 获取所有活跃指标
     */
    List<BusinessMetric> getAllActiveMetrics();
    
    /**
     * 刷新指标数据（手动触发）
     */
    void refreshMetrics();
}
