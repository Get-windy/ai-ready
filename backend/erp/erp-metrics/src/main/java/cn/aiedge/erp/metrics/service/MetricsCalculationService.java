package cn.aiedge.erp.metrics.service;

import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.entity.MetricData;
import cn.aiedge.erp.metrics.enums.MetricType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 指标计算服务接口
 */
public interface MetricsCalculationService {
    
    /**
     * 计算指定指标的值
     */
    BigDecimal calculateMetric(BusinessMetric metric, LocalDateTime calculationTime);
    
    /**
     * 批量计算指定类型的所有指标
     */
    Map<String, BigDecimal> calculateMetricsByType(MetricType type, LocalDateTime calculationTime);
    
    /**
     * 计算订单相关指标
     */
    BigDecimal calculateOrderMetric(String metricCode, LocalDateTime calculationTime);
    
    /**
     * 计算库存相关指标
     */
    BigDecimal calculateInventoryMetric(String metricCode, LocalDateTime calculationTime);
    
    /**
     * 计算用户相关指标
     */
    BigDecimal calculateUserMetric(String metricCode, LocalDateTime calculationTime);
    
    /**
     * 计算销售相关指标
     */
    BigDecimal calculateSalesMetric(String metricCode, LocalDateTime calculationTime);
    
    /**
     * 保存指标数据
     */
    void saveMetricData(MetricData metricData);
    
    /**
     * 批量保存指标数据
     */
    void batchSaveMetricData(List<MetricData> metricDataList);
}
