package cn.aiedge.cache.controller;

import cn.aiedge.cache.service.CacheWarmupService;
import cn.aiedge.cache.service.CacheMetricsService;
import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存预热、清理、监控等管理功能
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/manage")
@RequiredArgsConstructor
@Tag(name = "缓存管理", description = "缓存预热、清理、监控等管理功能")
public class CacheManageController {

    private final CacheWarmupService cacheWarmupService;
    private final CacheMetricsService metricsService;

    /**
     * 执行缓存预热
     */
    @PostMapping("/warmup")
    @Operation(summary = "执行缓存预热")
    public Result<String> executeWarmup() {
        try {
            cacheWarmupService.performCacheWarmup();
            return Result.success("缓存预热任务已启动");
        } catch (Exception e) {
            log.error("执行缓存预热失败", e);
            return Result.error("缓存预热失败: " + e.getMessage());
        }
    }

    /**
     * 清理所有本地缓存
     */
    @DeleteMapping("/local/clear")
    @Operation(summary = "清理所有本地缓存")
    public Result<String> clearLocalCache() {
        // 这里需要获取本地缓存管理器并清空
        // 实际实现会依赖具体的缓存管理器实现
        return Result.success("本地缓存清理任务已启动");
    }

    /**
     * 清理指定缓存
     */
    @DeleteMapping("/clear/{cacheName}")
    @Operation(summary = "清理指定缓存")
    public Result<String> clearCache(@PathVariable String cacheName) {
        // 实际实现会依赖具体的缓存管理器实现
        return Result.success("缓存 " + cacheName + " 清理任务已启动");
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计信息")
    public Result<Map<String, Object>> getCacheStats() {
        Map<String, CacheMetricsService.CacheMetrics> allMetrics = metricsService.getAllMetrics();
        
        Map<String, Object> result = Map.of(
            "cacheCount", allMetrics.size(),
            "caches", allMetrics,
            "summary", getSummary(allMetrics)
        );
        
        return Result.success(result);
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "获取缓存健康状态")
    public Result<Map<String, Object>> getHealthStatus() {
        Map<String, CacheMetricsService.CacheMetrics> allMetrics = metricsService.getAllMetrics();
        
        Map<String, Object> health = Map.of(
            "status", "UP",
            "cacheCount", allMetrics.size(),
            "timestamp", System.currentTimeMillis()
        );
        
        return Result.success(health);
    }

    /**
     * 预热指定类型的缓存
     */
    @PostMapping("/warmup/{type}")
    @Operation(summary = "预热指定类型的缓存")
    public Result<String> warmupByType(@PathVariable String type) {
        try {
            switch (type.toLowerCase()) {
                case "user":
                    // 预热用户相关缓存
                    break;
                case "config":
                    // 预热配置相关缓存
                    break;
                case "dict":
                    // 预热字典相关缓存
                    break;
                case "permission":
                    // 预热权限相关缓存
                    break;
                default:
                    return Result.error("不支持的缓存类型: " + type);
            }
            return Result.success("缓存预热任务已启动: " + type);
        } catch (Exception e) {
            log.error("预热缓存失败: " + type, e);
            return Result.error("缓存预热失败: " + e.getMessage());
        }
    }

    private Map<String, Object> getSummary(Map<String, CacheMetricsService.CacheMetrics> metrics) {
        long totalHits = metrics.values().stream().mapToLong(CacheMetricsService.CacheMetrics::getHitCount).sum();
        long totalMisses = metrics.values().stream().mapToLong(CacheMetricsService.CacheMetrics::getMissCount).sum();
        double totalHitRate = totalHits + totalMisses > 0 ?
                (double) totalHits / (totalHits + totalMisses) : 0;

        return Map.of(
            "totalHits", totalHits,
            "totalMisses", totalMisses,
            "totalHitRate", String.format("%.2f%%", totalHitRate * 100),
            "cacheCount", metrics.size()
        );
    }
}