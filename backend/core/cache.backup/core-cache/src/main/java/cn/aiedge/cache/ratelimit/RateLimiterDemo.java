package cn.aiedge.cache.ratelimit;

/**
 * Demo application showcasing the usage of different rate limiting algorithms.
 * Demonstrates practical applications of Token Bucket, Leaky Bucket, and Sliding Window algorithms.
 */
public class RateLimiterDemo {
    
    public static void main(String[] args) {
        System.out.println("Rate Limiter Algorithms Demo");
        System.out.println("=============================\n");
        
        // Demo 1: Token Bucket - Good for API rate limiting with burst allowance
        demonstrateTokenBucket();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 2: Leaky Bucket - Good for traffic shaping
        demonstrateLeakyBucket();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 3: Sliding Window - Good for time-windowed quotas
        demonstrateSlidingWindow();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 4: Rate limiting advisor
        demonstrateAdvisor();
    }
    
    private static void demonstrateTokenBucket() {
        System.out.println("Demo 1: Token Bucket Algorithm");
        System.out.println("-------------------------------");
        
        // Create a token bucket: capacity 5, refill rate 2 tokens/sec
        RateLimiter rateLimiter = RateLimiterFactory.createTokenBucket(5, 2.0);
        System.out.println("Created Token Bucket: " + rateLimiter.getConfigInfo());
        
        System.out.println("Attempting 8 requests rapidly:");
        for (int i = 0; i < 8; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            System.out.printf("Request %d: %s%n", i + 1, allowed ? "ALLOWED" : "DENIED");
            
            // Small delay to simulate processing
            try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
        }
        
        System.out.println("Current available permits: " + rateLimiter.availablePermits());
    }
    
    private static void demonstrateLeakyBucket() {
        System.out.println("Demo 2: Leaky Bucket Algorithm");
        System.out.println("------------------------------");
        
        // Create a leaky bucket: capacity 5, leak rate 1 request/sec
        RateLimiter rateLimiter = RateLimiterFactory.createLeakyBucket(5, 1.0);
        System.out.println("Created Leaky Bucket: " + rateLimiter.getConfigInfo());
        
        System.out.println("Attempting 8 requests rapidly (simulating burst):");
        for (int i = 0; i < 8; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            System.out.printf("Request %d: %s%n", i + 1, allowed ? "ALLOWED" : "DENIED");
        }
        
        System.out.println("Current available permits: " + rateLimiter.availablePermits());
        
        // Wait and try again to see leak effect
        System.out.println("Waiting 3 seconds to see leak effect...");
        try { Thread.sleep(3000); } catch (InterruptedException e) { /* ignore */ }
        
        System.out.println("Trying 2 more requests after waiting:");
        for (int i = 0; i < 2; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            System.out.printf("Request %d: %s%n", i + 1, allowed ? "ALLOWED" : "DENIED");
        }
    }
    
    private static void demonstrateSlidingWindow() {
        System.out.println("Demo 3: Sliding Window Algorithm");
        System.out.println("-------------------------------");
        
        // Create a sliding window: 5 requests per 2 seconds
        RateLimiter rateLimiter = RateLimiterFactory.createSlidingWindow(5, 2000L);
        System.out.println("Created Sliding Window: " + rateLimiter.getConfigInfo());
        
        System.out.println("Attempting 7 requests rapidly:");
        for (int i = 0; i < 7; i++) {
            boolean allowed = rateLimiter.tryAcquire();
            System.out.printf("Request %d: %s%n", i + 1, allowed ? "ALLOWED" : "DENIED");
            
            // Small delay to simulate processing
            try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
        }
        
        System.out.println("Current usage: " + String.format("%.2f%%", 
                  (5 - rateLimiter.availablePermits()) / 5.0 * 100));
    }
    
    private static void demonstrateAdvisor() {
        System.out.println("Demo 4: Rate Limiting Advisor");
        System.out.println("-----------------------------");
        
        // Demonstrate advisor recommendations
        System.out.println("Recommendation for API Rate Limiting:");
        var apiRecommendation = RateLimitingAdvisor.recommendForCommonScenario(
            RateLimitingAdvisor.CommonScenario.API_RATE_LIMITING);
        System.out.println(apiRecommendation);
        
        System.out.println("\nRecommendation for Traffic Shaping:");
        var trafficRecommendation = RateLimitingAdvisor.recommendForCommonScenario(
            RateLimitingAdvisor.CommonScenario.TRAFFIC_SHAPING);
        System.out.println(trafficRecommendation);
        
        System.out.println("\nRecommendation for Quota Management:");
        var quotaRecommendation = RateLimitingAdvisor.recommendForCommonScenario(
            RateLimitingAdvisor.CommonScenario.QUOTA_MANAGEMENT);
        System.out.println(quotaRecommendation);
        
        System.out.println("\nPerformance Characteristics:");
        System.out.println(RateLimitingAdvisor.getPerformanceCharacteristics());
    }
}