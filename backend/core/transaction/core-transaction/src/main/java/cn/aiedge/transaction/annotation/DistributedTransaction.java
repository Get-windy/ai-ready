package cn.aiedge.transaction.annotation;

import cn.aiedge.transaction.enums.TransactionMode;

import java.lang.annotation.*;

/**
 * 分布式事务注解
 * 用于标识需要分布式事务管理的方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedTransaction {

    /**
     * 事务名称
     */
    String name() default "";

    /**
     * 事务模式
     */
    TransactionMode mode() default TransactionMode.TCC;

    /**
     * 超时时间（秒）
     */
    int timeout() default 30;

    /**
     * 最大重试次数
     */
    int maxRetries() default 3;

    /**
     * 参与者服务名称
     */
    String[] participants() default {};
}