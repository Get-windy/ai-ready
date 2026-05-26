package cn.aiedge.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限检查注解
 * 用于标记需要进行数据权限验证的方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermissionCheck {
    
    /**
     * 资源类型
     */
    String resourceType() default "";
    
    /**
     * 权限动作
     */
    String action() default "READ";
    
    /**
     * 是否启用缓存
     */
    boolean useCache() default true;
}