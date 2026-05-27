package cn.aiedge.common.config;

import cn.aiedge.common.ratelimit.AiReadyRateLimitConfig;
import cn.aiedge.common.ratelimit.RateLimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiReadyRateLimitConfig.class)
@ConditionalOnProperty(name = "ai-ready.ratelimit.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitAutoConfiguration {
    
    @Bean
    public RateLimitInterceptor rateLimitInterceptor(RedisTemplate<String, Object> redisTemplate, AiReadyRateLimitConfig config) {
        log.info("初始化限流拦截器，配置：{}", config);
        return new RateLimitInterceptor(redisTemplate, config);
    }
    
    /**
     * Web MVC配置，注册限流拦截器
     */
    @Configuration
    @ConditionalOnProperty(name = "ai-ready.ratelimit.enabled", havingValue = "true", matchIfMissing = false)
    public static class RateLimitWebMvcConfig implements WebMvcConfigurer {
        
        private final RateLimitInterceptor rateLimitInterceptor;
        
        public RateLimitWebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
            this.rateLimitInterceptor = rateLimitInterceptor;
        }
        
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            // 注册限流拦截器，可以指定拦截的路径
            registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/**") // 拦截所有请求
                    .excludePathPatterns("/health", "/actuator/**"); // 排除健康检查等路径
        }
    }
}