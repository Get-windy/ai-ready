package cn.aiedge.erp.supply.dashboard.controller;

import cn.aiedge.erp.core.common.ApiResult;
import cn.aiedge.erp.core.common.PageResult;
import cn.aiedge.erp.supply.dashboard.entity.MetricConfig;
import cn.aiedge.erp.supply.dashboard.entity.MetricData;
import cn.aiedge.erp.supply.dashboard.service.MetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 供应链仪表板控制器
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@Slf4j
@RestController
@RequestMapping("/api/supply-dashboard")
@RequiredArgsConstructor
@Tag(name = "供应链仪表板", description = "ERP供应链数据可视化仪表板接口")
public class SupplyDashboardController {

    private final MetricService metricService;

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查仪表板服务健康状况")
    public ApiResult<Map<String, Object>> healthCheck() {
        log.info("执行健康检查");
        return ApiResult.success(Map.of(
            "status", "UP",
            "service", "supply-dashboard",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/metrics/config/{metricCode}")
    @Operation(summary = "获取指标配置", description = "根据指标编码获取配置信息")
    public ApiResult<MetricConfig> getMetricConfig(
            @Parameter(description = "指标编码", required = true)
            @PathVariable String metricCode,
            
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId) {
        
        log.info("获取指标配置: metricCode={}, orgId={}", metricCode, orgId);
        MetricConfig config;
        if (orgId != null) {
            config = metricService.getMetricConfig(metricCode, orgId);
        } else {
            config = metricService.getMetricConfig(metricCode);
        }
        return ApiResult.success(config);
    }

    @GetMapping("/metrics/category/{category}")
    @Operation(summary = "获取类别指标", description = "根据指标类别获取所有指标配置")
    public ApiResult<List<MetricConfig>> getMetricsByCategory(
            @Parameter(description = "指标类别", required = true)
            @PathVariable String category,
            
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId) {
        
        log.info("获取类别指标: category={}, orgId={}", category, orgId);
        List<MetricConfig> metrics;
        if (orgId != null) {
            metrics = metricService.getMetricsByCategoryAndOrgId(category, orgId);
        } else {
            metrics = metricService.getMetricsByCategory(category);
        }
        return ApiResult.success(metrics);
    }

    @GetMapping("/metrics/data/latest/{metricCode}")
    @Operation(summary = "获取最新指标数据", description = "获取指定指标的最新数据")
    public ApiResult<MetricData> getLatestMetricData(
            @Parameter(description = "指标编码", required = true)
            @PathVariable String metricCode,
            
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId) {
        
        log.info("获取最新指标数据: metricCode={}, orgId={}", metricCode, orgId);
        MetricData data;
        if (orgId != null) {
            data = metricService.getLatestMetricData(metricCode, orgId);
        } else {
            data = metricService.getLatestMetricData(metricCode);
        }
        return ApiResult.success(data);
    }

    @GetMapping("/metrics/data/history/{metricCode}")
    @Operation(summary = "获取指标历史数据", description = "获取指定指标的历史数据")
    public ApiResult<List<MetricData>> getMetricHistory(
            @Parameter(description = "指标编码", required = true)
            @PathVariable String metricCode,
            
            @Parameter(description = "开始日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "结束日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId) {
        
        log.info("获取指标历史数据: metricCode={}, startDate={}, endDate={}, orgId={}", 
                metricCode, startDate, endDate, orgId);
        List<MetricData> history;
        if (orgId != null) {
            history = metricService.getMetricHistory(metricCode, startDate, endDate, orgId);
        } else {
            history = metricService.getMetricHistory(metricCode, startDate, endDate);
        }
        return ApiResult.success(history);
    }

    @PostMapping("/metrics/calculate/{metricCode}")
    @Operation(summary = "计算指标", description = "计算指定指标的当前值")
    public ApiResult<MetricData> calculateMetric(
            @Parameter(description = "指标编码", required = true)
            @PathVariable String metricCode,
            
            @Parameter(description = "计算日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId) {
        
        log.info("计算指标: metricCode={}, date={}, orgId={}", metricCode, date, orgId);
        MetricData data;
        if (orgId != null) {
            data = metricService.calculateMetric(metricCode, date, orgId);
        } else {
            data = metricService.calculateMetric(metricCode, date);
        }
        return ApiResult.success(data);
    }

    @GetMapping("/metrics/warnings")
    @Operation(summary = "获取预警指标", description = "获取当前处于预警状态的指标")
    public ApiResult<List<MetricData>> getWarningMetrics(
            @Parameter(description = "日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId) {
        
        log.info("获取预警指标: date={}, orgId={}", date, orgId);
        List<MetricData> warnings;
        if (orgId != null) {
            warnings = metricService.getWarningMetrics(date, orgId);
        } else {
            warnings = metricService.getWarningMetrics(date);
        }
        return ApiResult.success(warnings);
    }

    @GetMapping("/metrics/statistics/{metricCode}")
    @Operation(summary = "获取指标统计", description = "获取指标的统计信息")
    public ApiResult<Map<String, Object>> getMetricStatistics(
            @Parameter(description = "指标编码", required = true)
            @PathVariable String metricCode,
            
            @Parameter(description = "开始日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "结束日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("获取指标统计: metricCode={}, startDate={}, endDate={}", 
                metricCode, startDate, endDate);
        Map<String, Object> statistics = metricService.getMetricStatistics(metricCode, startDate, endDate);
        return ApiResult.success(statistics);
    }

    @GetMapping("/dashboard/overview")
    @Operation(summary = "获取仪表板概览", description = "获取供应链仪表板概览数据")
    public ApiResult<Map<String, Object>> getDashboardOverview(
            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId,
            
            @Parameter(description = "日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("获取仪表板概览: orgId={}, date={}", orgId, date);
        
        // 模拟概览数据
        Map<String, Object> overview = Map.of(
            "supplyChainHealth", Map.of(
                "score", 85.5,
                "status", "GOOD",
                "trend", "UP",
                "change", 2.3
            ),
            "inventoryMetrics", Map.of(
                "turnoverRate", 8.5,
                "daysOnHand", 32.5,
                "safetyStockLevel", 92.3,
                "slowMovingItems", 15
            ),
            "purchaseMetrics", Map.of(
                "orderFulfillmentRate", 96.8,
                "supplierOnTimeRate", 98.2,
                "costSavingsRate", 5.7,
                "activeSuppliers", 42
            ),
            "logisticsMetrics", Map.of(
                "deliveryOnTimeRate", 99.1,
                "transportCostRatio", 2.8,
                "warehouseUtilization", 78.5,
                "averageDeliveryTime", 1.8
            ),
            "topAlerts", List.of(
                Map.of("id", 1, "metric", "库存周转率", "level", "WARNING", "message", "低于目标值10%"),
                Map.of("id", 2, "metric", "采购成本", "level", "INFO", "message", "接近预警阈值"),
                Map.of("id", 3, "metric", "供应商A交货准时率", "level", "CRITICAL", "message", "连续3天低于90%")
            ),
            "lastUpdated", System.currentTimeMillis()
        );
        
        return ApiResult.success(overview);
    }

    @GetMapping("/system/status")
    @Operation(summary = "获取系统状态", description = "获取仪表板系统状态信息")
    public ApiResult<Map<String, Object>> getSystemStatus() {
        log.info("获取系统状态");
        Map<String, Object> systemStatus = metricService.getSystemHealthStatus();
        return ApiResult.success(systemStatus);
    }

    @PostMapping("/data/cleanup")
    @Operation(summary = "清理过期数据", description = "清理指定天数之前的过期数据")
    public ApiResult<Integer> cleanupExpiredData(
            @Parameter(description = "保留天数", required = true)
            @RequestParam int daysToKeep) {
        
        log.info("清理过期数据: daysToKeep={}", daysToKeep);
        int cleanedCount = metricService.cleanExpiredData(daysToKeep);
        return ApiResult.success(cleanedCount);
    }
}