package cn.aiedge.dms.route.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.dms.route.dto.GeocodeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 地理编码结果缓存（Redis 优先 + 进程内兜底）
 *
 * 地图服务的地址解析有 QPS / 日配额限制，同一地址在配送场景会被反复解析
 * （同一客户地址被多张单据引用、多实例部署时被不同实例解析），因此：
 * 1. **Redis 为主**（跨实例共享，24h TTL）——多实例部署时命中率与配额节省才有意义；
 * 2. **进程内 Map 为兜底**（Redis 不可用时自动降级，功能不受影响）。
 *
 * 只缓存**成功结果**：失败结果不缓存，避免配置 Key 后仍返回旧失败。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
public class GeocodeCache {

    /** 缓存有效期：24 小时（行政区划/POI 变化频率极低） */
    private static final Duration TTL = Duration.ofHours(24);

    /** 本地兜底容量上限 */
    private static final int MAX_LOCAL_ENTRIES = 2000;

    private static final String REDIS_KEY_PREFIX = "dms:route:geocode:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;

    private final Map<String, Entry> local = new ConcurrentHashMap<>();
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();

    public GeocodeCache(ObjectProvider<StringRedisTemplate> redisProvider, ObjectMapper objectMapper) {
        this.redisProvider = redisProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取缓存（Redis → 本地；命中即标记 cached=true）
     */
    public GeocodeResponse get(String address, String city) {
        String cacheKey = key(address, city);

        StringRedisTemplate redis = redis();
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(cacheKey);
                if (json != null) {
                    hitCount.incrementAndGet();
                    return objectMapper.readValue(json, GeocodeResponse.class).toBuilder().cached(true).build();
                }
            } catch (Exception e) {
                log.warn("[GeocodeCache] Redis 读取失败，改用本地缓存: {}", e.getMessage());
            }
        }

        Entry entry = local.get(cacheKey);
        if (entry != null && System.currentTimeMillis() - entry.timestamp() <= TTL.toMillis()) {
            hitCount.incrementAndGet();
            return entry.response().toBuilder().cached(true).build();
        }
        if (entry != null) {
            local.remove(cacheKey);
        }
        missCount.incrementAndGet();
        return null;
    }

    /**
     * 写缓存（同时写 Redis 与本地；仅缓存成功结果）
     */
    public void put(String address, String city, GeocodeResponse response) {
        if (response == null || !response.isSuccess()) {
            return;
        }
        String cacheKey = key(address, city);

        StringRedisTemplate redis = redis();
        if (redis != null) {
            try {
                redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), TTL);
            } catch (Exception e) {
                log.warn("[GeocodeCache] Redis 写入失败，仅写本地缓存: {}", e.getMessage());
            }
        }

        if (local.size() >= MAX_LOCAL_ENTRIES) {
            long now = System.currentTimeMillis();
            local.entrySet().removeIf(e -> now - e.getValue().timestamp() > TTL.toMillis());
            if (local.size() >= MAX_LOCAL_ENTRIES) {
                log.warn("[GeocodeCache] 本地缓存达到容量上限，清空重建");
                local.clear();
            }
        }
        local.put(cacheKey, new Entry(response, System.currentTimeMillis()));
    }

    /** 当前缓存条数（Redis 可用时统计 Redis 键数，失败回退本地条数） */
    public int size() {
        StringRedisTemplate redis = redis();
        if (redis != null) {
            try {
                var keys = redis.keys(REDIS_KEY_PREFIX + "*");
                if (keys != null) {
                    return keys.size();
                }
            } catch (Exception e) {
                log.debug("[GeocodeCache] Redis 统计失败，回退本地计数: {}", e.getMessage());
            }
        }
        return local.size();
    }

    /** 缓存命中统计（运维排障用） */
    public Map<String, Long> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("hit", hitCount.get());
        stats.put("miss", missCount.get());
        stats.put("size", (long) size());
        stats.put("backend", redis() != null ? 1L : 0L);
        return stats;
    }

    private StringRedisTemplate redis() {
        try {
            return redisProvider.getIfAvailable();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 缓存键：`租户ID + 源串（城市|地址）` 做 URL-Safe Base64
     *
     * · 带**租户前缀**：多租户下不同租户的同一地址不共享缓存（避免跨租户命中，且便于按租户清理）；
     * · Base64 可逆、无哈希碰撞风险。
     */
    private String key(String address, String city) {
        String raw = (city == null ? "" : city.trim()) + "|" + (address == null ? "" : address.trim().toLowerCase());
        return REDIS_KEY_PREFIX + currentTenantId() + ":" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 当前租户（未登录/无上下文按 0 处理） */
    private Long currentTenantId() {
        try {
            Long tenantId = SecurityUtils.getCurrentTenantId();
            return tenantId == null ? 0L : tenantId;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 清空全部地理编码缓存（换 Key / 切服务商时调用）
     *
     * 换 Key 后旧缓存可能来自另一服务商/另一坐标系，必须整体失效，避免新旧混用。
     */
    public int clearAll() {
        int removed = 0;
        StringRedisTemplate redis = redis();
        if (redis != null) {
            try {
                var keys = redis.keys(REDIS_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                    removed = keys.size();
                }
            } catch (Exception e) {
                log.warn("[GeocodeCache] 清理 Redis 缓存失败: {}", e.getMessage());
            }
        }
        int localSize = local.size();
        local.clear();
        log.info("[GeocodeCache] 缓存已清空: redis={}, local={}", removed, localSize);
        return removed + localSize;
    }

    private record Entry(GeocodeResponse response, long timestamp) {
    }
}
