package cn.aiedge.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务权限检查注解
 * 用于标记需要进行业务权限验证的方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessPermissionCheck {
    
    /**
     * 权限资源标识
     */
    String resource() default "";
    
    /**
     * 权限动作
     */
    String action() default "ACCESS";
    
    /**
     * 是否启用缓存
     */
    boolean useCache() default true;
    
    /**
     * 是否记录权限日志
     */
    boolean logPermission() default true;
}