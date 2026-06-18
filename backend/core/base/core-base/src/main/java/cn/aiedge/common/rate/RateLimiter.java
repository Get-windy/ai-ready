package cn.aiedge.common.rate;

/**
 * 限流器接口
 * 
 * 定义限流器的基本操作
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface RateLimiter {
    
    /**
     * 尝试获取许可
     * 
     * @param permits 需要获取的许可数
     * @return 是否获取成功
     */
    boolean tryAcquire(int permits);
    
    /**
     * 尝试获取许可，带超时
     * 
     * @param permits 需要获取的许可数
     * @param timeout 超时时间（毫秒）
     * @return 是否获取成功
     */
    boolean tryAcquire(int permits, long timeout);
    
    /**
     * 获取剩余许可数
     * 
     * @return 剩余许可数
     */
    long availablePermits();
    
    /**
     * 获取限流器配置
     * 
     * @return 限流器配置
     */
    RateLimitConfig.Rule getConfig();
}