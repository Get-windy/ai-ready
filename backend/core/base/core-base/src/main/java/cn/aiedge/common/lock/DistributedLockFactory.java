package cn.aiedge.common.lock;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 分布式锁工厂类
 * 
 * 提供创建各种分布式锁的便捷方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class DistributedLockFactory {
    
    /**
     * 创建Redis分布式锁
     * 
     * @param redisTemplate Redis模板
     * @param lockKey 锁的键
     * @return Redis分布式锁实例
     */
    public static DistributedLock createRedisLock(RedisTemplate<String, Object> redisTemplate, String lockKey) {
        return new RedisDistributedLock(redisTemplate, lockKey);
    }
    
    /**
     * 创建Redis分布式锁
     * 
     * @param redisTemplate Redis模板
     * @param lockKey 锁的键
     * @param leaseTime 锁的租约时间（毫秒）
     * @return Redis分布式锁实例
     */
    public static DistributedLock createRedisLock(RedisTemplate<String, Object> redisTemplate, String lockKey, long leaseTime) {
        return new RedisDistributedLock(redisTemplate, lockKey, leaseTime);
    }
    
    /**
     * 创建RedLock分布式锁
     * 
     * @param redisTemplates Redis模板列表
     * @param lockKey 锁的键
     * @param leaseTime 锁的租约时间（毫秒）
     * @return RedLock分布式锁实例
     */
    public static DistributedLock createRedLock(List<RedisTemplate<String, Object>> redisTemplates, String lockKey, long leaseTime) {
        return new RedLock(redisTemplates, lockKey, leaseTime);
    }
    
    /**
     * 创建默认租约时间的Redis分布式锁
     * 
     * @param redisTemplate Redis模板
     * @param lockKey 锁的键
     * @return Redis分布式锁实例
     */
    public static DistributedLock createDefaultRedisLock(RedisTemplate<String, Object> redisTemplate, String lockKey) {
        return new RedisDistributedLock(redisTemplate, lockKey, RedisDistributedLock.DEFAULT_LEASE_TIME);
    }
}