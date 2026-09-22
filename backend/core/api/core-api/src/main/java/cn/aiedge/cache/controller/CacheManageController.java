package cn.aiedge.cache.controller;

import cn.aiedge.cache.service.CacheService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 缓存管理控制器（系统 → 系统监控 → 缓存管理，菜单 62206）
 *
 * <p>⚠️ 2026-09-18 重写：此前本类**整页假数据** —— 9 个「缓存区域」是类常量
 * （{@code REGIONS}）、概述数字由常量求和得出、`expiredKeys` 写死 32、
 * 键列表是 `name + ":" + i` 按公式现编（连 type/size/ttl 都是取模算出来的）。
 * 而删除类端点却是**真删 Redis**（含会话）—— 「读假 / 写真」是最危险的组合：
 * 用户看着假数字点「清除」，真数据被删掉。
 *
 * <p>现在全部改为真实读取 Redis：概览取自 {@code INFO}（used_memory / keyspace_hits /
 * expired_keys），区域按**键前缀**真实聚合，键列表用 {@code SCAN}（**不用 KEYS**，避免阻塞）。
 * 区域级的「命中率」Redis 只在全局统计，故不再提供该列（前端显示为「—」而不是编一个数）。
 * Redis 不可用时返回明确的错误信息，不再返回假数据。
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "缓存管理", description = "缓存监控与管理功能")
@SaCheckLogin
public class CacheManageController {

    private final StringRedisTemplate redisTemplate;
    private final CacheService cacheService;

    /** 单次列举/扫描的键上限（防止大库把响应撑爆） */
    private static final int SCAN_LIMIT = 200;
    private static final int SCAN_BATCH = 200;

    // ==================== 缓存概览 ====================

    @SaCheckPermission("system:cache:view")
    @GetMapping("/status")
    @Operation(summary = "获取缓存概览统计（真实读取 Redis INFO）")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        try {
            Properties info = redisTemplate.execute((RedisCallback<Properties>) c -> c.serverCommands().info());
            Long dbSize = redisTemplate.execute((RedisCallback<Long>) c -> c.serverCommands().dbSize());
            Map<String, Object> status = buildStatus(info, dbSize);
            return ResponseEntity.ok(Map.of("code", 200, "data", status, "message", "ok"));
        } catch (Exception e) {
            log.error("[缓存管理] 读取 Redis 信息失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "Redis 不可用：" + e.getMessage(),
                    "data", Collections.emptyMap()));
        }
    }

    private Map<String, Object> buildStatus(Properties info, Long dbSize) {
        String usedMemory = info == null ? null : info.getProperty("used_memory_human");
        long hits = parseLong(info, "keyspace_hits");
        long misses = parseLong(info, "keyspace_misses");
        long expired = parseLong(info, "expired_keys");
        long commands = parseLong(info, "total_commands_processed");
        long connectedClients = parseLong(info, "connected_clients");
        long evicted = parseLong(info, "evicted_keys");

        long total = hits + misses;
        double hitRate = total > 0 ? Math.round(hits * 1000.0 / total) / 10.0 : 0.0;

        // 按「键前缀」（第一个冒号之前的部分，无冒号则归入 (无前缀)）真实聚合。
        // 用 SCAN 采样而非 KEYS：KEYS 在大库上会阻塞整个 Redis。
        List<String> sampledKeys = new ArrayList<>(scanKeys("*", 2000));
        Map<String, List<String>> grouped = sampledKeys.stream()
                .collect(Collectors.groupingBy(CacheManageController::prefixOf));

        List<Map<String, Object>> regions = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            List<String> keys = entry.getValue();
            long ttlSum = 0;
            int ttlCount = 0;
            for (String k : keys) {
                Long ttl = ttlOf(k);
                if (ttl != null && ttl > 0) {
                    ttlSum += ttl;
                    ttlCount++;
                }
            }
            Map<String, Object> region = new LinkedHashMap<>();
            region.put("name", entry.getKey());
            region.put("keyCount", (long) keys.size());
            // 区域级内存/命中率 Redis 不提供 —— 如实返回 null，前端显示「—」，不要编数
            region.put("memory", null);
            region.put("hitRate", null);
            region.put("avgTtl", ttlCount > 0 ? ttlSum / ttlCount : null);
            regions.add(region);
        }
        regions.sort((a, b) -> Long.compare((Long) b.get("keyCount"), (Long) a.get("keyCount")));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalSize", usedMemory == null ? null : usedMemory);
        status.put("totalKeys", dbSize == null ? sampledKeys.size() : dbSize);
        status.put("hitRate", hitRate);
        status.put("expiredKeys", expired);
        status.put("connectedClients", connectedClients);
        status.put("totalCommands", commands);
        status.put("evictedKeys", evicted);
        status.put("sampledKeys", sampledKeys.size());
        status.put("regions", regions);
        return status;
    }

    private static String prefixOf(String key) {
        int idx = key.indexOf(':');
        return idx > 0 ? key.substring(0, idx) : "(无前缀)";
    }

    private static long parseLong(Properties info, String key) {
        if (info == null) return 0L;
        String v = info.getProperty(key);
        if (v == null) return 0L;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ==================== 缓存区域管理 ====================

    @SaCheckPermission("system:cache:delete")
    @DeleteMapping("/region/{name}")
    @Operation(summary = "清除指定缓存区域（按前缀 SCAN 删除）")
    public ResponseEntity<Map<String, Object>> clearRegion(@PathVariable String name) {
        String pattern = "(无前缀)".equals(name) ? "*" : name + ":*";
        try {
            long deleted = deleteByPattern(pattern);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deleted", deleted,
                    "message", "缓存区域 '" + name + "' 已清除，共删除 " + deleted + " 个键"));
        } catch (Exception e) {
            log.error("[缓存管理] 清除区域失败: {}", name, e);
            return ResponseEntity.ok(Map.of("success", false, "message", "清除失败：" + e.getMessage()));
        }
    }

    @SaCheckPermission("system:cache:delete")
    @DeleteMapping("/all")
    @Operation(summary = "清除所有缓存（危险：会一并删除登录会话等全部键）")
    public ResponseEntity<Map<String, Object>> clearAll() {
        try {
            Long before = redisTemplate.execute((RedisCallback<Long>) c -> c.serverCommands().dbSize());
            long deleted = deleteByPattern("*");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deleted", deleted,
                    "keysBefore", before == null ? -1 : before,
                    "message", "已清除 " + deleted + " 个键（含登录会话，相关用户需重新登录）"));
        } catch (Exception e) {
            log.error("[缓存管理] 清除全部失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "清除失败：" + e.getMessage()));
        }
    }

    // ==================== 缓存键管理 ====================

    @SaCheckPermission("system:cache:view")
    @GetMapping("/region/{name}/keys")
    @Operation(summary = "获取缓存区域键列表（SCAN 真实列举，最多 " + SCAN_LIMIT + " 个）")
    public ResponseEntity<Map<String, Object>> getRegionKeys(@PathVariable String name) {
        String pattern = "(无前缀)".equals(name) ? "*" : name + ":*";
        try {
            Set<String> keys = scanKeys(pattern, SCAN_LIMIT);
            List<Map<String, Object>> list = new ArrayList<>();
            for (String key : keys) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", key);
                item.put("type", typeOf(key));
                item.put("ttl", ttlOf(key));
                list.add(item);
            }
            return ResponseEntity.ok(Map.of("code", 200, "data", list, "message", "ok"));
        } catch (Exception e) {
            log.error("[缓存管理] 列举键失败: {}", name, e);
            return ResponseEntity.ok(Map.of(
                    "code", 500, "success", false,
                    "message", "Redis 不可用：" + e.getMessage(),
                    "data", Collections.emptyList()));
        }
    }

    @SaCheckPermission("system:cache:delete")
    @DeleteMapping("/region/{region}/key/{key}")
    @Operation(summary = "删除指定缓存键")
    public ResponseEntity<Map<String, Object>> deleteKey(
            @PathVariable String region,
            @PathVariable String key) {
        try {
            Boolean ok = redisTemplate.delete(key);
            return ResponseEntity.ok(Map.of(
                    "success", Boolean.TRUE.equals(ok),
                    "message", Boolean.TRUE.equals(ok)
                            ? "缓存键 '" + key + "' 已删除"
                            : "缓存键 '" + key + "' 不存在"));
        } catch (Exception e) {
            log.error("[缓存管理] 删除键失败: {}", key, e);
            return ResponseEntity.ok(Map.of("success", false, "message", "删除失败：" + e.getMessage()));
        }
    }

    // ==================== Redis 底层工具 ====================

    /** 用 SCAN 列举匹配的键（**不用 KEYS**：KEYS 会阻塞整个 Redis 实例） */
    private Set<String> scanKeys(String pattern, int limit) {
        Set<String> keys = new LinkedHashSet<>();
        redisTemplate.execute((RedisCallback<Void>) conn -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH).build();
            try (Cursor<byte[]> cursor = conn.keyCommands().scan(options)) {
                while (cursor.hasNext() && keys.size() < limit) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return null;
        });
        return keys;
    }

    /** 用 SCAN 分批删除匹配的键，返回实际删除数量 */
    private long deleteByPattern(String pattern) {
        long deleted = 0;
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH).build();
        while (true) {
            List<byte[]> batch = new ArrayList<>();
            redisTemplate.execute((RedisCallback<Void>) conn -> {
                try (Cursor<byte[]> cursor = conn.keyCommands().scan(options)) {
                    while (cursor.hasNext() && batch.size() < SCAN_BATCH) {
                        batch.add(cursor.next());
                    }
                }
                return null;
            });
            if (batch.isEmpty()) break;
            Long n = redisTemplate.execute((RedisCallback<Long>) conn -> conn.keyCommands().del(batch.toArray(new byte[0][])));
            deleted += n == null ? 0 : n;
            if (batch.size() < SCAN_BATCH) break;
        }
        return deleted;
    }

    private String typeOf(String key) {
        try {
            DataType type = redisTemplate.type(key);
            return type == null ? "unknown" : type.code();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Long ttlOf(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            return null;
        }
    }
}
