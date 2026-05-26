package cn.aiedge.cache.ratelimit;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * Leaky Bucket algorithm implementation for rate limiting.
 * This algorithm processes requests at a fixed rate regardless of the incoming rate,
 * effectively smoothing out burst traffic.
 */
public class LeakyBucket implements RateLimiter {
    
    private final int capacity;                    // Maximum number of requests the bucket can hold
    private final double leakRate;                 // Rate at which requests leak from the bucket (requests per second)
    private final ReentrantLock lock;              // Lock for thread safety
    private volatile long lastUpdateTime;          // Last time the bucket was updated
    private volatile int currentWaterLevel;        // Current number of requests in the bucket (water level)
    
    /**
     * Constructor for LeakyBucket
     * @param capacity Maximum number of requests the bucket can hold
     * @param leakRate Rate at which requests are processed (leak rate) in requests per second
     */
    public LeakyBucket(int capacity, double leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.lock = new ReentrantLock();
        this.lastUpdateTime = System.currentTimeMillis();
        this.currentWaterLevel = 0;
    }
    
    /**
     * Attempts to process a request through the leaky bucket
     * @param numRequests Number of requests to process (default is 1)
     * @return true if the request is accepted, false if the bucket is full (overflow)
     */
    public boolean request(int numRequests) {
        lock.lock();
        try {
            // First, leak water based on elapsed time since last update
            leakWater();
            
            // Check if adding the request would exceed the bucket capacity
            if (currentWaterLevel + numRequests <= capacity) {
                // Add the request to the bucket (increase water level)
                currentWaterLevel += numRequests;
                return true;
            } else {
                // Bucket overflow - reject the request
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Attempts to process a single request through the leaky bucket
     * @return true if the request is accepted, false if the bucket is full
     */
    public boolean request() {
        return request(1);
    }
    
    /**
     * Processes water leakage based on elapsed time since last update
     */
    private void leakWater() {
        long currentTime = System.currentTimeMillis();
        long elapsedTimeMs = currentTime - lastUpdateTime;
        
        // Calculate how much water should have leaked based on elapsed time
        double leakedAmount = (elapsedTimeMs / 1000.0) * leakRate;
        
        // Update the water level by subtracting the leaked amount
        currentWaterLevel = Math.max(0, (int) (currentWaterLevel - leakedAmount));
        
        // Update the last update time
        lastUpdateTime = currentTime;
    }
    
    /**
     * Gets the current water level in the bucket (number of pending requests)
     * @return Current water level
     */
    public int getCurrentWaterLevel() {
        lock.lock();
        try {
            leakWater(); // Ensure water level is up to date
            return currentWaterLevel;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets the capacity of the bucket
     * @return Bucket capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Gets the leak rate of the bucket
     * @return Leak rate (requests per second)
     */
    public double getLeakRate() {
        return leakRate;
    }
    
    /**
     * Gets the percentage of bucket utilization
     * @return Utilization percentage (0.0 to 100.0)
     */
    public double getUtilizationPercentage() {
        lock.lock();
        try {
            leakWater(); // Ensure water level is up to date
            return ((double) currentWaterLevel / capacity) * 100.0;
        } finally {
            lock.unlock();
        }
    }
    
    // Implementation of RateLimiter interface methods
    
    @Override
    public boolean tryAcquire(int permits) {
        return request(permits);
    }
    
    @Override
    public boolean tryAcquire() {
        return request(1);
    }
    
    @Override
    public boolean tryAcquire(int permits, long timeoutMs) {
        // For leaky bucket, we don't wait as it processes requests at a fixed rate
        // This is a simplified implementation - in a real system you might want to
        // implement waiting logic based on the leak rate
        return request(permits);
    }
    
    @Override
    public int availablePermits() {
        lock.lock();
        try {
            leakWater(); // Ensure water level is up to date
            return capacity - currentWaterLevel; // Available capacity
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public String getConfigInfo() {
        return String.format("LeakyBucket[capacity=%d, leakRate=%.2f, currentWaterLevel=%d]", 
            capacity, leakRate, getCurrentWaterLevel());
    }
}