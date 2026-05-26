package cn.aiedge.base.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     * CREATE: 创建, UPDATE: 更新, DELETE: 删除, 
     * QUERY: 查询, EXPORT: 导出, IMPORT: 导入,
     * LOGIN: 登录, LOGOUT: 登出, OTHER: 其他
     */
    String type() default "OTHER";

    /**
     * 操作描述
     */
    String desc() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResponse() default false;

    /**
     * SpEL表达式，用于动态获取业务键值
     */
    String businessKey() default "";

    /**
     * SpEL表达式，用于动态获取业务类型
     */
    String businessType() default "";
}