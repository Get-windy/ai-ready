package cn.aiedge.common.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存注解
 * 用于方法级别缓存控制
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    
    /**
     * 缓存key前缀
     */
    String prefix();
    
    /**
     * 缓存key组成部分（SpEL表达式）
     * 例如：#id, #user.id
     */
    String key() default "";
    
    /**
     * 过期时间
     */
    long ttl() default 300;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 是否缓存空值（防止穿透）
     */
    boolean cacheNull() default true;
    
    /**
     * 条件表达式（SpEL）
     * 为true时才缓存
     */
    String condition() default "";
}
