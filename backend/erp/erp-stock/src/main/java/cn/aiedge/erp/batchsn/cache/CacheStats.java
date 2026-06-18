package cn.aiedge.erp.batchsn.cache;

import lombok.Data;

/**
 * 缓存统计信息
 *
 * @author team-member
 * @date 2026-05-01
 */
@Data
public class CacheStats {
    
    /**
     * 缓存命中次数
     */
    private long hitCount;
    
    /**
     * 缓存未命中次数
     */
    private long missCount;
    
    /**
     * 空值缓存命中次数
     */
    private long nullHitCount;
    
    /**
     * 缓存穿透次数
     */
    private long penetrationCount;
    
    /**
     * 缓存击穿次数（锁竞争）
     */
    private long breakdownCount;
    
    /**
     * 本地缓存命中次数
     */
    private long localHitCount;
    
    /**
     * Redis缓存命中次数
     */
    private long redisHitCount;
    
    /**
     * 缓存总操作次数
     */
    public long getTotalCount() {
        return hitCount + missCount;
    }
    
    /**
     * 缓存命中率
     */
    public double getHitRate() {
        long total = getTotalCount();
        return total == 0 ? 0 : (double) hitCount / total;
    }
    
    /**
     * 本地缓存命中率
     */
    public double getLocalHitRate() {
        long localTotal = localHitCount + (missCount - redisHitCount);
        return localTotal == 0 ? 0 : (double) localHitCount / localTotal;
    }
    
    /**
     * Redis缓存命中率
     */
    public double getRedisHitRate() {
        long redisTotal = redisHitCount + (missCount - localHitCount);
        return redisTotal == 0 ? 0 : (double) redisHitCount / redisTotal;
    }
    
    /**
     * 缓存穿透率
     */
    public double getPenetrationRate() {
        long total = getTotalCount();
        return total == 0 ? 0 : (double) penetrationCount / total;
    }
    
    /**
     * 空值缓存命中率
     */
    public double getNullHitRate() {
        long nullTotal = nullHitCount + penetrationCount;
        return nullTotal == 0 ? 0 : (double) nullHitCount / nullTotal;
    }
    
    /**
     * 记录命中
     */
    public void recordHit(boolean isLocal) {
        hitCount++;
        if (isLocal) {
            localHitCount++;
        } else {
            redisHitCount++;
        }
    }
    
    /**
     * 记录未命中
     */
    public void recordMiss() {
        missCount++;
    }
    
    /**
     * 记录空值命中
     */
    public void recordNullHit() {
        nullHitCount++;
        hitCount++;
    }
    
    /**
     * 记录缓存穿透
     */
    public void recordPenetration() {
        penetrationCount++;
        missCount++;
    }
    
    /**
     * 记录缓存击穿
     */
    public void recordBreakdown() {
        breakdownCount++;
    }
    
    /**
     * 重置统计
     */
    public void reset() {
        hitCount = 0;
        missCount = 0;
        nullHitCount = 0;
        penetrationCount = 0;
        breakdownCount = 0;
        localHitCount = 0;
        redisHitCount = 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "CacheStats{total=%d, hitRate=%.2f%%, localHitRate=%.2f%%, redisHitRate=%.2f%%, penetrationRate=%.2f%%, nullHitRate=%.2f%%, breakdown=%d}",
            getTotalCount(),
            getHitRate() * 100,
            getLocalHitRate() * 100,
            getRedisHitRate() * 100,
            getPenetrationRate() * 100,
            getNullHitRate() * 100,
            breakdownCount
        );
    }
}