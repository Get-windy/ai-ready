package cn.aiedge.common.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存删除注解
 * 方法执行后删除缓存
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {
    
    /**
     * 缓存key前缀
     */
    String prefix();
    
    /**
     * 缓存key组成部分（SpEL表达式）
     */
    String key() default "";
    
    /**
     * 是否删除所有匹配前缀的缓存
     */
    boolean allEntries() default false;
    
    /**
     * 是否在方法执行前删除
     */
    boolean beforeInvocation() default false;
}
