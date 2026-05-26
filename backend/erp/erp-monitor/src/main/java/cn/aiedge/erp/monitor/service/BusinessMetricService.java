package cn.aiedge.erp.monitor.service;

import cn.aiedge.erp.monitor.dto.MetricDashboardDTO;
import cn.aiedge.erp.monitor.dto.MetricQueryDTO;
import cn.aiedge.erp.monitor.dto.MetricRealTimeDTO;
import cn.aiedge.erp.monitor.entity.BusinessMetric;
import cn.aiedge.erp.monitor.entity.MetricDefinition;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 业务指标服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface BusinessMetricService {

    /**
     * 获取实时指标
     */
    List<MetricRealTimeDTO> getRealTimeMetrics(Long tenantId, List<String> metricTypes);

    /**
     * 获取指标仪表盘数据
     */
    MetricDashboardDTO getDashboard(Long tenantId);

    /**
     * 查询指标列表
     */
    IPage<BusinessMetric> queryMetrics(Page<BusinessMetric> page, MetricQueryDTO query);

    /**
     * 获取历史指标数据
     */
    List<BusinessMetric> getHistoryMetrics(Long tenantId, String metricCode, 
                                           String period, LocalDateTime startTime, 
                                           LocalDateTime endTime);

    /**
     * 获取指标趋势
     */
    Map<String, Object> getMetricTrend(Long tenantId, String metricCode, 
                                       String period, int hours);

    /**
     * 刷新指标数据
     */
    void refreshMetrics(Long tenantId);

    /**
     * 获取指标定义
     */
    List<MetricDefinition> getMetricDefinitions(Long tenantId, String metricType);

    /**
     * 保存指标定义
     */
    MetricDefinition saveMetricDefinition(MetricDefinition definition);

    /**
     * 删除指标定义
     */
    boolean deleteMetricDefinition(Long id);

    /**
     * 获取核心业务指标
     */
    Map<String, Object> getCoreBusinessMetrics(Long tenantId);

    /**
     * 获取订单指标
     */
    Map<String, Object> getOrderMetrics(Long tenantId, String period);

    /**
     * 获取库存指标
     */
    Map<String, Object> getInventoryMetrics(Long tenantId, String period);

    /**
     * 获取用户指标
     */
    Map<String, Object> getUserMetrics(Long tenantId, String period);

    /**
     * 获取销售指标
     */
    Map<String, Object> getSalesMetrics(Long tenantId, String period);
}
