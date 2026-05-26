package cn.aiedge.common.rate;

/**
 * 限流器工厂类
 * 
 * 根据配置创建相应的限流器实例
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class RateLimiterFactory {
    
    /**
     * 根据配置创建限流器
     * 
     * @param config 限流配置
     * @return 限流器实例
     */
    public static RateLimiter createRateLimiter(RateLimitConfig.Rule config) {
        RateLimitConfig.Algorithm algo = config.getAlgorithm();
        if (algo == RateLimitConfig.Algorithm.TOKEN_BUCKET) {
            return new TokenBucketRateLimiter(config);
        } else if (algo == RateLimitConfig.Algorithm.LEAKY_BUCKET) {
            return new LeakyBucketRateLimiter(config);
        } else if (algo == RateLimitConfig.Algorithm.SLIDING_WINDOW) {
            return new ImprovedSlidingWindowRateLimiter(config);
        } else {
            return new TokenBucketRateLimiter(config);
        }
    }
    
    /**
     * 创建默认的令牌桶限流器
     * 
     * @return 令牌桶限流器实例
     */
    public static RateLimiter createDefaultRateLimiter() {
        RateLimitConfig.Rule rule = new RateLimitConfig.Rule();
        return new TokenBucketRateLimiter(rule);
    }
}