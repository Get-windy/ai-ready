package cn.aiedge.cache.policy;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * 预刷新过期策略
 * 在缓存过期前自动异步刷新，避免缓存雪崩
 * 适用于高并发、高可用场景
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public class RefreshAheadPolicy implements CacheExpirationPolicy {

    private final long ttlSeconds;
    private final double refreshThreshold;
    private final String name;

    /**
     * 创建预刷新过期策略
     *
     * @param ttlSeconds       过期时间（秒）
     * @param refreshThreshold 预刷新阈值（0-1之间）
     */
    public RefreshAheadPolicy(long ttlSeconds, double refreshThreshold) {
        this.ttlSeconds = ttlSeconds;
        this.refreshThreshold = Math.max(0.5, Math.min(0.95, refreshThreshold));
        this.name = "refresh-ahead-" + ttlSeconds + "s-" + (int)(refreshThreshold * 100) + "%";
    }

    /**
     * 创建预刷新过期策略（默认75%阈值）
     *
     * @param ttlSeconds 过期时间（秒）
     */
    public RefreshAheadPolicy(long ttlSeconds) {
        this(ttlSeconds, 0.75);
    }

    /**
     * 创建预刷新过期策略
     *
     * @param ttl              过期时间
     * @param unit             时间单位
     * @param refreshThreshold 预刷新阈值（0-1之间）
     */
    public RefreshAheadPolicy(long ttl, TimeUnit unit, double refreshThreshold) {
        this(unit.toSeconds(ttl), refreshThreshold);
    }

    @Override
    public boolean isSliding() {
        return false;
    }

    @Override
    public boolean isRefreshAhead() {
        return true;
    }

    /**
     * 计算刷新时间点（秒）
     *
     * @return 刷新时间点
     */
    public long getRefreshAfterSeconds() {
        return (long) (ttlSeconds * refreshThreshold);
    }

    // ==================== 预定义策略 ====================

    /**
     * 配置数据预刷新（1小时TTL，75%刷新）
     */
    public static final RefreshAheadPolicy CONFIG = new RefreshAheadPolicy(3600, 0.75);

    /**
     * 字典数据预刷新（2小时TTL，80%刷新）
     */
    public static final RefreshAheadPolicy DICT = new RefreshAheadPolicy(7200, 0.80);

    /**
     * 菜单数据预刷新（30分钟TTL，70%刷新）
     */
    public static final RefreshAheadPolicy MENU = new RefreshAheadPolicy(1800, 0.70);
}
