package cn.aiedge.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关限流过滤器
 * 
 * 功能：
 * 1. 基于多种维度的限流（IP/用户/API）
 * 2. 支持多种限流算法（令牌桶/漏桶/滑动窗口）
 * 3. 熔断降级
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai-ready.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AiReadyRateLimitConfig rateLimitConfig;
    
    // 限流器缓存
    private final Map<String, TokenBucketLimiter> tokenBucketLimiters = new ConcurrentHashMap<>();
    private final Map<String, LeakyBucketLimiter> leakyBucketLimiters = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowLimiter> slidingWindowLimiters = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitConfig.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        String clientIp = getClientIp(exchange);
        String userId = getUserId(exchange);
        
        // 构建限流key
        String ipKey = "ip:" + clientIp + ":" + path;
        String userKey = "user:" + userId + ":" + path;
        String apiKey = "api:" + path;
        
        // 检查熔断器状态
        CircuitBreaker circuitBreaker = getCircuitBreaker(path);
        if (!circuitBreaker.allowRequest()) {
            log.warn("熔断器开启，拒绝请求: path={}", path);
            return onResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE, 
                "服务暂时不可用，请稍后重试");
        }
        
        try {
            // IP级别限流
            if (!checkIpRateLimit(ipKey, clientIp)) {
                log.warn("IP限流触发: ip={}, path={}", clientIp, path);
                return onResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, 
                    "请求过于频繁，请稍后再试");
            }
            
            // 用户级别限流
            if (!checkUserRateLimit(userKey, userId)) {
                log.warn("用户限流触发: userId={}, path={}", userId, path);
                return onResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, 
                    "您的请求过于频繁，请稍后再试");
            }
            
            // API级别限流
            if (!checkApiRateLimit(apiKey, path)) {
                log.warn("API限流触发: path={}", path);
                return onResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, 
                    "服务繁忙，请稍后再试");
            }
            
            // 执行请求
            return chain.filter(exchange)
                .doOnSuccess(v -> circuitBreaker.recordSuccess())
                .doOnError(e -> circuitBreaker.recordFailure());
                
        } catch (Exception e) {
            log.error("限流检查异常", e);
            // 异常时放行，避免影响正常请求
            return chain.filter(exchange);
        }
    }

    /**
     * IP级别限流检查
     */
    private boolean checkIpRateLimit(String key, String clientIp) {
        int ipQps = rateLimitConfig.getIpQps();
        if (ipQps <= 0) {
            return true;
        }
        
        TokenBucketLimiter limiter = tokenBucketLimiters.computeIfAbsent(
            "ip", k -> new TokenBucketLimiter(redisTemplate));
        
        return limiter.tryAcquire(key, ipQps, ipQps * 2);
    }

    /**
     * 用户级别限流检查
     */
    private boolean checkUserRateLimit(String key, String userId) {
        int userQps = rateLimitConfig.getUserQps();
        if (userQps <= 0) {
            return true;
        }
        
        TokenBucketLimiter limiter = tokenBucketLimiters.computeIfAbsent(
            "user", k -> new TokenBucketLimiter(redisTemplate));
        
        return limiter.tryAcquire(key, userQps, userQps * 2);
    }

    /**
     * API级别限流检查
     */
    private boolean checkApiRateLimit(String key, String path) {
        AiReadyRateLimitConfig.ApiLimit apiLimit = rateLimitConfig.getApiLimits().get(path);
        if (apiLimit == null) {
            // 使用默认配置
            int defaultQps = rateLimitConfig.getDefaultQps();
            int defaultCapacity = rateLimitConfig.getDefaultCapacity();
            
            TokenBucketLimiter limiter = tokenBucketLimiters.computeIfAbsent(
                "api", k -> new TokenBucketLimiter(redisTemplate));
            
            return limiter.tryAcquire(key, defaultQps, defaultCapacity);
        }
        
        TokenBucketLimiter limiter = tokenBucketLimiters.computeIfAbsent(
            "api", k -> new TokenBucketLimiter(redisTemplate));
        
        return limiter.tryAcquire(key, apiLimit.getQps(), apiLimit.getCapacity());
    }

    /**
     * 获取熔断器
     */
    private CircuitBreaker getCircuitBreaker(String path) {
        return circuitBreakers.computeIfAbsent(path, k -> {
            return new CircuitBreaker(10, 60000); // 10次失败后熔断，60秒后尝试恢复
        });
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取用户ID
     */
    private String getUserId(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            userId = exchange.getRequest().getHeaders().getFirst("Authorization");
        }
        return userId != null ? userId : "anonymous";
    }

    /**
     * 构建响应
     */
    private Mono<Void> onResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        String body = String.format("{\"code\":%d,\"message\":\"%s\"}", status.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // 优先级高，在认证之前执行
    }
}
