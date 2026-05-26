package cn.aiedge.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * API网关限流配置
 * 配置基于IP、用户或API的限流策略
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class RateLimitConfig {

    /**
     * 基于IP地址的限流键解析器
     * 
     * @return IP限流键解析器
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For") != null ?
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-For").split(",")[0] :
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    /**
     * 基于用户的限流键解析器
     * 
     * @return 用户限流键解析器
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("Authorization") != null ?
                exchange.getRequest().getHeaders().getFirst("Authorization") : "anonymous"
        );
    }

    /**
     * 基于API端点的限流键解析器
     * 
     * @return API限流键解析器
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getPath().toString()
        );
    }
}
