package cn.aiedge.statistics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.report.dto.*;
import cn.aiedge.report.service.IStatisticsService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据统计控制器
 */
@Tag(name = "数据统计分析")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final IStatisticsService statisticsService;

    @Operation(summary = "客户统计")
    @GetMapping("/customer")
    @SaCheckLogin
    public ApiResponse<CustomerStatsDTO> getCustomerStats(StatsQueryRequest request) {
        CustomerStatsDTO stats = statisticsService.getCustomerStats(request);
        return ApiResponse.ok(stats);
    }

    @Operation(summary = "订单统计")
    @GetMapping("/order")
    @SaCheckLogin
    public ApiResponse<OrderStatsDTO> getOrderStats(StatsQueryRequest request) {
        OrderStatsDTO stats = statisticsService.getOrderStats(request);
        return ApiResponse.ok(stats);
    }

    @Operation(summary = "销售统计")
    @GetMapping("/sales")
    @SaCheckLogin
    public ApiResponse<SalesStatsDTO> getSalesStats(StatsQueryRequest request) {
        SalesStatsDTO stats = statisticsService.getSalesStats(request);
        return ApiResponse.ok(stats);
    }

    @Operation(summary = "客户趋势分析")
    @GetMapping("/customer/trend")
    @SaCheckLogin
    public ApiResponse<CustomerStatsDTO> getCustomerTrend(StatsQueryRequest request) {
        CustomerStatsDTO stats = statisticsService.getCustomerTrend(request);
        return ApiResponse.ok(stats);
    }

    @Operation(summary = "订单趋势分析")
    @GetMapping("/order/trend")
    @SaCheckLogin
    public ApiResponse<OrderStatsDTO> getOrderTrend(StatsQueryRequest request) {
        OrderStatsDTO stats = statisticsService.getOrderTrend(request);
        return ApiResponse.ok(stats);
    }
}
