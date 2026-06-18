package cn.aiedge.erp.monitor.service;

import cn.aiedge.erp.monitor.entity.BusinessMetric;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标采集服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface MetricCollectorService {

    /**
     * 采集订单指标
     */
    List<BusinessMetric> collectOrderMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 采集库存指标
     */
    List<BusinessMetric> collectInventoryMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 采集用户指标
     */
    List<BusinessMetric> collectUserMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 采集销售指标
     */
    List<BusinessMetric> collectSalesMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 采集采购指标
     */
    List<BusinessMetric> collectPurchaseMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 采集财务指标
     */
    List<BusinessMetric> collectFinanceMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 采集所有指标
     */
    List<BusinessMetric> collectAllMetrics(Long tenantId, LocalDateTime statTime);

    /**
     * 实时计算指标
     */
    BusinessMetric calculateRealTimeMetric(String metricCode, Long tenantId);

    /**
     * 批量采集并保存指标
     */
    int batchCollectAndSave(Long tenantId, List<String> metricTypes);
}
