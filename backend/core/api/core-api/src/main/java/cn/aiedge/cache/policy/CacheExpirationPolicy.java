package cn.aiedge.cache.policy;

import java.util.concurrent.TimeUnit;

/**
 * 缓存过期策略接口
 * 定义不同的缓存过期行为
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface CacheExpirationPolicy {

    /**
     * 获取过期时间（秒）
     *
     * @return 过期时间，-1表示永不过期
     */
    long getTtlSeconds();

    /**
     * 是否启用滑动过期
     * 滑动过期：每次访问缓存时重置过期时间
     *
     * @return 是否启用
     */
    boolean isSliding();

    /**
     * 是否启用预刷新
     * 预刷新：在过期前自动异步刷新缓存
     *
     * @return 是否启用
     */
    boolean isRefreshAhead();

    /**
     * 获取预刷新阈值（百分比）
     * 例如：0.75 表示在TTL过75%时开始刷新
     *
     * @return 预刷新阈值（0-1之间）
     */
    default double getRefreshThreshold() {
        return 0.75;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getName();
}
