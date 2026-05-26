package com.aiready.permission.annotation;

import java.lang.annotation.*;

/**
 * 角色注解 - 用于方法级别的角色控制
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    
    /**
     * 需要的角色编码列表
     */
    String[] value();
    
    /**
     * 逻辑关系：AND表示需要所有角色，OR表示需要任一角色
     */
    Logical logical() default Logical.OR;
    
    /**
     * 逻辑关系枚举
     */
    enum Logical {
        AND, OR
    }
}
