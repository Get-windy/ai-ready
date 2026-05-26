package com.aiready.permission.annotation;

import java.lang.annotation.*;

/**
 * 权限注解 - 用于方法级别的权限控制
 * 支持基于角色的访问控制(RBAC)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * 需要的权限编码列表
     */
    String[] value() default {};
    
    /**
     * 需要的角色列表
     */
    String[] roles() default {};
    
    /**
     * 逻辑关系：AND表示需要所有权限，OR表示需要任一权限
     */
    Logical logical() default Logical.AND;
    
    /**
     * 数据权限范围
     */
    DataScope dataScope() default DataScope.ALL;
    
    /**
     * 逻辑关系枚举
     */
    enum Logical {
        AND, OR
    }
    
    /**
     * 数据权限范围枚举
     */
    enum DataScope {
        ALL,           // 全部数据
        DEPT_ONLY,     // 本部门数据
        DEPT_AND_CHILD,// 本部门及子部门数据
        SELF_ONLY      // 仅本人数据
    }
}
