package cn.aiedge.common.algorithm.ratelimit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Leaky Bucket Algorithm implementation for rate limiting
 * The leaky bucket algorithm allows requests at a fixed rate,
 * smoothing out burst traffic to a constant flow.
 */
public class LeakyBucket {
    private final int capacity;           // Bucket capacity (max requests allowed)
    private final int leakRate;          // Leak rate (requests per second)
    private final AtomicLong waterLevel; // Current water level (pending requests)
    private final AtomicLong lastUpdateTime;

    /**
     * Constructor for LeakyBucket
     * @param capacity Maximum capacity of the bucket
     * @param leakRate Rate at which requests are processed (per second)
     */
    public LeakyBucket(int capacity, int leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.waterLevel = new AtomicLong(0);
        this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * Process a request through the leaky bucket algorithm
     * @param requestCount Number of requests to process (default 1)
     * @return true if the request is allowed, false if rate limited
     */
    public boolean request(int requestCount) {
        // Get current time
        long currentTime = System.currentTimeMillis();
        
        // Calculate time elapsed since last update in seconds
        long elapsedTimeMs = currentTime - lastUpdateTime.get();
        double elapsedTimeSec = elapsedTimeMs / 1000.0;
        
        // Calculate how much water has leaked out during this time
        double leakedWater = elapsedTimeSec * leakRate;
        
        // Update water level by removing leaked water and adding new requests
        long currentWaterLevel = waterLevel.get();
        
        // Use CAS to ensure atomic update
        while (true) {
            long oldWaterLevel = waterLevel.get();
            
            // Calculate new water level after leakage and new requests
            double newWaterLevel = Math.max(0, oldWaterLevel - leakedWater) + requestCount;
            
            // If new water level exceeds capacity, reject the request
            if (newWaterLevel > capacity) {
                return false;
            }
            
            // Update the water level atomically
            if (waterLevel.compareAndSet(oldWaterLevel, (long) Math.ceil(newWaterLevel))) {
                break;
            }
        }
        
        // Update the last update time atomically
        lastUpdateTime.set(currentTime);
        
        return true;
    }

    /**
     * Process a single request through the leaky bucket algorithm
     * @return true if the request is allowed, false if rate limited
     */
    public boolean request() {
        return request(1);
    }

    /**
     * Get current water level in the bucket
     * @return current water level
     */
    public long getCurrentWaterLevel() {
        // Calculate current water level considering leakage since last update
        long currentTime = System.currentTimeMillis();
        long lastTime = lastUpdateTime.get();
        long elapsedTimeMs = currentTime - lastTime;
        double elapsedTimeSec = elapsedTimeMs / 1000.0;
        
        long currentWaterLevel = waterLevel.get();
        double leakedWater = elapsedTimeSec * leakRate;
        return (long) Math.max(0, currentWaterLevel - leakedWater);
    }

    /**
     * Get bucket capacity
     * @return bucket capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Get leak rate (requests per second)
     * @return leak rate
     */
    public int getLeakRate() {
        return leakRate;
    }
}