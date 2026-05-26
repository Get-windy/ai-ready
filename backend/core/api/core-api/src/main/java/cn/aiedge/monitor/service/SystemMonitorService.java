package cn.aiedge.monitor.service;

import cn.aiedge.monitor.model.SystemMetrics;

import java.util.List;
import java.util.Map;

/**
 * 系统监控服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SystemMonitorService {

    /**
     * 获取当前系统指标
     *
     * @return 系统指标
     */
    SystemMetrics getCurrentMetrics();

    /**
     * 获取历史指标
     *
     * @param hours 小时数
     * @return 指标列表
     */
    List<SystemMetrics> getHistoryMetrics(int hours);

    /**
     * 获取指标趋势
     *
     * @param metricName 指标名称
     * @param hours      小时数
     * @return 趋势数据
     */
    Map<String, Object> getMetricTrend(String metricName, int hours);

    /**
     * 获取系统概览
     *
     * @return 系统概览数据
     */
    Map<String, Object> getSystemOverview();

    /**
     * 检查系统健康状态
     *
     * @return 健康状态
     */
    Map<String, Object> checkHealth();

    /**
     * 获取JVM信息
     *
     * @return JVM信息
     */
    Map<String, Object> getJvmInfo();

    /**
     * 获取线程信息
     *
     * @return 线程信息
     */
    Map<String, Object> getThreadInfo();

    /**
     * 获取内存信息
     *
     * @return 内存信息
     */
    Map<String, Object> getMemoryInfo();

    /**
     * 执行垃圾回收
     */
    void performGc();
}
