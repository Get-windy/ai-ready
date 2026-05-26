package cn.aiedge.common.rate;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 令牌桶算法限流器实现
 * 
 * 令牌桶算法特点：
 * - 以固定速率向桶中添加令牌
 * - 请求需要消耗令牌才能通过
 * - 桶有最大容量，超出的令牌会被丢弃
 * - 支持突发流量（只要桶中有足够的令牌）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class TokenBucketRateLimiter implements RateLimiter {
    
    private final AtomicLong tokens; // 当前令牌数
    private final long capacity;     // 桶容量
    private final long refillRate;   // 令牌填充速率（每秒）
    private final ReentrantLock lock = new ReentrantLock();
    
    private volatile long lastRefillTime; // 上次填充时间
    private final RateLimitConfig.Rule config;
    
    public TokenBucketRateLimiter(RateLimitConfig.Rule config) {
        this.capacity = config.getCapacity();
        this.refillRate = config.getRefillRate();
        this.tokens = new AtomicLong(this.capacity); // 初始化时桶满
        this.lastRefillTime = System.currentTimeMillis();
        this.config = config;
    }
    
    @Override
    public boolean tryAcquire(int permits) {
        return tryAcquire(permits, 0);
    }
    
    @Override
    public boolean tryAcquire(int permits, long timeout) {
        if (permits <= 0) {
            return true;
        }
        
        lock.lock();
        try {
            // 填充令牌
            refillTokens();
            
            // 检查是否有足够的令牌
            long currentTokens = tokens.get();
            if (currentTokens >= permits) {
                tokens.addAndGet(-permits);
                return true;
            }
            
            // 如果指定了超时时间，等待令牌
            if (timeout > 0) {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    refillTokens();
                    currentTokens = tokens.get();
                    if (currentTokens >= permits) {
                        tokens.addAndGet(-permits);
                        return true;
                    }
                    try {
                        Thread.sleep(10); // 短暂休眠
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public long availablePermits() {
        refillTokens();
        return tokens.get();
    }
    
    @Override
    public RateLimitConfig.Rule getConfig() {
        return config;
    }
    
    /**
     * 填充令牌
     */
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastRefillTime;
        
        if (elapsedTime > 0) {
            // 计算应该补充的令牌数
            long tokensToAdd = (elapsedTime * refillRate) / 1000;
            
            if (tokensToAdd > 0) {
                long currentTokens = tokens.get();
                long newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                
                if (tokens.compareAndSet(currentTokens, newTokens)) {
                    lastRefillTime = now;
                }
            }
        }
    }
    
    /**
     * 获取当前令牌数
     * 
     * @return 当前令牌数
     */
    public long getCurrentTokens() {
        refillTokens();
        return tokens.get();
    }
}