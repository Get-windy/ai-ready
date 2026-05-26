package cn.aiedge.permission.annotation;

import java.lang.annotation.*;

/**
 * 角色验证注解
 * 用于标注接口所需的角色编码
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    
    /**
     * 需要的角色编码（支持多个，满足其一即可）
     */
    String[] value() default {};
    
    /**
     * 逻辑关系（AND/OR）
     */
    RequirePermission.Logical logical() default RequirePermission.Logical.OR;
}
