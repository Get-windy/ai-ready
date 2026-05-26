package cn.aiedge.cache.annotation;

import java.lang.annotation.*;

/**
 * 多级缓存清除注解
 * 同时清除本地Caffeine缓存和分布式Redis缓存
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCacheEvict {

    /**
     * 缓存名称
     */
    String value() default "";

    /**
     * 缓存Key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 是否清除本地缓存
     */
    boolean local() default true;

    /**
     * 是否清除Redis缓存
     */
    boolean redis() default true;

    /**
     * 是否清除所有缓存
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前清除
     */
    boolean beforeInvocation() default false;

    /**
     * 缓存条件，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 是否使用延迟双删策略
     */
    boolean delayedDoubleDelete() default true;

    /**
     * 延迟删除时间（毫秒）
     */
    long delayMillis() default 500;
}