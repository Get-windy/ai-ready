package cn.aiedge.cache.controller;

import cn.aiedge.cache.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存管理控制器
 * 提供缓存监控与管理接口
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "缓存管理", description = "缓存监控与管理功能")
public class CacheManageController {

    @Autowired(required = false)
    private CacheService cacheService;

    /**
     * 模拟的缓存区域数据
     */
    private static final List<Map<String, Object>> REGIONS = List.of(
            createRegion("user:info", 850, 45.2, 91.2, 1800),
            createRegion("user:perms", 420, 22.8, 88.5, 3600),
            createRegion("sys:config", 180, 12.5, 96.8, 7200),
            createRegion("dict:data", 560, 28.3, 93.1, 3600),
            createRegion("menu:data", 120, 8.5, 85.4, 7200),
            createRegion("dept:data", 95, 6.2, 90.7, 3600),
            createRegion("product:data", 280, 15.8, 82.3, 1800),
            createRegion("customer:data", 190, 10.6, 79.6, 1800),
            createRegion("order:data", 145, 6.4, 86.2, 300)
    );

    private static Map<String, Object> createRegion(String name, int keyCount, double memory, double hitRate, int ttl) {
        Map<String, Object> region = new LinkedHashMap<>();
        region.put("name", name);
        region.put("keyCount", keyCount);
        region.put("memory", String.format("%.1f MB", memory));
        region.put("hitRate", hitRate);
        region.put("ttl", ttl);
        return region;
    }

    // ==================== 缓存概览 ====================

    @GetMapping("/status")
    @Operation(summary = "获取缓存概览统计")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        int totalKeys = REGIONS.stream().mapToInt(r -> (int) r.get("keyCount")).sum();
        double totalMemory = REGIONS.stream()
                .mapToDouble(r -> Double.parseDouble(((String) r.get("memory")).replace(" MB", "")))
                .sum();
        double avgHitRate = REGIONS.stream()
                .mapToDouble(r -> (double) r.get("hitRate"))
                .average()
                .orElse(0.0);
        int expiredKeys = 32;

        List<Map<String, Object>> regions = REGIONS.stream()
                .map(r -> {
                    Map<String, Object> copy = new LinkedHashMap<>(r);
                    // 确保内存字段是数值类型，便于前端展示
                    copy.put("memoryValue", Double.parseDouble(((String) r.get("memory")).replace(" MB", "")));
                    return copy;
                })
                .collect(Collectors.toList());

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalSize", String.format("%.1f MB", totalMemory));
        status.put("totalKeys", totalKeys);
        status.put("hitRate", Math.round(avgHitRate * 10.0) / 10.0);
        status.put("expiredKeys", expiredKeys);
        status.put("regions", regions);

        return ResponseEntity.ok(Map.of("code", 200, "data", status, "message", "ok"));
    }

    // ==================== 缓存区域管理 ====================

    @DeleteMapping("/region/{name}")
    @Operation(summary = "清除指定缓存区域")
    public ResponseEntity<Map<String, Object>> clearRegion(@PathVariable String name) {
        // 模拟清除：如果 cacheService 可用则执行真实清除
        if (cacheService != null) {
            cacheService.deleteByPattern(name + ":*");
        }

        boolean exists = REGIONS.stream().anyMatch(r -> r.get("name").equals(name));
        if (!exists) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "缓存区域不存在: " + name
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "缓存区域 '" + name + "' 已清除"
        ));
    }

    @DeleteMapping("/all")
    @Operation(summary = "清除所有缓存")
    public ResponseEntity<Map<String, Object>> clearAll() {
        if (cacheService != null) {
            cacheService.deleteByPattern("*");
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "所有缓存已清除"
        ));
    }

    // ==================== 缓存键管理 ====================

    @GetMapping("/region/{name}/keys")
    @Operation(summary = "获取缓存区域键列表")
    public ResponseEntity<Map<String, Object>> getRegionKeys(@PathVariable String name) {
        boolean exists = REGIONS.stream().anyMatch(r -> r.get("name").equals(name));
        if (!exists) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "缓存区域不存在: " + name,
                    "keys", Collections.emptyList()
            ));
        }

        // 根据区域名称生成模拟的键列表
        String prefix = name.replace(":", ":");
        int count = REGIONS.stream()
                .filter(r -> r.get("name").equals(name))
                .findFirst()
                .map(r -> (int) r.get("keyCount"))
                .orElse(0);

        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= Math.min(count, 50); i++) {
            keys.add(prefix + i);
        }

        return ResponseEntity.ok(Map.of("code", 200, "data", keys, "message", "ok"));
    }

    @DeleteMapping("/region/{region}/key/{key}")
    @Operation(summary = "删除指定缓存键")
    public ResponseEntity<Map<String, Object>> deleteKey(
            @PathVariable String region,
            @PathVariable String key) {
        if (cacheService != null) {
            cacheService.delete(key);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "缓存键 '" + key + "' 已删除"
        ));
    }
}
