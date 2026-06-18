package cn.aiedge.common.rate;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Leaky Bucket Rate Limiter Implementation
 * 
 * Based on the leaky bucket algorithm, providing smooth request processing rate
 * 
 * Algorithm characteristics:
 * - Constant processing rate
 * - Smooth output, handling burst traffic
 * - Limiting average rate rather than instantaneous rate
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class LeakyBucketRateLimiter implements RateLimiter {
    
    private volatile long waterLevel;    // Current water level (unprocessed requests)
    private final long capacity;         // Bucket capacity
    private final long leakRate;         // Leak rate (requests processed per second)
    private final ReentrantLock lock = new ReentrantLock();
    
    private volatile long lastUpdateTime; // Last update time
    private final RateLimitConfig.Rule config;
    
    /**
     * Constructor
     * 
     * @param config Rate limit configuration
     */
    public LeakyBucketRateLimiter(RateLimitConfig.Rule config) {
        this.capacity = config.getCapacity();
        this.leakRate = config.getRefillRate();
        this.waterLevel = 0; // Initially empty bucket
        this.lastUpdateTime = System.currentTimeMillis();
        this.config = config;
    }
    
    @Override
    public boolean tryAcquire(int permits) {
        lock.lock();
        try {
            // Update water level: first reduce water according to time elapsed, then add new requests
            updateWaterLevel();
            
            // Check if adding new requests would exceed bucket capacity
            long newWaterLevel = waterLevel + permits;
            
            // If exceeds bucket capacity, reject request
            if (newWaterLevel > capacity) {
                return false;
            }
            
            // Update water level (add new requests)
            waterLevel = newWaterLevel;
            
            return true;
            
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean tryAcquire(int permits, long timeout) {
        // Leaky bucket algorithm makes immediate decision, does not support waiting for permits
        return tryAcquire(permits);
    }
    
    @Override
    public long availablePermits() {
        // For leaky bucket algorithm, the concept of available permits differs from token bucket
        // Return remaining capacity as reference value
        lock.lock();
        try {
            // First update water level to reflect current state
            updateWaterLevel();
            
            return Math.max(0, capacity - waterLevel);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public RateLimitConfig.Rule getConfig() {
        return config;
    }
    
    /**
     * Update water level based on time elapsed (simulate leak process)
     */
    private void updateWaterLevel() {
        long currentTime = System.currentTimeMillis();
        long elapsedTimeMs = currentTime - lastUpdateTime;
        
        if (elapsedTimeMs > 0) {
            // Calculate amount of water leaked during this period
            double elapsedTimeSec = elapsedTimeMs / 1000.0;
            double leakedWater = elapsedTimeSec * leakRate;
            
            // Reduce water level, but not less than 0
            waterLevel = Math.max(0, (long)(waterLevel - leakedWater));
            lastUpdateTime = currentTime;
        }
    }
    
    /**
     * Get current water level
     * 
     * @return Current water level
     */
    public long getCurrentWaterLevel() {
        lock.lock();
        try {
            // First update water level to reflect current state
            updateWaterLevel();
            
            return waterLevel;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get bucket capacity
     * 
     * @return Bucket capacity
     */
    public long getCapacity() {
        return capacity;
    }
    
    /**
     * Get leak rate
     * 
     * @return Leak rate
     */
    public long getLeakRate() {
        return leakRate;
    }
}