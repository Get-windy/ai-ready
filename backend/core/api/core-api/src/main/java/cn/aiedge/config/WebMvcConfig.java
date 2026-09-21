package cn.aiedge.config;

import cn.aiedge.module.interceptor.ModuleEntitlementInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 配置异步请求支持，主要用于 SSE (Server-Sent Events) 连接。
 * 设置异步请求超时时间与 SSE 超时时间匹配，避免 SSE 连接在发送事件前超时。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ModuleEntitlementInterceptor moduleEntitlementInterceptor;

    /**
     * 配置异步请求支持
     * <p>
     * SSE 连接需要较长的超时时间，默认设置为 30 分钟，
     * 与 SseNotificationService 的 SSE_TIMEOUT 保持一致。
     * </p>
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求超时时间：30 分钟（与 SSE 超时时间一致）
        configurer.setDefaultTimeout(30 * 60 * 1000L);
    }

    /**
     * 模块 entitlement 门（order=3）。
     *
     * <p><b>为什么注册在这里而不是 {@code SaTokenConfig}</b>：`SaTokenConfig` 在 core-base，
     * 而本拦截器依赖的 {@code ModuleEntitlementService} 在 core-api（core-base 不能反向依赖
     * core-api），故只能由 core-api 侧注册。**但顺序不能改** —— 它必须排在 `SaTokenConfig`
     * 那条链的 order 0/1/2 之后，原因见 {@link ModuleEntitlementInterceptor} 的类注释
     * （要落在 `@SaCheckPermission` 之后，且要在租户头校验之后）。</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleEntitlementInterceptor)
                .addPathPatterns("/**")
                .order(3);
    }
}