package cn.aiedge.common.config;

import cn.aiedge.common.ratelimit.RateLimitConfig;
import cn.aiedge.common.ratelimit.RateLimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 限流自动配置类
 * 
 * 自动配置限流相关的组件
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RateLimitConfig.class)
@ConditionalOnProperty(name = "ai-ready.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {
    
    /**
     * 创建限流拦截器
     * 
     * @param redisTemplate Redis模板
     * @param config 限流配置
     * @return 限流拦截器
     */
    @Bean
    public RateLimitInterceptor rateLimitInterceptor(RedisTemplate<String, Object> redisTemplate, RateLimitConfig config) {
        log.info("初始化限流拦截器，配置：{}", config);
        return new RateLimitInterceptor(redisTemplate, config);
    }
    
    /**
     * Web MVC配置，注册限流拦截器
     */
    @Configuration
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