package cn.aiedge.common.algorithm.ratelimit;

/**
 * Example usage of the LeakyBucket rate limiting algorithm
 */
public class LeakyBucketExample {
    public static void main(String[] args) throws InterruptedException {
        // Create a leaky bucket with capacity of 5 requests and leak rate of 1 request per second
        LeakyBucket bucket = new LeakyBucket(5, 1);
        
        System.out.println("Leaky Bucket Rate Limiter Example");
        System.out.println("Capacity: " + bucket.getCapacity() + ", Leak Rate: " + bucket.getLeakRate() + " req/sec");
        System.out.println();

        // Simulate making requests
        for (int i = 1; i <= 8; i++) {
            boolean allowed = bucket.request();
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "REJECTED"));
            
            // Small delay between requests to simulate real usage
            Thread.sleep(200);
        }
        
        System.out.println("\nWaiting 3 seconds to allow bucket to drain...");
        Thread.sleep(3000);
        
        // Try a few more requests after waiting
        for (int i = 1; i <= 3; i++) {
            boolean allowed = bucket.request();
            System.out.println("Request " + (i + 8) + ": " + (allowed ? "ALLOWED" : "REJECTED"));
        }
        
        System.out.println("\nFinal water level: " + bucket.getCurrentWaterLevel());
    }
}