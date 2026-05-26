package cn.aiedge.common.ratelimit;

import cn.aiedge.common.exception.BizException;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 限流拦截器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai-ready.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitConfig rateLimitConfig;
    private TokenBucketLimiter tokenBucketLimiter;

    // 手动添加构造方法，以防 @RequiredArgsConstructor 不生效
    public RateLimitInterceptor(RedisTemplate<String, Object> redisTemplate, RateLimitConfig rateLimitConfig) {
        this.redisTemplate = redisTemplate;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                              Object handler) throws Exception {
        
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        
        // 检查是否有@RateLimit注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = method.getDeclaringClass().getAnnotation(RateLimit.class);
        }
        
        if (rateLimit == null) {
            return true;
        }
        
        // 初始化限流器
        if (tokenBucketLimiter == null) {
            tokenBucketLimiter = new TokenBucketLimiter(redisTemplate);
        }
        
        // 构建限流key
        String key = buildKey(request, rateLimit);
        
        // 获取限流参数
        int qps = rateLimit.qps() > 0 ? rateLimit.qps() : rateLimitConfig.getDefaultQps();
        int capacity = rateLimit.capacity() > 0 ? rateLimit.capacity() : rateLimitConfig.getDefaultCapacity();
        long timeout = rateLimit.timeout() >= 0 ? rateLimit.timeout() : rateLimitConfig.getDefaultTimeout();
        
        // 尝试获取令牌
        boolean allowed;
        if (timeout > 0) {
            allowed = tokenBucketLimiter.tryAcquireWithWait(key, 1, qps, capacity, timeout);
        } else {
            allowed = tokenBucketLimiter.tryAcquire(key, 1, qps, capacity);
        }
        
        if (!allowed) {
            log.warn("请求被限流: key={}, uri={}, ip={}", 
                key, request.getRequestURI(), getClientIp(request));
            
            // 返回HTTP 429
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":429,\"message\":\"" + rateLimit.message() + "\"}"
            );
            return false;
        }
        
        return true;
    }

    /**
     * 构建限流key
     */
    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 自定义key前缀
        if (StrUtil.isNotBlank(rateLimit.key())) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        
        // 根据限流类型添加标识
        switch (rateLimit.type()) {
            case IP:
                keyBuilder.append("ip:").append(getClientIp(request));
                break;
            case USER:
                // 从请求属性或Header获取用户ID
                String userId = request.getHeader("X-User-Id");
                if (StrUtil.isBlank(userId)) {
                    userId = (String) request.getAttribute("userId");
                }
                keyBuilder.append("user:").append(StrUtil.isNotBlank(userId) ? userId : "anonymous");
                break;
            case API:
                keyBuilder.append("api:").append(request.getRequestURI());
                break;
            default:
                // 默认使用URI + IP组合
                keyBuilder.append(request.getRequestURI())
                    .append(":").append(getClientIp(request));
        }
        
        return keyBuilder.toString();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
