package com.aiready.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解 - 用于数据级别的访问控制
 * 支持基于组织和角色的数据过滤
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    
    /**
     * 数据表别名
     */
    String tableAlias() default "";
    
    /**
     * 部门ID字段名
     */
    String deptColumn() default "dept_id";
    
    /**
     * 用户ID字段名
     */
    String userColumn() default "create_by";
    
    /**
     * 是否启用数据权限
     */
    boolean enabled() default true;
    
    /**
     * 排除的方法名（不应用数据权限）
     */
    String[] excludeMethods() default {};
}
