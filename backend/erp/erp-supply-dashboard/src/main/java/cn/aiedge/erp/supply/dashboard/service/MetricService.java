package cn.aiedge.erp.supply.dashboard.service;

import cn.aiedge.erp.supply.dashboard.entity.MetricConfig;
import cn.aiedge.erp.supply.dashboard.entity.MetricData;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 指标服务接口
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
public interface MetricService {

    /**
     * 获取指标配置
     */
    MetricConfig getMetricConfig(String metricCode);

    /**
     * 获取指标配置（带组织上下文）
     */
    MetricConfig getMetricConfig(String metricCode, Long orgId);

    /**
     * 获取所有激活的指标配置
     */
    List<MetricConfig> getAllActiveMetrics();

    /**
     * 根据类别获取指标配置
     */
    List<MetricConfig> getMetricsByCategory(String category);

    /**
     * 根据类别和组织ID获取指标配置
     */
    List<MetricConfig> getMetricsByCategoryAndOrgId(String category, Long orgId);

    /**
     * 创建或更新指标配置
     */
    MetricConfig saveMetricConfig(MetricConfig metricConfig);

    /**
     * 删除指标配置（逻辑删除）
     */
    void deleteMetricConfig(Long id);

    /**
     * 获取指标数据
     */
    MetricData getMetricData(String metricCode, LocalDate metricDate);

    /**
     * 获取指标数据（带组织上下文）
     */
    MetricData getMetricData(String metricCode, LocalDate metricDate, Long orgId);

    /**
     * 获取指标历史数据
     */
    List<MetricData> getMetricHistory(String metricCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指标历史数据（带组织上下文）
     */
    List<MetricData> getMetricHistory(String metricCode, LocalDate startDate, LocalDate endDate, Long orgId);

    /**
     * 获取最新指标数据
     */
    MetricData getLatestMetricData(String metricCode);

    /**
     * 获取最新指标数据（带组织上下文）
     */
    MetricData getLatestMetricData(String metricCode, Long orgId);

    /**
     * 计算指标值
     */
    MetricData calculateMetric(String metricCode, LocalDate metricDate);

    /**
     * 计算指标值（带组织上下文）
     */
    MetricData calculateMetric(String metricCode, LocalDate metricDate, Long orgId);

    /**
     * 批量计算指标
     */
    List<MetricData> calculateMetrics(List<String> metricCodes, LocalDate metricDate);

    /**
     * 批量计算指标（带组织上下文）
     */
    List<MetricData> calculateMetrics(List<String> metricCodes, LocalDate metricDate, Long orgId);

    /**
     * 重新计算历史数据
     */
    List<MetricData> recalculateHistory(String metricCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指标统计数据
     */
    Map<String, Object> getMetricStatistics(String metricCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指标趋势分析
     */
    Map<String, Object> getMetricTrendAnalysis(String metricCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指标对比分析
     */
    Map<String, Object> compareMetrics(List<String> metricCodes, LocalDate date);

    /**
     * 获取预警指标列表
     */
    List<MetricData> getWarningMetrics(LocalDate date);

    /**
     * 获取预警指标列表（带组织上下文）
     */
    List<MetricData> getWarningMetrics(LocalDate date, Long orgId);

    /**
     * 检查指标状态
     */
    String checkMetricStatus(String metricCode, LocalDate date);

    /**
     * 检查指标状态（带组织上下文）
     */
    String checkMetricStatus(String metricCode, LocalDate date, Long orgId);

    /**
     * 导出指标数据
     */
    byte[] exportMetricData(List<String> metricCodes, LocalDate startDate, LocalDate endDate, String format);

    /**
     * 导入指标数据
     */
    List<MetricData> importMetricData(byte[] data, String format);

    /**
     * 清理过期数据
     */
    int cleanExpiredData(int daysToKeep);

    /**
     * 获取系统健康状态
     */
    Map<String, Object> getSystemHealthStatus();

    /**
     * 获取指标计算队列状态
     */
    Map<String, Object> getCalculationQueueStatus();
}