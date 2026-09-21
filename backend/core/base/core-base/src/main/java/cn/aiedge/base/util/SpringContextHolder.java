package cn.aiedge.base.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 静态 Spring 容器访问点。
 *
 * <p>为什么需要它：Jackson 通过 {@code @JsonSerialize(using = ...)} 指定的序列化器
 * （如 {@code DataMaskSerializer}）是由 Jackson 直接 new 出来的，不经过 Spring，
 * 因此拿不到容器里的 Service。这类「框架回调」中的依赖只能从静态入口取。</p>
 *
 * <p>取不到 bean 时一律返回 {@code null} 而不抛异常 —— 调用方必须能容忍
 * 「容器尚未就绪 / 该 bean 不存在」并降级到默认行为，否则会让整个序列化链路失败。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    /** 容器引用。Spring 启动时注入一次，之后只读 */
    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.context = applicationContext;
    }

    /**
     * 按类型取 bean，取不到返回 null（不抛异常）。
     */
    public static <T> T getBeanOrNull(Class<T> type) {
        ApplicationContext ctx = context;
        if (ctx == null) {
            return null;
        }
        try {
            return ctx.getBean(type);
        } catch (BeansException e) {
            return null;
        }
    }
}
