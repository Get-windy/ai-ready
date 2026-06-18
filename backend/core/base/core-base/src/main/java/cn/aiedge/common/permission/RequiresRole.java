package cn.aiedge.common.permission;

import java.lang.annotation.*;

/**
 * 角色注解
 * 标注在Controller方法上进行角色校验
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * 角色标识，支持多个（AND关系）
     */
    String[] value() default {};

    /**
     * 角色逻辑：AND | OR
     */
    Logical logical() default Logical.OR;

    /**
     * 角色逻辑枚举
     */
    enum Logical {
        AND, OR
    }
}
