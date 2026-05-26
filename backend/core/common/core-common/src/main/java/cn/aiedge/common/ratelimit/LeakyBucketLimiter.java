package cn.aiedge.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 漏桶限流器实现
 * 
 * 漏桶算法是一种常用的流量整形和速率限制算法。
 * 特点：
 * 1. 平滑输出：无论输入流量如何波动，输出流量都是恒定的
 * 2. 流量整形：将突发流量转换为均匀流量
 * 3. 恒定处理速度：以固定的速率处理请求
 * 
 * 算法原理：
 * 1. 请求进入漏桶（缓冲区）
 * 2. 按固定速率从桶中"漏水"（处理请求）
 * 3. 桶满时新请求被丢弃或等待
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class LeakyBucketLimiter {

    private final ConcurrentMap<String, LeakyBucket> buckets = new ConcurrentHashMap<>();
    
    /**
     * 尝试获取许可
     * 
     * @param key 限流键
     * @param capacity 桶容量
     * @param leakRate 泄漏速率（每秒处理的请求数）
     * @return 是否获得许可
     */
    public boolean tryAcquire(String key, int capacity, int leakRate) {
        return tryAcquire(key, 1, capacity, leakRate);
    }

    /**
     * 尝试获取指定数量的许可
     * 
     * @param key 限流键
     * @param permits 请求数量
     * @param capacity 桶容量
     * @param leakRate 泄漏速率（每秒处理的请求数）
     * @return 是否获得许可
     */
    public boolean tryAcquire(String key, int permits, int capacity, int leakRate) {
        try {
            LeakyBucket bucket = buckets.computeIfAbsent(key, k -> new LeakyBucket(capacity, leakRate));
            boolean allowed = bucket.request(permits);
            
            if (!allowed) {
                log.debug("漏桶限流拒绝: key={}, permits={}", key, permits);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("漏桶限流检查异常: key={}", key, e);
            // 异常时允许通过，避免影响正常业务
            return true;
        }
    }

    /**
     * 获取当前水位
     * 
     * @param key 限流键
     * @return 当前水位
     */
    public long getCurrentWaterLevel(String key) {
        LeakyBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getCurrentWaterLevel() : 0;
    }

    /**
     * 重置限流器
     * 
     * @param key 限流键
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * 漏桶内部实现类
     */
    private static class LeakyBucket {
        private final int capacity;           // 桶容量
        private final int leakRate;          // 漏水速率（每秒处理的请求数）
        private volatile long waterLevel;    // 当前水量
        private volatile long lastUpdateTime; // 最后更新时间

        public LeakyBucket(int capacity, int leakRate) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.waterLevel = 0;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 处理请求
         * 
         * @param requestCount 请求的数量
         * @return 是否允许通过
         */
        public synchronized boolean request(int requestCount) {
            long currentTime = System.currentTimeMillis();
            
            // 计算自上次更新以来经过的时间（秒）
            long elapsedTimeMs = currentTime - lastUpdateTime;
            double elapsedTimeSec = elapsedTimeMs / 1000.0;
            
            // 计算这段时间内漏掉的水量
            double leakedWater = elapsedTimeSec * leakRate;
            
            // 更新水量：减去漏掉的水，再加上新的请求
            waterLevel = Math.max(0, waterLevel - (long) leakedWater);
            long newWaterLevel = waterLevel + requestCount;
            
            // 如果超过桶容量，则拒绝请求
            if (newWaterLevel > capacity) {
                return false;
            }
            
            // 更新水量和时间
            waterLevel = newWaterLevel;
            lastUpdateTime = currentTime;
            
            return true;
        }

        /**
         * 获取当前水位（考虑泄漏后的实际水位）
         * 
         * @return 当前水位
         */
        public long getCurrentWaterLevel() {
            long currentTime = System.currentTimeMillis();
            long elapsedTimeMs = currentTime - lastUpdateTime;
            double elapsedTimeSec = elapsedTimeMs / 1000.0;
            double leakedWater = elapsedTimeSec * leakRate;
            return Math.max(0, (long)(waterLevel - leakedWater));
        }
    }
}