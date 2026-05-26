package cn.aiedge.cache.ratelimit;

/**
 * Parameter class for configuring rate limiters.
 * Provides a standardized way to pass configuration parameters to rate limiter factories.
 */
public class RateLimiterParams {
    
    private final int capacity;            // Capacity for token bucket or leaky bucket
    private final double refillRate;       // Refill rate for token bucket (tokens per second)
    private final double leakRate;         // Leak rate for leaky bucket (requests per second)
    private final int limit;               // Limit for sliding window counter
    private final long windowSizeInMs;     // Window size for sliding window counter
    private final int initialTokens;       // Initial tokens for token bucket
    

    
    /**
     * Full constructor for all parameters
     */
    public RateLimiterParams(int capacity, double refillRate, double leakRate, 
                            int limit, long windowSizeInMs, int initialTokens) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.leakRate = leakRate;
        this.limit = limit;
        this.windowSizeInMs = windowSizeInMs;
        this.initialTokens = initialTokens;
    }
    
    // Getters
    public int getCapacity() {
        return capacity;
    }
    
    public double getRefillRate() {
        return refillRate;
    }
    
    public double getLeakRate() {
        return leakRate;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public long getWindowSizeInMs() {
        return windowSizeInMs;
    }
    
    public int getInitialTokens() {
        return initialTokens;
    }
    
    /**
     * Builder pattern for creating RateLimiterParams
     */
    public static class Builder {
        private int capacity = 10;
        private double refillRate = 1.0;
        private double leakRate = 1.0;
        private int limit = 10;
        private long windowSizeInMs = 60000; // 1 minute default
        private int initialTokens = 0;
        
        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }
        
        public Builder refillRate(double refillRate) {
            this.refillRate = refillRate;
            return this;
        }
        
        public Builder leakRate(double leakRate) {
            this.leakRate = leakRate;
            return this;
        }
        
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        public Builder windowSizeInMs(long windowSizeInMs) {
            this.windowSizeInMs = windowSizeInMs;
            return this;
        }
        
        public Builder initialTokens(int initialTokens) {
            this.initialTokens = initialTokens;
            return this;
        }
        
        public RateLimiterParams build() {
            return new RateLimiterParams(
                capacity, refillRate, leakRate, 
                limit, windowSizeInMs, initialTokens
            );
        }
    }
}