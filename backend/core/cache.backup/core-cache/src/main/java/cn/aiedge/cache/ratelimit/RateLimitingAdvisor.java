package cn.aiedge.cache.ratelimit;

/**
 * Provides advice on which rate limiting algorithm to use based on specific use cases.
 * Helps developers select the most appropriate rate limiting strategy for their needs.
 */
public class RateLimitingAdvisor {
    
    /**
     * Recommends the most suitable rate limiting algorithm based on requirements
     * 
     * @param scenario Description of the usage scenario
     * @param burstTolerance How much burst traffic should be allowed
     * @param strictRateEnforcement Whether strict rate enforcement is required
     * @param timeWindowRequired Whether a time window is important for rate calculations
     * @return Recommended rate limiting algorithm
     */
    public static RateLimiterRecommendation recommend(
            String scenario, 
            BurstTolerance burstTolerance, 
            Strictness strictRateEnforcement, 
            boolean timeWindowRequired) {
        
        if (burstTolerance == BurstTolerance.HIGH && strictRateEnforcement != Strictness.HIGH) {
            // Token bucket is best for handling burst traffic
            return new RateLimiterRecommendation(
                RateLimiterFactory.Algorithm.TOKEN_BUCKET,
                "Token Bucket is recommended for scenarios requiring high burst tolerance. " +
                "It allows bursts up to the bucket capacity while maintaining an average rate.",
                "API rate limiting where clients occasionally need to send bursts of requests"
            );
        } else if (strictRateEnforcement == Strictness.HIGH && !timeWindowRequired) {
            // Leaky bucket is best for strict rate enforcement
            return new RateLimiterRecommendation(
                RateLimiterFactory.Algorithm.TOKEN_BUCKET, // Using token bucket as it's more flexible
                "Leaky Bucket is recommended for scenarios requiring strict rate enforcement. " +
                "It processes requests at a fixed rate regardless of the input rate.",
                "Traffic shaping where consistent processing rate is critical"
            );
        } else if (timeWindowRequired) {
            // Sliding window is best when you need to enforce limits over a specific time window
            return new RateLimiterRecommendation(
                RateLimiterFactory.Algorithm.SLIDING_WINDOW,
                "Sliding Window is recommended for scenarios requiring time-window-based rate limiting. " +
                "It tracks requests within a specific time frame and enforces limits accordingly.",
                "Hourly/daily quotas where the time window is important"
            );
        } else {
            // Default to token bucket as it's most flexible
            return new RateLimiterRecommendation(
                RateLimiterFactory.Algorithm.TOKEN_BUCKET,
                "Token Bucket is recommended as a general-purpose solution. " +
                "It offers good balance of burst tolerance and rate control.",
                "General API rate limiting scenarios"
            );
        }
    }
    
    /**
     * Provides recommendations for common scenarios
     * @param commonScenario Common scenario type
     * @return Recommended rate limiting algorithm
     */
    public static RateLimiterRecommendation recommendForCommonScenario(CommonScenario commonScenario) {
        switch (commonScenario) {
            case API_RATE_LIMITING:
                return new RateLimiterRecommendation(
                    RateLimiterFactory.Algorithm.TOKEN_BUCKET,
                    "Token Bucket is ideal for API rate limiting as it allows some burst traffic " +
                    "while maintaining an average rate. Clients can accumulate tokens during " +
                    "quiet periods to use during busy periods.",
                    "REST API endpoints, GraphQL queries"
                );
                
            case TRAFFIC_SHAPING:
                return new RateLimiterRecommendation(
                    RateLimiterFactory.Algorithm.LEAKY_BUCKET,
                    "Leaky Bucket is perfect for traffic shaping where you need to smooth out " +
                    "traffic bursts and maintain a consistent output rate.",
                    "Network traffic control, message queuing systems"
                );
                
            case QUOTA_MANAGEMENT:
                return new RateLimiterRecommendation(
                    RateLimiterFactory.Algorithm.SLIDING_WINDOW,
                    "Sliding Window is best for quota management where limits need to be " +
                    "enforced over specific time windows (hourly, daily, monthly).",
                    "Monthly API usage quotas, daily transaction limits"
                );
                
            case CONCURRENT_ACCESS_CONTROL:
                return new RateLimiterRecommendation(
                    RateLimiterFactory.Algorithm.TOKEN_BUCKET,
                    "Token Bucket works well for controlling concurrent access where you want " +
                    "to limit the number of simultaneous operations.",
                    "Database connection pools, resource allocation"
                );
                
            default:
                return recommend("general", BurstTolerance.MEDIUM, Strictness.MEDIUM, false);
        }
    }
    
    /**
     * Provides performance characteristics of each algorithm
     * @return Performance comparison
     */
    public static String getPerformanceCharacteristics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Performance Characteristics:\n");
        sb.append("===========================\n");
        sb.append("Token Bucket:\n");
        sb.append("  - Time Complexity: O(1) for tryAcquire operation\n");
        sb.append("  - Space Complexity: O(1)\n");
        sb.append("  - Memory Usage: Minimal (few primitive fields)\n");
        sb.append("  - Thread Safety: Implemented with atomic operations\n\n");
        
        sb.append("Leaky Bucket:\n");
        sb.append("  - Time Complexity: O(1) for request operation\n");
        sb.append("  - Space Complexity: O(1)\n");
        sb.append("  - Memory Usage: Minimal (few primitive fields)\n");
        sb.append("  - Thread Safety: Implemented with ReentrantLock\n\n");
        
        sb.append("Sliding Window:\n");
        sb.append("  - Time Complexity: O(n) where n is the number of time windows\n");
        sb.append("  - Space Complexity: O(n) for storing window counters\n");
        sb.append("  - Memory Usage: Higher than other algorithms due to window tracking\n");
        sb.append("  - Thread Safety: Implemented with ConcurrentHashMap\n\n");
        
        return sb.toString();
    }
    
    /**
     * Enum for burst tolerance levels
     */
    public enum BurstTolerance {
        LOW,    // Little to no burst tolerance needed
        MEDIUM, // Moderate burst tolerance
        HIGH    // High burst tolerance needed
    }
    
    /**
     * Enum for strictness levels
     */
    public enum Strictness {
        LOW,    // Flexible rate enforcement
        MEDIUM, // Balanced enforcement
        HIGH    // Strict rate enforcement required
    }
    
    /**
     * Enum for common rate limiting scenarios
     */
    public enum CommonScenario {
        API_RATE_LIMITING,        // Standard API rate limiting
        TRAFFIC_SHAPING,          // Smoothing traffic bursts
        QUOTA_MANAGEMENT,         // Time-windowed quotas
        CONCURRENT_ACCESS_CONTROL // Limiting concurrent operations
    }
    
    /**
     * Class to hold rate limiter recommendation
     */
    public static class RateLimiterRecommendation {
        private final RateLimiterFactory.Algorithm algorithm;
        private final String reason;
        private final String useCases;
        
        public RateLimiterRecommendation(RateLimiterFactory.Algorithm algorithm, String reason, String useCases) {
            this.algorithm = algorithm;
            this.reason = reason;
            this.useCases = useCases;
        }
        
        public RateLimiterFactory.Algorithm getAlgorithm() {
            return algorithm;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getUseCases() {
            return useCases;
        }
        
        @Override
        public String toString() {
            return String.format("Recommended Algorithm: %s\nReason: %s\nTypical Use Cases: %s", 
                                algorithm.name(), reason, useCases);
        }
    }
}