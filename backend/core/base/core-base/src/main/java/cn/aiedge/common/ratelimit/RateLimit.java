package cn.aiedge.common.ratelimit;

import java.lang.annotation.*;

/**
 * 限流注解
 * 标注在Controller方法上进行限流
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "";

    /**
     * 限流类型
     */
    LimitType type() default LimitType.DEFAULT;

    /**
     * 每秒请求数（QPS）
     */
    int qps() default 100;

    /**
     * 令牌桶容量
     */
    int capacity() default 200;

    /**
     * 等待超时时间（毫秒），0表示不等待直接拒绝
     */
    long timeout() default 0;

    /**
     * 提示消息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 默认（全局限流）
         */
        DEFAULT,

        /**
         * 按IP限流
         */
        IP,

        /**
         * 按用户限流
         */
        USER,

        /**
         * 按API限流
         */
        API
    }
}
