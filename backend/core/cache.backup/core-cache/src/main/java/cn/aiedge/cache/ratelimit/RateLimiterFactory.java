package cn.aiedge.cache.ratelimit;

/**
 * Factory class for creating different types of rate limiters.
 * Provides a centralized way to instantiate rate limiting algorithms.
 */
public class RateLimiterFactory {
    
    /**
     * Creates a TokenBucket rate limiter
     * @param capacity Maximum number of tokens the bucket can hold
     * @param refillRate Rate at which tokens are added to the bucket (per second)
     * @return A new TokenBucket instance
     */
    public static RateLimiter createTokenBucket(int capacity, double refillRate) {
        return new TokenBucket(capacity, refillRate);
    }
    
    /**
     * Creates a TokenBucket rate limiter with initial tokens
     * @param capacity Maximum number of tokens the bucket can hold
     * @param refillRate Rate at which tokens are added to the bucket (per second)
     * @param initialTokens Initial number of tokens in the bucket
     * @return A new TokenBucket instance
     */
    public static RateLimiter createTokenBucket(int capacity, double refillRate, int initialTokens) {
        return new TokenBucket(capacity, refillRate, initialTokens);
    }
    
    /**
     * Creates a LeakyBucket rate limiter
     * @param capacity Maximum number of requests the bucket can hold
     * @param leakRate Rate at which requests are processed (leak rate) in requests per second
     * @return A new LeakyBucket instance
     */
    public static RateLimiter createLeakyBucket(int capacity, double leakRate) {
        return new LeakyBucket(capacity, leakRate);
    }
    
    /**
     * Creates a SlidingWindowCounter rate limiter
     * @param limit Maximum number of requests allowed in the time window
     * @param windowSizeInMs Size of the time window in milliseconds
     * @return A new SlidingWindowCounter instance
     */
    public static RateLimiter createSlidingWindow(int limit, long windowSizeInMs) {
        return new SlidingWindowCounter(limit, windowSizeInMs);
    }
    
    /**
     * Creates a SlidingWindowCounter rate limiter with default 1-minute window
     * @param limit Maximum number of requests allowed in the time window
     * @return A new SlidingWindowCounter instance
     */
    public static RateLimiter createSlidingWindow(int limit) {
        return new SlidingWindowCounter(limit);
    }
    
    /**
     * Creates a rate limiter based on the specified algorithm using builder pattern
     * @param algorithm The type of rate limiting algorithm to create
     * @param builder Builder with configuration parameters
     * @return A new RateLimiter instance
     */
    public static RateLimiter create(Algorithm algorithm, RateLimiterParams.Builder builder) {
        RateLimiterParams params = builder.build();
        return create(algorithm, params);
    }
    
    /**
     * Enum representing the different rate limiting algorithms
     */
    public enum Algorithm {
        TOKEN_BUCKET,
        LEAKY_BUCKET,
        SLIDING_WINDOW
    }
    
    /**
     * Creates a rate limiter based on the specified algorithm
     * @param algorithm The type of rate limiting algorithm to create
     * @param params Parameters for configuring the rate limiter
     * @return A new RateLimiter instance
     */
    public static RateLimiter create(Algorithm algorithm, RateLimiterParams params) {
        switch (algorithm) {
            case TOKEN_BUCKET:
                // Use the 3-parameter constructor that includes initial tokens
                return new TokenBucket(
                    params.getCapacity(), 
                    params.getRefillRate(), 
                    params.getInitialTokens()
                );
            case LEAKY_BUCKET:
                return new LeakyBucket(params.getCapacity(), params.getLeakRate());
            case SLIDING_WINDOW:
                return new SlidingWindowCounter(params.getLimit(), params.getWindowSizeInMs());
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }
}