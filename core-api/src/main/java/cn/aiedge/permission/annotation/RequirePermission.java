package cn.aiedge.permission.annotation;

import java.lang.annotation.*;

/**
 * 权限验证注解
 * 用于标注接口所需的权限编码
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * 需要的权限编码（支持多个，满足其一即可）
     */
    String[] value() default {};
    
    /**
     * 逻辑关系（AND/OR）
     */
    Logical logical() default Logical.OR;
    
    /**
     * 逻辑枚举
     */
    enum Logical {
        /** 需要全部满足 */
        AND,
        /** 满足其一即可 */
        OR
    }
}
