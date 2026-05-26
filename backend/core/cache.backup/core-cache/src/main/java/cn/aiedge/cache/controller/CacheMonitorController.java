package cn.aiedge.cache.controller;

import cn.aiedge.cache.service.CacheMetricsService;
import cn.aiedge.cache.service.CacheMetricsService.CacheMetrics;
import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控控制器
 * 提供缓存性能指标查询接口
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/monitor")
@RequiredArgsConstructor
@Tag(name = "缓存监控", description = "缓存性能指标查询")
public class CacheMonitorController {

    private final CacheMetricsService metricsService;

    /**
     * 获取所有缓存指标
     */
    @GetMapping("/metrics")
    @Operation(summary = "获取所有缓存指标")
    public Result<Map<String, Object>> getAllMetrics() {
        Map<String, CacheMetrics> allMetrics = metricsService.getAllMetrics();
        
        Map<String, Object> result = new HashMap<>();
        result.put("caches", allMetrics);
        
        // 计算总体统计
        long totalHits = allMetrics.values().stream().mapToLong(CacheMetrics::getHitCount).sum();
        long totalMisses = allMetrics.values().stream().mapToLong(CacheMetrics::getMissCount).sum();
        double totalHitRate = totalHits + totalMisses > 0 ? 
                (double) totalHits / (totalHits + totalMisses) : 0;
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHits", totalHits);
        summary.put("totalMisses", totalMisses);
        summary.put("totalHitRate", String.format("%.2f%%", totalHitRate * 100));
        summary.put("cacheCount", allMetrics.size());
        
        result.put("summary", summary);
        
        return Result.success(result);
    }

    /**
     * 获取指定缓存指标
     */
    @GetMapping("/metrics/{cacheName}")
    @Operation(summary = "获取指定缓存指标")
    public Result<CacheMetrics> getMetrics(@PathVariable String cacheName) {
        CacheMetrics metrics = metricsService.getMetrics(cacheName);
        if (metrics == null) {
            return Result.error("缓存不存在: " + cacheName);
        }
        return Result.success(metrics);
    }

    /**
     * 获取缓存命中率
     */
    @GetMapping("/hit-rate/{cacheName}")
    @Operation(summary = "获取缓存命中率")
    public Result<Map<String, Object>> getHitRate(@PathVariable String cacheName) {
        double hitRate = metricsService.getHitRate(cacheName);
        Map<String, Object> result = new HashMap<>();
        result.put("cacheName", cacheName);
        result.put("hitRate", String.format("%.2f%%", hitRate * 100));
        return Result.success(result);
    }

    /**
     * 获取缓存QPS
     */
    @GetMapping("/qps/{cacheName}")
    @Operation(summary = "获取缓存QPS")
    public Result<Map<String, Object>> getQps(@PathVariable String cacheName) {
        double qps = metricsService.getAverageQps(cacheName);
        Map<String, Object> result = new HashMap<>();
        result.put("cacheName", cacheName);
        result.put("qps", String.format("%.2f", qps));
        return Result.success(result);
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "获取缓存健康状态")
    public Result<Map<String, Object>> getHealthStatus() {
        Map<String, CacheMetrics> allMetrics = metricsService.getAllMetrics();
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("cacheCount", allMetrics.size());
        
        // 检查各缓存健康状态
        Map<String, String> cacheHealth = new HashMap<>();
        for (Map.Entry<String, CacheMetrics> entry : allMetrics.entrySet()) {
            String cacheName = entry.getKey();
            CacheMetrics metrics = entry.getValue();
            
            // 命中率低于50%视为警告
            if (metrics.getHitRate() < 0.5) {
                cacheHealth.put(cacheName, "WARNING: low hit rate");
            } else {
                cacheHealth.put(cacheName, "HEALTHY");
            }
        }
        health.put("caches", cacheHealth);
        
        return Result.success(health);
    }
}
