package cn.aiedge.erp.metrics.controller;

import cn.aiedge.erp.metrics.dto.*;
import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.enums.MetricType;
import cn.aiedge.erp.metrics.service.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务指标监控Controller
 */
@RestController
@RequestMapping("/api/erp/metrics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MetricsController {
    
    private final MetricsService metricsService;
    
    /**
     * 获取仪表盘指标数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics() {
        log.info("Getting dashboard metrics");
        DashboardMetricsDTO dashboard = metricsService.getDashboardMetrics();
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * 按类型获取指标
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<MetricValueDTO>> getMetricsByType(@PathVariable String type) {
        log.info("Getting metrics by type: {}", type);
        try {
            MetricType metricType = MetricType.fromCode(type);
            List<MetricValueDTO> metrics = metricsService.getMetricsByType(metricType);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取指标当前值
     */
    @GetMapping("/current/{metricCode}")
    public ResponseEntity<MetricValueDTO> getCurrentMetric(@PathVariable String metricCode) {
        log.info("Getting current metric: {}", metricCode);
        try {
            MetricValueDTO metric = metricsService.getCurrentMetric(metricCode);
            return ResponseEntity.ok(metric);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 批量获取指标当前值
     */
    @PostMapping("/current/batch")
    public ResponseEntity<List<MetricValueDTO>> getCurrentMetricsBatch(@RequestBody List<String> metricCodes) {
        log.info("Getting current metrics batch: {}", metricCodes);
        List<MetricValueDTO> metrics = metricsService.getCurrentMetrics(metricCodes);
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 获取指标历史数据
     */
    @PostMapping("/history")
    public ResponseEntity<MetricHistoryDTO> getMetricHistory(@Valid @RequestBody MetricQueryRequest request) {
        log.info("Getting metric history for: {}", request.getMetricCode());
        try {
            MetricHistoryDTO history = metricsService.getMetricHistory(request);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取所有活跃指标
     */
    @GetMapping("/list")
    public ResponseEntity<List<BusinessMetric>> getAllActiveMetrics() {
        log.info("Getting all active metrics");
        List<BusinessMetric> metrics = metricsService.getAllActiveMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 手动刷新指标数据
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshMetrics() {
        log.info("Manual refresh metrics triggered");
        metricsService.refreshMetrics();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Metrics refresh triggered successfully");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取支持的指标类型
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, String>> getMetricTypes() {
        log.info("Getting metric types");
        Map<String, String> types = new HashMap<>();
        for (MetricType type : MetricType.values()) {
            types.put(type.getCode(), type.getDescription());
        }
        return ResponseEntity.ok(types);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "erp-metrics");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
