package cn.aiedge.common.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存更新注解
 * 方法执行后更新缓存
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {
    
    /**
     * 缓存key前缀
     */
    String prefix();
    
    /**
     * 缓存key组成部分（SpEL表达式）
     */
    String key() default "";
    
    /**
     * 过期时间
     */
    long ttl() default 300;
}
