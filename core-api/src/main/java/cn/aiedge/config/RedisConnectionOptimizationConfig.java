package cn.aiedge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Redis连接池优化配置
 * 
 * 优化策略：
 * 1. 连接池预热
 * 2. 序列化优化
 * 3. 常用操作封装
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisConnectionOptimizationConfig {

    /**
     * Redis操作工具类
     * 
     * 提供高性能的Redis操作方法
     */
    public static class RedisOperations {

        private final RedisTemplate<String, Object> redisTemplate;

        public RedisOperations(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
            // 设置序列化器
            this.redisTemplate.setKeySerializer(new StringRedisSerializer());
            this.redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        }

        /**
         * 批量获取（减少网络往返）
         */
        public java.util.Map<String, Object> multiGet(java.util.List<String> keys) {
            return redisTemplate.opsForValue().multiGet(keys);
        }

        /**
         * 批量设置（减少网络往返）
         */
        public void multiSet(java.util.Map<String, Object> map, long ttlSeconds) {
            redisTemplate.opsForValue().multiSet(map);
            // 设置过期时间
            for (String key : map.keySet()) {
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            }
        }

        /**
         * 管道操作（批量执行命令）
         */
        public <T> T executePipeline(org.springframework.data.redis.core.RedisCallback<T> action) {
            return redisTemplate.executePipelined(action).get(0);
        }

        /**
         * 分布式锁获取
         */
        public boolean tryLock(String lockKey, String value, long expireSeconds) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                lockKey, value, expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        }

        /**
         * 分布式锁释放
         */
        public boolean releaseLock(String lockKey, String value) {
            Object currentValue = redisTemplate.opsForValue().get(lockKey);
            if (value.equals(currentValue)) {
                redisTemplate.delete(lockKey);
                return true;
            }
            return false; // 锁已过期或被其他线程持有
        }

        /**
         * 缓存预热
         */
        public void warmUp(java.util.Map<String, Object> hotData, long ttlSeconds) {
            log.info("[Redis] Warming up {} hot entries", hotData.size());
            multiSet(hotData, ttlSeconds);
        }
    }
}