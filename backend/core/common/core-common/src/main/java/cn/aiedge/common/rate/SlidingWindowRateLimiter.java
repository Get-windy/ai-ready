package cn.aiedge.common.rate;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑动窗口限流器实现
 * 
 * 滑动窗口算法特点：
 * - 将时间划分为若干个小窗口
 * - 记录每个小窗口内的请求次数
 * - 计算当前时间窗口内的总请求数
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class SlidingWindowRateLimiter implements RateLimiter {
    
    private final long limit;      // 限制请求数
    private final long window;     // 时间窗口大小（毫秒）
    private final Map<Long, Long> windowMap; // 存储时间窗口和请求次数的映射
    private final RateLimitConfig.Rule config;
    
    public SlidingWindowRateLimiter(RateLimitConfig.Rule config) {
        this.limit = config.getLimit();
        this.window = config.getWindow() * 1000; // 转换为毫秒
        this.windowMap = new ConcurrentHashMap<>();
        this.config = config;
    }
    
    @Override
    public boolean tryAcquire(int permits) {
        long currentTime = System.currentTimeMillis();
        cleanupExpiredWindows(currentTime);
        
        // 计算当前窗口内的总请求数
        long currentRequests = getCurrentRequestCount(currentTime);
        
        // 检查是否超过限制
        if (currentRequests + permits <= limit) {
            // 记录新的请求
            recordRequest(currentTime, permits);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean tryAcquire(int permits, long timeout) {
        // 滑动窗口算法不支持等待，直接返回结果
        return tryAcquire(permits);
    }
    
    @Override
    public long availablePermits() {
        long currentTime = System.currentTimeMillis();
        cleanupExpiredWindows(currentTime);
        long currentRequests = getCurrentRequestCount(currentTime);
        return Math.max(0, limit - currentRequests);
    }
    
    @Override
    public RateLimitConfig.Rule getConfig() {
        return config;
    }
    
    /**
     * 清理过期的时间窗口
     */
    private void cleanupExpiredWindows(long currentTime) {
        long threshold = currentTime - window;
        
        // 移除过期的时间窗口
        windowMap.entrySet().removeIf(entry -> entry.getKey() < threshold);
    }
    
    /**
     * 获取当前窗口内的请求数
     */
    private long getCurrentRequestCount(long currentTime) {
        long sum = 0;
        long threshold = currentTime - window;
        
        for (Map.Entry<Long, Long> entry : windowMap.entrySet()) {
            if (entry.getKey() >= threshold) {
                sum += entry.getValue();
            }
        }
        
        return sum;
    }
    
    /**
     * 记录请求
     */
    private void recordRequest(long currentTime, int permits) {
        // 将时间对齐到窗口边界
        long windowKey = (currentTime / 1000) * 1000; // 按秒对齐
        
        windowMap.compute(windowKey, (key, oldValue) -> 
            oldValue == null ? (long) permits : oldValue + permits);
    }
    
    /**
     * 获取当前时间窗口内的请求数
     */
    public long getCurrentRequestCount() {
        long currentTime = System.currentTimeMillis();
        cleanupExpiredWindows(currentTime);
        return getCurrentRequestCount(currentTime);
    }
}