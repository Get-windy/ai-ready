package cn.aiedge.cache.stats;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计信息
 * 跟踪缓存命中率、访问次数等指标
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class CacheStatistics {

    /**
     * 缓存名称 -> 统计数据
     */
    private final Map<String, CacheStats> statsMap = new ConcurrentHashMap<>();

    /**
     * 记录缓存命中
     *
     * @param cacheName 缓存名称
     */
    public void recordHit(String cacheName) {
        getOrCreateStats(cacheName).hits.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     *
     * @param cacheName 缓存名称
     */
    public void recordMiss(String cacheName) {
        getOrCreateStats(cacheName).misses.incrementAndGet();
    }

    /**
     * 记录缓存加载
     *
     * @param cacheName 缓存名称
     */
    public void recordLoad(String cacheName) {
        getOrCreateStats(cacheName).loads.incrementAndGet();
    }

    /**
     * 记录缓存加载失败
     *
     * @param cacheName 缓存名称
     */
    public void recordLoadFailure(String cacheName) {
        getOrCreateStats(cacheName).loadFailures.incrementAndGet();
    }

    /**
     * 记录缓存驱逐
     *
     * @param cacheName 缓存名称
     */
    public void recordEviction(String cacheName) {
        getOrCreateStats(cacheName).evictions.incrementAndGet();
    }

    /**
     * 获取或创建统计对象
     */
    private CacheStats getOrCreateStats(String cacheName) {
        return statsMap.computeIfAbsent(cacheName, k -> new CacheStats());
    }

    /**
     * 获取缓存命中率
     *
     * @param cacheName 缓存名称
     * @return 命中率（0-1之间）
     */
    public double getHitRate(String cacheName) {
        CacheStats stats = statsMap.get(cacheName);
        if (stats == null) {
            return 0.0;
        }
        return stats.getHitRate();
    }

    /**
     * 获取缓存统计摘要
     *
     * @param cacheName 缓存名称
     * @return 统计摘要
     */
    public CacheStatsSummary getSummary(String cacheName) {
        CacheStats stats = statsMap.get(cacheName);
        if (stats == null) {
            return new CacheStatsSummary(cacheName, 0, 0, 0, 0, 0, 0.0);
        }
        return new CacheStatsSummary(
                cacheName,
                stats.hits.get(),
                stats.misses.get(),
                stats.loads.get(),
                stats.loadFailures.get(),
                stats.evictions.get(),
                stats.getHitRate()
        );
    }

    /**
     * 获取所有缓存统计
     *
     * @return 统计映射
     */
    public Map<String, CacheStatsSummary> getAllSummaries() {
        Map<String, CacheStatsSummary> summaries = new ConcurrentHashMap<>();
        statsMap.forEach((name, stats) -> summaries.put(name, getSummary(name)));
        return summaries;
    }

    /**
     * 重置指定缓存的统计
     *
     * @param cacheName 缓存名称
     */
    public void reset(String cacheName) {
        statsMap.remove(cacheName);
    }

    /**
     * 重置所有统计
     */
    public void resetAll() {
        statsMap.clear();
    }

    /**
     * 缓存统计数据
     */
    @Getter
    private static class CacheStats {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong loads = new AtomicLong(0);
        private final AtomicLong loadFailures = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);

        public double getHitRate() {
            long totalRequests = hits.get() + misses.get();
            if (totalRequests == 0) {
                return 0.0;
            }
            return (double) hits.get() / totalRequests;
        }
    }

    /**
     * 缓存统计摘要
     */
    @Getter
    public static class CacheStatsSummary {
        private final String cacheName;
        private final long hits;
        private final long misses;
        private final long loads;
        private final long loadFailures;
        private final long evictions;
        private final double hitRate;

        public CacheStatsSummary(String cacheName, long hits, long misses, 
                                 long loads, long loadFailures, long evictions, double hitRate) {
            this.cacheName = cacheName;
            this.hits = hits;
            this.misses = misses;
            this.loads = loads;
            this.loadFailures = loadFailures;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }

        /**
         * 获取总请求数
         */
        public long getTotalRequests() {
            return hits + misses;
        }

        /**
         * 获取平均加载成功率
         */
        public double getLoadSuccessRate() {
            long totalLoads = loads + loadFailures;
            if (totalLoads == 0) {
                return 1.0;
            }
            return (double) loads / totalLoads;
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheStats[%s]: hits=%d, misses=%d, hitRate=%.2f%%, loads=%d, evictions=%d",
                    cacheName, hits, misses, hitRate * 100, loads, evictions
            );
        }
    }
}
