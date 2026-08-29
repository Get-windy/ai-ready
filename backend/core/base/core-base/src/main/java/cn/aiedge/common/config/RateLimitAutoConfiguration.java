package cn.aiedge.common.config;

import cn.aiedge.common.ratelimit.AiReadyRateLimitConfig;
import cn.aiedge.common.ratelimit.RateLimitInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiReadyRateLimitConfig.class)
@ConditionalOnProperty(prefix = "ai-ready.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean(StringRedisTemplate.class)
public class RateLimitAutoConfiguration {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor(StringRedisTemplate redisTemplate, AiReadyRateLimitConfig config) {
        log.info("初始化限流拦截器，配置：{}", config);
        return new RateLimitInterceptor(redisTemplate, config);
    }
}