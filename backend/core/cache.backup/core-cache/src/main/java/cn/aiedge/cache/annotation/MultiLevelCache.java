package cn.aiedge.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存注解
 * 同时支持本地Caffeine缓存和分布式Redis缓存
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCache {

    /**
     * 缓存名称
     */
    String value() default "";

    /**
     * 缓存Key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 本地缓存过期时间
     */
    long localTtl() default 300;

    /**
     * 本地缓存时间单位
     */
    TimeUnit localTimeUnit() default TimeUnit.SECONDS;

    /**
     * 本地缓存最大容量
     */
    long localMaxSize() default 10000;

    /**
     * Redis缓存过期时间
     */
    long redisTtl() default 3600;

    /**
     * Redis缓存时间单位
     */
    TimeUnit redisTimeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用本地缓存
     */
    boolean enableLocal() default true;

    /**
     * 是否启用Redis缓存
     */
    boolean enableRedis() default true;

    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 缓存失效条件，支持SpEL表达式
     */
    String unless() default "";

    /**
     * 是否同步加载（防止缓存击穿）
     */
    boolean sync() default false;

    /**
     * 同步加载超时时间（毫秒）
     */
    long syncTimeout() default 5000;

    /**
     * 是否启用预刷新机制
     * 在缓存即将过期前自动异步刷新
     */
    boolean enableRefreshAhead() default false;
}