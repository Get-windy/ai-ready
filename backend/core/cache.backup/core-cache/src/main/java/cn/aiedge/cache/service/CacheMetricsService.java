package cn.aiedge.cache.service;

import cn.hutool.core.date.DateUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控服务
 * 收集和统计缓存性能指标
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class CacheMetricsService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();
    private final Map<String, CacheMetrics> metricsMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public CacheMetricsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // 每30秒采集一次指标
        scheduler.scheduleAtFixedRate(this::collectMetrics, 30, 30, TimeUnit.SECONDS);
        log.info("缓存监控服务初始化完成");
    }

    /**
     * 注册本地缓存
     */
    public void registerLocalCache(String name, Cache<String, Object> cache) {
        localCaches.put(name, cache);
        metricsMap.put(name, new CacheMetrics(name));
        log.info("注册缓存监控: {}", name);
    }

    /**
     * 采集指标
     */
    private void collectMetrics() {
        for (Map.Entry<String, Cache<String, Object>> entry : localCaches.entrySet()) {
            String name = entry.getKey();
            Cache<String, Object> cache = entry.getValue();
            CacheStats stats = cache.stats();
            CacheMetrics metrics = metricsMap.get(name);

            if (metrics != null) {
                metrics.setHitCount(stats.hitCount());
                metrics.setMissCount(stats.missCount());
                metrics.setHitRate(stats.hitRate());
                metrics.setLoadCount(stats.loadCount());
                metrics.setEvictionCount(stats.evictionCount());
                metrics.setTotalLoadTime(stats.totalLoadTime());
                metrics.setLastUpdateTime(DateUtil.now());

                // 计算QPS
                long currentTime = System.currentTimeMillis();
                long requestCount = stats.hitCount() + stats.missCount();
                if (metrics.getLastRequestCount() > 0) {
                    long deltaRequests = requestCount - metrics.getLastRequestCount();
                    long deltaTime = currentTime - metrics.getLastCollectTime();
                    double qps = deltaTime > 0 ? (double) deltaRequests / (deltaTime / 1000.0) : 0;
                    metrics.setQps(qps);
                }
                metrics.setLastRequestCount(requestCount);
                metrics.setLastCollectTime(currentTime);
            }
        }
    }

    /**
     * 获取缓存指标
     */
    public CacheMetrics getMetrics(String cacheName) {
        return metricsMap.get(cacheName);
    }

    /**
     * 获取所有缓存指标
     */
    public Map<String, CacheMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsMap);
    }

    /**
     * 记录缓存命中
     */
    public void recordHit(String cacheName) {
        CacheMetrics metrics = metricsMap.get(cacheName);
        if (metrics != null) {
            metrics.getHitCounter().incrementAndGet();
        }
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss(String cacheName) {
        CacheMetrics metrics = metricsMap.get(cacheName);
        if (metrics != null) {
            metrics.getMissCounter().incrementAndGet();
        }
    }

    /**
     * 获取缓存命中率
     */
    public double getHitRate(String cacheName) {
        CacheMetrics metrics = metricsMap.get(cacheName);
        return metrics != null ? metrics.getHitRate() : 0.0;
    }

    /**
     * 获取平均QPS
     */
    public double getAverageQps(String cacheName) {
        CacheMetrics metrics = metricsMap.get(cacheName);
        return metrics != null ? metrics.getQps() : 0.0;
    }

    /**
     * 缓存指标数据
     */
    @Data
    public static class CacheMetrics {
        private String cacheName;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long loadCount;
        private long evictionCount;
        private long totalLoadTime;
        private double qps;
        private String lastUpdateTime;
        
        // 内部计数器
        private AtomicLong hitCounter = new AtomicLong(0);
        private AtomicLong missCounter = new AtomicLong(0);
        private long lastRequestCount = 0;
        private long lastCollectTime = System.currentTimeMillis();

        public CacheMetrics(String cacheName) {
            this.cacheName = cacheName;
        }

        /**
         * 获取命中率百分比
         */
        public String getHitRatePercentage() {
            return String.format("%.2f%%", hitRate * 100);
        }

        /**
         * 获取平均加载时间（毫秒）
         */
        public double getAverageLoadTime() {
            return loadCount > 0 ? (double) totalLoadTime / loadCount / 1_000_000.0 : 0;
        }
    }
}
