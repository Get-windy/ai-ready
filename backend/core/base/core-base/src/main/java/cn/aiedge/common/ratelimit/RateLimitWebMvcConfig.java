package cn.aiedge.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 限流WebMvc配置
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai-ready.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean(RedisTemplate.class)
public class RateLimitWebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")  // 拦截所有API请求
            .excludePathPatterns(
                "/api/auth/login",       // 登录接口不限流
                "/api/auth/captcha",     // 验证码接口不限流
                "/api/health",           // 健康检查不限流
                "/api/public/**"         // 公开接口不限流
            )
            .order(1);  // 优先级，数字越小优先级越高
    }
}
