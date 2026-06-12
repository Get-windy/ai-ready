package cn.aiedge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
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
public class WebMvcConfig implements WebMvcConfigurer {

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
}