package cn.aiedge.common.ratelimit;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 滑动窗口限流器
 */
public class SlidingWindowLimiter {
    
    private final int maxRequests;                     // 最大请求数
    private final long windowSizeInMillis;             // 窗口大小（毫秒）
    private final ConcurrentLinkedQueue<Long> queue;   // 请求时间队列
    private final AtomicInteger currentCount;          // 当前计数
    
    public SlidingWindowLimiter(int maxRequests, long windowSizeInMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.queue = new ConcurrentLinkedQueue<>();
        this.currentCount = new AtomicInteger(0);
    }
    
    /**
     * 尝试获取许可
     */
    public boolean tryAcquire() {
        long currentTime = System.currentTimeMillis();
        
        // 清理过期请求
        while (!queue.isEmpty()) {
            Long time = queue.peek();
            if (time != null && currentTime - time > windowSizeInMillis) {
                queue.poll();
                currentCount.decrementAndGet();
            } else {
                break;
            }
        }
        
        // 检查是否超过限制
        if (currentCount.get() < maxRequests) {
            queue.offer(currentTime);
            currentCount.incrementAndGet();
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取当前窗口内的请求数
     */
    public int getCurrentCount() {
        return currentCount.get();
    }
}
