package cn.aiedge.statistics.service;

import cn.aiedge.report.dto.CustomerStatsDTO;
import cn.aiedge.report.dto.OrderStatsDTO;
import cn.aiedge.report.dto.SalesStatsDTO;
import cn.aiedge.report.dto.StatsQueryRequest;

/**
 * 统计分析服务接口
 */
public interface IStatisticsService {

    /**
     * 客户统计
     */
    CustomerStatsDTO getCustomerStats(StatsQueryRequest request);

    /**
     * 订单统计
     */
    OrderStatsDTO getOrderStats(StatsQueryRequest request);

    /**
     * 销售统计
     */
    SalesStatsDTO getSalesStats(StatsQueryRequest request);

    /**
     * 客户趋势分析
     */
    CustomerStatsDTO getCustomerTrend(StatsQueryRequest request);

    /**
     * 订单趋势分析
     */
    OrderStatsDTO getOrderTrend(StatsQueryRequest request);
}
