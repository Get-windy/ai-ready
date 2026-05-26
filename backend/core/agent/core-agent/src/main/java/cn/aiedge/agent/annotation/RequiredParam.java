package cn.aiedge.agent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记能力参数为必填项
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredParam {

    /**
     * 参数名
     */
    String value();

    /**
     * 参数描述
     */
    String description() default "";
}
