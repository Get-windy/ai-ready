package cn.aiedge.common.permission;

import java.lang.annotation.*;

/**
 * 权限注解
 * 标注在Controller方法上进行权限校验
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限标识，支持多个（AND关系）
     */
    String[] value() default {};

    /**
     * 权限逻辑：AND | OR
     */
    Logical logical() default Logical.AND;

    /**
     * 权限逻辑枚举
     */
    enum Logical {
        AND, OR
    }
}
