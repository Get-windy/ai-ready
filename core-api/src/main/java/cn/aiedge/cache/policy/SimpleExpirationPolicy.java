package cn.aiedge.cache.policy;

import lombok.Getter;

/**
 * 简单固定过期策略
 * 缓存在固定时间后过期
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public class SimpleExpirationPolicy implements CacheExpirationPolicy {

    private final long ttlSeconds;
    private final String name;

    /**
     * 创建简单过期策略
     *
     * @param ttlSeconds 过期时间（秒）
     */
    public SimpleExpirationPolicy(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        this.name = "simple-" + ttlSeconds + "s";
    }

    /**
     * 创建简单过期策略
     *
     * @param ttl  过期时间
     * @param unit 时间单位
     */
    public SimpleExpirationPolicy(long ttl, TimeUnit unit) {
        this.ttlSeconds = unit.toSeconds(ttl);
        this.name = "simple-" + ttlSeconds + "s";
    }

    @Override
    public boolean isSliding() {
        return false;
    }

    @Override
    public boolean isRefreshAhead() {
        return false;
    }

    // ==================== 预定义策略 ====================

    /**
     * 短期缓存策略（5分钟）
     */
    public static final SimpleExpirationPolicy SHORT = new SimpleExpirationPolicy(300);

    /**
     * 中期缓存策略（30分钟）
     */
    public static final SimpleExpirationPolicy MEDIUM = new SimpleExpirationPolicy(1800);

    /**
     * 长期缓存策略（2小时）
     */
    public static final SimpleExpirationPolicy LONG = new SimpleExpirationPolicy(7200);

    /**
     * 永久缓存策略（1天）
     */
    public static final SimpleExpirationPolicy PERMANENT = new SimpleExpirationPolicy(86400);

    /**
     * 永不过期策略
     */
    public static final SimpleExpirationPolicy NEVER_EXPIRE = new SimpleExpirationPolicy(-1);
}
