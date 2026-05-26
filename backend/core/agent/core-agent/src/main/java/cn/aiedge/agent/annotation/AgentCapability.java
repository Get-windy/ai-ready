package cn.aiedge.agent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentCapability {

    String code();

    String name() default "";

    String description() default "";

    String[] tags() default {};

    int timeout() default 30;

    boolean requireAuth() default true;

    String version() default "1.0";
}
