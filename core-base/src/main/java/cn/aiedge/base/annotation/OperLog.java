package cn.aiedge.base.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标记在Controller方法上，自动记录操作日志
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作名称
     */
    String action() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResult() default false;

    /**
     * 是否忽略敏感字段（密码等）
     */
    boolean ignoreSensitive() default true;
}
