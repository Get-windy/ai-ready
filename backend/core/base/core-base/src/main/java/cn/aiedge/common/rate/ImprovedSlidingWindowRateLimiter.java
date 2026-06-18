package cn.aiedge.common.rate;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高精度滑动窗口限流器实现
 * 
 * 滑动窗口算法特点：
 * - 将时间窗口细分为更小的时间片，提高精度
 * - 准确统计时间窗口内的请求数
 * - 实现高精度的时间片切分机制
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class ImprovedSlidingWindowRateLimiter implements RateLimiter {
    
    private final long limit;      // 限制请求数
    private final long windowMs;   // 时间窗口大小（毫秒）
    private final Map<Long, Long> requestCounts; // 存储时间片和请求次数的映射
    private final long timeSliceMs; // 时间片大小（毫秒），默认为1秒
    private final RateLimitConfig.Rule config;
    
    public ImprovedSlidingWindowRateLimiter(RateLimitConfig.Rule config) {
        this.config = config;
        this.limit = config.getLimit();
        this.windowMs = config.getWindow() * 1000; // 转换为毫秒
        this.timeSliceMs = 1000; // 1秒时间片，可根据需要调整
        this.requestCounts = new ConcurrentHashMap<>();
    }
    
    /**
     * 构造函数，允许自定义时间片大小
     */
    public ImprovedSlidingWindowRateLimiter(RateLimitConfig.Rule config, long timeSliceMs) {
        this.config = config;
        this.limit = config.getLimit();
        this.windowMs = config.getWindow() * 1000; // 转换为毫秒
        this.timeSliceMs = timeSliceMs;
        this.requestCounts = new ConcurrentHashMap<>();
    }
    
    @Override
    public boolean tryAcquire(int permits) {
        long currentTime = System.currentTimeMillis();
        
        // 清理过期的时间片
        cleanupExpiredSlices(currentTime);
        
        // 计算当前时间窗口内的总请求数
        long currentWindowCount = getCurrentWindowRequestCount(currentTime);
        
        // 检查是否超过限制
        if (currentWindowCount + permits <= limit) {
            // 记录新的请求
            recordRequest(currentTime, permits);
            return true;
        }
        
        log.debug("Rate limit exceeded: current={}, limit={}, requested={}", 
                 currentWindowCount, limit, permits);
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
        cleanupExpiredSlices(currentTime);
        long currentWindowCount = getCurrentWindowRequestCount(currentTime);
        return Math.max(0, limit - currentWindowCount);
    }
    
    @Override
    public RateLimitConfig.Rule getConfig() {
        return config;
    }
    
    /**
     * 清理过期的时间片
     */
    private void cleanupExpiredSlices(long currentTime) {
        long threshold = currentTime - windowMs;
        
        // 移除过期的时间片
        requestCounts.entrySet().removeIf(entry -> entry.getKey() < threshold);
    }
    
    /**
     * 获取当前时间窗口内的请求数
     */
    private long getCurrentWindowRequestCount(long currentTime) {
        long sum = 0;
        long threshold = currentTime - windowMs;
        
        for (Map.Entry<Long, Long> entry : requestCounts.entrySet()) {
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
        // 将时间对齐到时间片边界
        long sliceKey = (currentTime / timeSliceMs) * timeSliceMs;
        
        requestCounts.compute(sliceKey, (key, oldValue) -> 
            oldValue == null ? (long) permits : oldValue + permits);
    }
    
    /**
     * 获取当前时间窗口内的请求数（公开方法，用于测试）
     */
    public long getCurrentWindowRequestCount() {
        long currentTime = System.currentTimeMillis();
        cleanupExpiredSlices(currentTime);
        return getCurrentWindowRequestCount(currentTime);
    }
    
    /**
     * 获取时间窗口大小（毫秒）
     */
    public long getWindowSizeMs() {
        return windowMs;
    }
    
    /**
     * 获取时间片大小（毫秒）
     */
    public long getTimeSliceMs() {
        return timeSliceMs;
    }
}