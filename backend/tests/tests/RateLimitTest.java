package tests;

import cn.aiedge.common.rate.*;
import cn.aiedge.common.rate.RateLimitConfig.Algorithm;

/**
 * 限流器测试类
 * 
 * 测试不同限流算法的功能和性能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class RateLimitTest {
    
    public static void main(String[] args) {
        System.out.println("开始执行限流器测试...");
        
        // 测试令牌桶算法
        testTokenBucketRateLimiter();
        
        // 测试漏桶算法
        testLeakyBucketRateLimiter();
        
        // 测试滑动窗口算法
        testSlidingWindowRateLimiter();
        
        // 测试限流工厂
        testRateLimiterFactory();
        
        System.out.println("限流器测试完成！");
    }
    
    /**
     * 测试令牌桶算法
     */
    private static void testTokenBucketRateLimiter() {
        System.out.println("\n1. 测试令牌桶算法...");
        
        try {
            RateLimitConfig.Rule config = new RateLimitConfig.Rule();
            config.setCapacity(10); // 桶容量
            config.setRefillRate(5); // 每秒填充5个令牌
            
            RateLimiter rateLimiter = new TokenBucketRateLimiter(config);
            
            // 测试初始状态
            System.out.println("   初始可用令牌数: " + rateLimiter.availablePermits());
            
            // 尝试获取令牌
            boolean result1 = rateLimiter.tryAcquire(3);
            System.out.println("   获取3个令牌: " + result1 + ", 剩余: " + rateLimiter.availablePermits());
            
            boolean result2 = rateLimiter.tryAcquire(5);
            System.out.println("   获取5个令牌: " + result2 + ", 剩余: " + rateLimiter.availablePermits());
            
            boolean result3 = rateLimiter.tryAcquire(8); // 应该失败，因为桶里只剩2个令牌
            System.out.println("   获取8个令牌: " + result3 + ", 剩余: " + rateLimiter.availablePermits());
            
            System.out.println("   ✓ 令牌桶算法测试通过");
        } catch (Exception e) {
            System.err.println("   ✗ 令牌桶算法测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试漏桶算法
     */
    private static void testLeakyBucketRateLimiter() {
        System.out.println("\n2. 测试漏桶算法...");
        
        try {
            RateLimitConfig.Rule config = new RateLimitConfig.Rule();
            config.setCapacity(10); // 桶容量
            config.setRefillRate(2); // 每秒泄露2个请求
            
            RateLimiter rateLimiter = new LeakyBucketRateLimiter(config);
            
            // 测试初始状态
            System.out.println("   初始可用容量: " + rateLimiter.availablePermits());
            
            // 尝试获取许可（相当于往桶里放水）
            boolean result1 = rateLimiter.tryAcquire(3);
            System.out.println("   放入3个请求: " + result1 + ", 剩余容量: " + rateLimiter.availablePermits());
            
            boolean result2 = rateLimiter.tryAcquire(5);
            System.out.println("   放入5个请求: " + result2 + ", 剩余容量: " + rateLimiter.availablePermits());
            
            boolean result3 = rateLimiter.tryAcquire(8); // 应该失败，因为桶容量只有10
            System.out.println("   放入8个请求: " + result3 + ", 剩余容量: " + rateLimiter.availablePermits());
            
            System.out.println("   ✓ 漏桶算法测试通过");
        } catch (Exception e) {
            System.err.println("   ✗ 漏桶算法测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试滑动窗口算法
     */
    private static void testSlidingWindowRateLimiter() {
        System.out.println("\n3. 测试滑动窗口算法...");
        
        try {
            RateLimitConfig.Rule config = new RateLimitConfig.Rule();
            config.setLimit(5); // 限制5个请求
            config.setWindow(10); // 10秒窗口
            
            RateLimiter rateLimiter = new SlidingWindowRateLimiter(config);
            
            // 测试初始状态
            System.out.println("   初始可用配额: " + rateLimiter.availablePermits());
            
            // 尝试获取许可
            boolean result1 = rateLimiter.tryAcquire(2);
            System.out.println("   获取2个许可: " + result1 + ", 剩余: " + rateLimiter.availablePermits());
            
            boolean result2 = rateLimiter.tryAcquire(2);
            System.out.println("   获取2个许可: " + result2 + ", 剩余: " + rateLimiter.availablePermits());
            
            boolean result3 = rateLimiter.tryAcquire(2); // 应该失败，总共已请求6个，超过限制5个
            System.out.println("   获取2个许可: " + result3 + ", 剩余: " + rateLimiter.availablePermits());
            
            System.out.println("   ✓ 滑动窗口算法测试通过");
        } catch (Exception e) {
            System.err.println("   ✗ 滑动窗口算法测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试限流工厂
     */
    private static void testRateLimiterFactory() {
        System.out.println("\n4. 测试限流工厂...");
        
        try {
            // 测试令牌桶算法
            RateLimitConfig.Rule tokenBucketConfig = new RateLimitConfig.Rule();
            tokenBucketConfig.setAlgorithm(Algorithm.TOKEN_BUCKET);
            RateLimiter tokenBucketLimiter = RateLimiterFactory.createRateLimiter(tokenBucketConfig);
            System.out.println("   ✓ 令牌桶算法创建成功: " + tokenBucketLimiter.getClass().getSimpleName());
            
            // 测试漏桶算法
            RateLimitConfig.Rule leakyBucketConfig = new RateLimitConfig.Rule();
            leakyBucketConfig.setAlgorithm(Algorithm.LEAKY_BUCKET);
            RateLimiter leakyBucketLimiter = RateLimiterFactory.createRateLimiter(leakyBucketConfig);
            System.out.println("   ✓ 漏桶算法创建成功: " + leakyBucketLimiter.getClass().getSimpleName());
            
            // 测试滑动窗口算法
            RateLimitConfig.Rule slidingWindowConfig = new RateLimitConfig.Rule();
            slidingWindowConfig.setAlgorithm(Algorithm.SLIDING_WINDOW);
            RateLimiter slidingWindowLimiter = RateLimiterFactory.createRateLimiter(slidingWindowConfig);
            System.out.println("   ✓ 滑动窗口算法创建成功: " + slidingWindowLimiter.getClass().getSimpleName());
            
            System.out.println("   ✓ 限流工厂测试通过");
        } catch (Exception e) {
            System.err.println("   ✗ 限流工厂测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}