package cn.aiedge.erp.monitor.controller;

import cn.aiedge.erp.monitor.dto.MetricDashboardDTO;
import cn.aiedge.erp.monitor.dto.MetricQueryDTO;
import cn.aiedge.erp.monitor.dto.MetricRealTimeDTO;
import cn.aiedge.erp.monitor.entity.BusinessMetric;
import cn.aiedge.erp.monitor.entity.MetricDefinition;
import cn.aiedge.erp.monitor.service.BusinessMetricService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 业务指标监控控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/monitor")
@RequiredArgsConstructor
@Tag(name = "业务指标监控", description = "业务指标实时监控和管理接口")
public class BusinessMetricController {

    private final BusinessMetricService businessMetricService;

    // ==================== 实时指标 ====================

    @GetMapping("/realtime")
    @Operation(summary = "获取实时指标")
    public List<MetricRealTimeDTO> getRealTimeMetrics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false, defaultValue = "order,inventory,user,sales") String metricTypes) {
        List<String> types = Arrays.asList(metricTypes.split(","));
        return businessMetricService.getRealTimeMetrics(tenantId, types);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "获取指标仪表盘")
    public MetricDashboardDTO getDashboard(
            @RequestParam(required = false) Long tenantId) {
        return businessMetricService.getDashboard(tenantId);
    }

    // ==================== 指标查询 ====================

    @PostMapping("/query")
    @Operation(summary = "查询指标列表")
    public IPage<BusinessMetric> queryMetrics(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestBody MetricQueryDTO query) {
        Page<BusinessMetric> page = new Page<>(pageNum, pageSize);
        return businessMetricService.queryMetrics(page, query);
    }

    @GetMapping("/history")
    @Operation(summary = "获取历史指标")
    public List<BusinessMetric> getHistoryMetrics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam String metricCode,
            @RequestParam(defaultValue = "1h") String period,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        return businessMetricService.getHistoryMetrics(tenantId, metricCode, period, startTime, endTime);
    }

    @GetMapping("/trend/{metricCode}")
    @Operation(summary = "获取指标趋势")
    public Map<String, Object> getMetricTrend(
            @PathVariable String metricCode,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "1h") String period,
            @RequestParam(defaultValue = "24") int hours) {
        return businessMetricService.getMetricTrend(tenantId, metricCode, period, hours);
    }

    // ==================== 业务指标 ====================

    @GetMapping("/core")
    @Operation(summary = "获取核心业务指标")
    public Map<String, Object> getCoreBusinessMetrics(
            @RequestParam(required = false) Long tenantId) {
        return businessMetricService.getCoreBusinessMetrics(tenantId);
    }

    @GetMapping("/order")
    @Operation(summary = "获取订单指标")
    public Map<String, Object> getOrderMetrics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "realtime") String period) {
        return businessMetricService.getOrderMetrics(tenantId, period);
    }

    @GetMapping("/inventory")
    @Operation(summary = "获取库存指标")
    public Map<String, Object> getInventoryMetrics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "realtime") String period) {
        return businessMetricService.getInventoryMetrics(tenantId, period);
    }

    @GetMapping("/user")
    @Operation(summary = "获取用户指标")
    public Map<String, Object> getUserMetrics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "realtime") String period) {
        return businessMetricService.getUserMetrics(tenantId, period);
    }

    @GetMapping("/sales")
    @Operation(summary = "获取销售指标")
    public Map<String, Object> getSalesMetrics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "realtime") String period) {
        return businessMetricService.getSalesMetrics(tenantId, period);
    }

    // ==================== 指标定义管理 ====================

    @GetMapping("/definitions")
    @Operation(summary = "获取指标定义列表")
    public List<MetricDefinition> getMetricDefinitions(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String metricType) {
        return businessMetricService.getMetricDefinitions(tenantId, metricType);
    }

    @PostMapping("/definitions")
    @Operation(summary = "保存指标定义")
    public MetricDefinition saveMetricDefinition(
            @RequestBody MetricDefinition definition) {
        return businessMetricService.saveMetricDefinition(definition);
    }

    @DeleteMapping("/definitions/{id}")
    @Operation(summary = "删除指标定义")
    public Map<String, Object> deleteMetricDefinition(@PathVariable Long id) {
        boolean success = businessMetricService.deleteMetricDefinition(id);
        return Map.of("success", success);
    }

    // ==================== 指标刷新 ====================

    @PostMapping("/refresh")
    @Operation(summary = "刷新指标数据")
    public Map<String, Object> refreshMetrics(
            @RequestParam(required = false) Long tenantId) {
        businessMetricService.refreshMetrics(tenantId);
        return Map.of("success", true, "message", "指标数据刷新成功");
    }
}
