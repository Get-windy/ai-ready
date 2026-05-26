package cn.aiedge.cache.policy;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * 滑动过期策略
 * 每次访问缓存时自动延长过期时间
 * 适用于会话、热点数据等场景
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public class SlidingExpirationPolicy implements CacheExpirationPolicy {

    private final long ttlSeconds;
    private final String name;

    /**
     * 创建滑动过期策略
     *
     * @param ttlSeconds 过期时间（秒）
     */
    public SlidingExpirationPolicy(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        this.name = "sliding-" + ttlSeconds + "s";
    }

    /**
     * 创建滑动过期策略
     *
     * @param ttl  过期时间
     * @param unit 时间单位
     */
    public SlidingExpirationPolicy(long ttl, TimeUnit unit) {
        this.ttlSeconds = unit.toSeconds(ttl);
        this.name = "sliding-" + ttlSeconds + "s";
    }

    @Override
    public boolean isSliding() {
        return true;
    }

    @Override
    public boolean isRefreshAhead() {
        return false;
    }

    // ==================== 预定义策略 ====================

    /**
     * 用户会话滑动过期（30分钟无活动过期）
     */
    public static final SlidingExpirationPolicy USER_SESSION = new SlidingExpirationPolicy(1800);

    /**
     * 用户Token滑动过期（1小时无活动过期）
     */
    public static final SlidingExpirationPolicy USER_TOKEN = new SlidingExpirationPolicy(3600);

    /**
     * 热点数据滑动过期（10分钟无访问过期）
     */
    public static final SlidingExpirationPolicy HOT_DATA = new SlidingExpirationPolicy(600);
}
