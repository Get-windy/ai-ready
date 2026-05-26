package cn.aiedge.cache.ratelimit;

import java.util.concurrent.atomic.AtomicLong;
import java.time.Instant;

/**
 * Token Bucket algorithm implementation for rate limiting.
 * This algorithm allows for burst traffic up to the bucket capacity
 * while maintaining an average rate of consumption.
 */
public class TokenBucket implements RateLimiter {
    
    private final int capacity;           // Maximum number of tokens in the bucket
    private final double refillRate;      // Number of tokens added per second
    private final AtomicLong tokens;      // Current number of tokens in the bucket
    private final AtomicLong lastRefillTime; // Last time tokens were refilled
    
    /**
     * Constructor for TokenBucket
     * @param capacity Maximum number of tokens the bucket can hold
     * @param refillRate Rate at which tokens are added to the bucket (per second)
     */
    public TokenBucket(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicLong(capacity); // Start with full bucket
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }
    
    /**
     * Constructor for TokenBucket with a specific initial token count
     * @param capacity Maximum number of tokens the bucket can hold
     * @param refillRate Rate at which tokens are added to the bucket (per second)
     * @param initialTokens Initial number of tokens in the bucket
     */
    public TokenBucket(int capacity, double refillRate, int initialTokens) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicLong(Math.min(initialTokens, capacity));
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }
    
    /**
     * Attempts to consume the specified number of tokens from the bucket
     * @param numTokens Number of tokens to consume
     * @return true if tokens were successfully consumed, false otherwise
     */
    public synchronized boolean tryConsume(int numTokens) {
        refillTokens();
        
        if (tokens.get() >= numTokens) {
            tokens.addAndGet(-numTokens);
            return true;
        }
        
        return false;
    }
    
    /**
     * Attempts to consume the specified number of tokens from the bucket
     * with a maximum wait time
     * @param numTokens Number of tokens to consume
     * @param maxWaitMs Maximum time to wait in milliseconds
     * @return true if tokens were successfully consumed, false if timeout occurs
     */
    public synchronized boolean tryConsume(int numTokens, long maxWaitMs) {
        refillTokens();
        
        if (tokens.get() >= numTokens) {
            tokens.addAndGet(-numTokens);
            return true;
        }
        
        // Calculate how long we need to wait to accumulate enough tokens
        long tokensNeeded = numTokens - tokens.get();
        double timeToWaitMs = (tokensNeeded / refillRate) * 1000;
        
        if (timeToWaitMs <= maxWaitMs) {
            // Wait for the required time to accumulate tokens
            try {
                Thread.sleep((long) timeToWaitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            // Refill again after waiting
            refillTokens();
            
            if (tokens.get() >= numTokens) {
                tokens.addAndGet(-numTokens);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Refills the bucket with tokens based on the elapsed time since the last refill
     */
    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();
        
        if (currentTime > lastRefill) {
            long elapsedTimeMs = currentTime - lastRefill;
            double tokensToAdd = (elapsedTimeMs / 1000.0) * refillRate;
            
            // Only update if our thread is the one that gets to do the refill
            if (lastRefillTime.compareAndSet(lastRefill, currentTime)) {
                long currentTokens = tokens.get();
                long newTokens = Math.min(capacity, (long) (currentTokens + tokensToAdd));
                
                tokens.set(newTokens);
            }
        }
    }
    
    /**
     * Gets the current number of tokens in the bucket
     * @return Current number of tokens
     */
    public long getCurrentTokens() {
        refillTokens();
        return tokens.get();
    }
    
    /**
     * Gets the capacity of the bucket
     * @return Bucket capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Gets the refill rate of the bucket
     * @return Refill rate (tokens per second)
     */
    public double getRefillRate() {
        return refillRate;
    }
    
    // Implementation of RateLimiter interface methods
    
    @Override
    public boolean tryAcquire(int permits) {
        return tryConsume(permits);
    }
    
    @Override
    public boolean tryAcquire() {
        return tryConsume(1);
    }
    
    @Override
    public boolean tryAcquire(int permits, long timeoutMs) {
        return tryConsume(permits, timeoutMs);
    }
    
    @Override
    public int availablePermits() {
        return (int) getCurrentTokens();
    }
    
    @Override
    public String getConfigInfo() {
        return String.format("TokenBucket[capacity=%d, refillRate=%.2f, currentTokens=%d]", 
            capacity, refillRate, getCurrentTokens());
    }
}