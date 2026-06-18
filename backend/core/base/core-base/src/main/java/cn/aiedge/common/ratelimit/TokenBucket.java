package cn.aiedge.common.ratelimit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket Algorithm Implementation for Rate Limiting
 */
public class TokenBucket {
    private final long capacity;           // Maximum tokens the bucket can hold
    private final double refillRate;      // Tokens added per second
    private final ReentrantLock lock;     // Lock for thread safety
    
    private volatile AtomicLong tokens;   // Current number of tokens in the bucket
    private volatile long lastRefillTime; // Last time tokens were refilled

    /**
     * Constructor for TokenBucket
     * @param capacity Maximum number of tokens the bucket can hold
     * @param refillRate Number of tokens added per second
     */
    public TokenBucket(long capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTime = System.currentTimeMillis();
        this.lock = new ReentrantLock();
    }

    /**
     * Try to consume specified number of tokens from the bucket
     * @param numTokens Number of tokens to consume
     * @return true if tokens were successfully consumed, false otherwise
     */
    public boolean tryConsume(long numTokens) {
        lock.lock();
        try {
            refillTokens();
            
            if (tokens.get() >= numTokens) {
                tokens.addAndGet(-numTokens);
                return true;
            }
            
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refill tokens based on elapsed time since last refill
     */
    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastRefillTime;
        
        // Calculate tokens to add based on time elapsed and refill rate
        double tokensToAdd = (timeElapsed / 1000.0) * refillRate;
        
        if (tokensToAdd > 0) {
            long newTokens = Math.min(capacity, tokens.get() + (long) tokensToAdd);
            tokens.set(newTokens);
            lastRefillTime = currentTime;
        }
    }

    /**
     * Get current number of tokens in the bucket
     * @return Current token count
     */
    public long getCurrentTokens() {
        refillTokens(); // Ensure we have the most accurate count
        return tokens.get();
    }

    /**
     * Get bucket capacity
     * @return Bucket capacity
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Get refill rate (tokens per second)
     * @return Refill rate
     */
    public double getRefillRate() {
        return refillRate;
    }
}