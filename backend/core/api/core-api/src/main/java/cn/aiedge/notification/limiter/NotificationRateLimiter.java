package cn.aiedge.notification.limiter;

import cn.aiedge.notification.config.NotificationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 通知发送限流器
 * 基于令牌桶算法实现
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
public class NotificationRateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final NotificationProperties properties;

    public NotificationRateLimiter(NotificationProperties properties) {
        this.properties = properties;
        
        // 初始化各渠道的限流桶
        for (Map.Entry<String, NotificationProperties.RateLimitConfig> entry 
                : properties.getRateLimit().entrySet()) {
            String channelType = entry.getKey();
            NotificationProperties.RateLimitConfig config = entry.getValue();
            buckets.put(channelType, new TokenBucket(config.getPermits(), config.getRate()));
            log.info("限流器初始化: channel={}, permits={}, rate={}/s", 
                channelType, config.getPermits(), config.getRate());
        }
    }

    /**
     * 尝试获取发送许可
     * 
     * @param channelType 渠道类型
     * @return 是否获取成功
     */
    public boolean tryAcquire(String channelType) {
        TokenBucket bucket = buckets.get(channelType);
        if (bucket == null) {
            // 未配置限流的渠道默认允许
            return true;
        }
        
        boolean acquired = bucket.tryAcquire();
        if (!acquired) {
            log.warn("发送限流触发: channel={}, 当前令牌数={}", channelType, bucket.getAvailableTokens());
        }
        return acquired;
    }

    /**
     * 尝试获取指定数量的发送许可
     * 
     * @param channelType 渠道类型
     * @param permits 请求数量
     * @return 是否获取成功
     */
    public boolean tryAcquire(String channelType, int permits) {
        TokenBucket bucket = buckets.get(channelType);
        if (bucket == null) {
            return true;
        }
        return bucket.tryAcquire(permits);
    }

    /**
     * 获取限流状态信息
     * 
     * @param channelType 渠道类型
     * @return 可用令牌数
     */
    public int getAvailableTokens(String channelType) {
        TokenBucket bucket = buckets.get(channelType);
        return bucket != null ? bucket.getAvailableTokens() : Integer.MAX_VALUE;
    }

    /**
     * 获取等待时间（毫秒）
     * 
     * @param channelType 渠道类型
     * @return 需要等待的时间，0表示无需等待
     */
    public long getWaitTimeMs(String channelType) {
        TokenBucket bucket = buckets.get(channelType);
        if (bucket == null || bucket.getAvailableTokens() > 0) {
            return 0;
        }
        return (long) (1000.0 / bucket.getRate());
    }

    /**
     * 令牌桶实现
     */
    private static class TokenBucket {
        private final int capacity;
        private final double rate;
        private double tokens;
        private long lastRefillTime;

        TokenBucket(int capacity, double rate) {
            this.capacity = capacity;
            this.rate = rate;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        synchronized boolean tryAcquire(int permits) {
            refill();
            if (tokens >= permits) {
                tokens -= permits;
                return true;
            }
            return false;
        }

        synchronized int getAvailableTokens() {
            refill();
            return (int) tokens;
        }

        double getRate() {
            return rate;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefillTime) / 1_000_000_000.0;
            double newTokens = elapsed * rate;
            
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTime = now;
            }
        }
    }
}
