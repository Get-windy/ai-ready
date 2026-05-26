package cn.aiedge.cache.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding Window Counter algorithm implementation for rate limiting.
 * This algorithm keeps track of requests within a specific time window,
 * providing accurate request counting across time boundaries.
 */
public class SlidingWindowCounter implements RateLimiter {
    
    private final int limit;                           // Maximum number of requests allowed in the time window
    private final long windowSizeInMs;                 // Size of the time window in milliseconds
    private final Map<Long, AtomicInteger> counters;   // Counters for each time window
    private final Object lock;                         // Lock for thread safety
    
    /**
     * Constructor for SlidingWindowCounter
     * @param limit Maximum number of requests allowed in the time window
     * @param windowSizeInMs Size of the time window in milliseconds
     */
    public SlidingWindowCounter(int limit, long windowSizeInMs) {
        this.limit = limit;
        this.windowSizeInMs = windowSizeInMs;
        this.counters = new ConcurrentHashMap<>();
        this.lock = new Object();
    }
    
    /**
     * Constructor for SlidingWindowCounter with default window size of 1 minute
     * @param limit Maximum number of requests allowed in the time window
     */
    public SlidingWindowCounter(int limit) {
        this(limit, 60000); // Default to 1-minute window
    }
    
    /**
     * Attempts to increment the counter for the current time window
     * @return true if the increment was successful and within the limit, false otherwise
     */
    public boolean increment() {
        return increment(1);
    }
    
    /**
     * Attempts to increment the counter by the specified count
     * @param count Number of requests to increment the counter by
     * @return true if the increment was successful and within the limit, false otherwise
     */
    public boolean increment(int count) {
        long currentTimeWindow = getCurrentTimeWindow();
        long previousTimeWindow = currentTimeWindow - windowSizeInMs;
        
        // Clean up expired windows
        cleanupExpiredWindows(currentTimeWindow);
        
        // Get the current window counter
        AtomicInteger currentCounter = counters.computeIfAbsent(
            currentTimeWindow, k -> new AtomicInteger(0)
        );
        
        // Get the previous window counter if it exists
        AtomicInteger previousCounter = counters.get(previousTimeWindow);
        int previousCount = (previousCounter != null) ? previousCounter.get() : 0;
        
        // Calculate the proportional count from the previous window
        // based on how much of the previous window is still within our current window
        long timeIntoCurrentWindow = System.currentTimeMillis() % windowSizeInMs;
        double previousWindowWeight = (windowSizeInMs - timeIntoCurrentWindow) / (double) windowSizeInMs;
        int weightedPreviousCount = (int) (previousCount * previousWindowWeight);
        
        // Calculate the total count in the sliding window
        int totalCount = weightedPreviousCount + currentCounter.get() + count;
        
        // Check if the total count exceeds the limit
        if (totalCount <= limit) {
            // Increment the counter for the current window
            currentCounter.addAndGet(count);
            return true;
        }
        
        return false;
    }
    
    /**
     * Cleans up expired time windows to prevent memory leaks
     * @param currentTimeWindow The current time window
     */
    private void cleanupExpiredWindows(long currentTimeWindow) {
        // Remove all windows that are older than the sliding window size
        counters.entrySet().removeIf(entry -> 
            entry.getKey() < (currentTimeWindow - windowSizeInMs)
        );
    }
    
    /**
     * Gets the current time window based on the current time and window size
     * @return The current time window identifier
     */
    private long getCurrentTimeWindow() {
        long currentTime = System.currentTimeMillis();
        return (currentTime / windowSizeInMs) * windowSizeInMs;
    }
    
    /**
     * Gets the current count in the sliding window
     * @return The current request count in the sliding window
     */
    public int getCurrentCount() {
        long currentTimeWindow = getCurrentTimeWindow();
        long previousTimeWindow = currentTimeWindow - windowSizeInMs;
        
        // Clean up expired windows
        cleanupExpiredWindows(currentTimeWindow);
        
        // Get the current window counter
        AtomicInteger currentCounter = counters.get(currentTimeWindow);
        int currentCount = (currentCounter != null) ? currentCounter.get() : 0;
        
        // Get the previous window counter if it exists
        AtomicInteger previousCounter = counters.get(previousTimeWindow);
        int previousCount = (previousCounter != null) ? previousCounter.get() : 0;
        
        // Calculate the proportional count from the previous window
        long timeIntoCurrentWindow = System.currentTimeMillis() % windowSizeInMs;
        double previousWindowWeight = (windowSizeInMs - timeIntoCurrentWindow) / (double) windowSizeInMs;
        int weightedPreviousCount = (int) (previousCount * previousWindowWeight);
        
        return weightedPreviousCount + currentCount;
    }
    
    /**
     * Gets the maximum allowed requests in the time window
     * @return The limit for the sliding window
     */
    public int getLimit() {
        return limit;
    }
    
    /**
     * Gets the size of the time window in milliseconds
     * @return The window size in milliseconds
     */
    public long getWindowSizeInMs() {
        return windowSizeInMs;
    }
    
    /**
     * Gets the percentage of the limit that is currently used
     * @return Usage percentage (0.0 to 100.0)
     */
    public double getUsagePercentage() {
        return ((double) getCurrentCount() / limit) * 100.0;
    }
    
    /**
     * Resets all counters to zero
     */
    public void reset() {
        synchronized (lock) {
            counters.clear();
        }
    }
    
    // Implementation of RateLimiter interface methods
    
    @Override
    public boolean tryAcquire(int permits) {
        return increment(permits);
    }
    
    @Override
    public boolean tryAcquire() {
        return increment(1);
    }
    
    @Override
    public boolean tryAcquire(int permits, long timeoutMs) {
        // For sliding window counter, we don't wait - either the request fits in the window or it doesn't
        // This is a simplified implementation - in a real system you might want to implement
        // waiting logic if needed
        return increment(permits);
    }
    
    @Override
    public int availablePermits() {
        int currentCount = getCurrentCount();
        return Math.max(0, limit - currentCount);
    }
    
    @Override
    public String getConfigInfo() {
        return String.format("SlidingWindowCounter[limit=%d, windowSize=%dms, currentCount=%d]", 
            limit, windowSizeInMs, getCurrentCount());
    }
}