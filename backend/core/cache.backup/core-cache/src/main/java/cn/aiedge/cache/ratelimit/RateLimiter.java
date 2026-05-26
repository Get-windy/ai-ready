package cn.aiedge.cache.ratelimit;

/**
 * Interface defining the contract for rate limiting algorithms.
 * Provides a common interface for different rate limiting implementations.
 */
public interface RateLimiter {
    
    /**
     * Attempts to acquire the specified number of permits.
     * @param permits Number of permits to acquire
     * @return true if the permits were acquired, false otherwise
     */
    boolean tryAcquire(int permits);
    
    /**
     * Attempts to acquire a single permit.
     * @return true if the permit was acquired, false otherwise
     */
    boolean tryAcquire();
    
    /**
     * Attempts to acquire the specified number of permits with a maximum wait time.
     * @param permits Number of permits to acquire
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if the permits were acquired, false if timeout occurred
     */
    boolean tryAcquire(int permits, long timeoutMs);
    
    /**
     * Gets the available permits at the current time.
     * @return Number of available permits
     */
    int availablePermits();
    
    /**
     * Gets the rate limiter's configuration details as a string.
     * @return Configuration details
     */
    String getConfigInfo();
}