package cn.aiedge.common.cache;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 缓存工具类
 * 提供通用的缓存操作方法
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class CacheHelper {

    private final StringRedisTemplate redisTemplate;
    
    /** 空值标记 */
    private static final String NULL_VALUE = "NULL_PLACEHOLDER";
    
    /** 默认缓存时间（秒） */
    private static final long DEFAULT_TTL = 300;
    
    /** 空值缓存时间（秒） */
    private static final long NULL_TTL = 60;

    public CacheHelper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询缓存（带穿透保护）
     * 
     * @param key 缓存key
     * @param type 返回类型
     * @param dbLoader 数据库加载器
     * @param ttl 缓存时间（秒）
     */
    public <T> T get(String key, Class<T> type, Supplier<T> dbLoader, long ttl) {
        // 1. 查询缓存
        String json = redisTemplate.opsForValue().get(key);
        
        // 2. 缓存命中
        if (StrUtil.isNotBlank(json)) {
            // 空值标记
            if (NULL_VALUE.equals(json)) {
                log.debug("缓存空值命中: key={}", key);
                return null;
            }
            log.debug("缓存命中: key={}", key);
            return JSONUtil.toBean(json, type);
        }
        
        // 3. 缓存未命中，查询数据库
        log.debug("缓存未命中: key={}", key);
        T data = dbLoader.get();
        
        // 4. 写入缓存
        if (data == null) {
            // 防止缓存穿透：缓存空值
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_TTL, TimeUnit.SECONDS);
            log.debug("缓存空值: key={}", key);
        } else {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(data), ttl, TimeUnit.SECONDS);
            log.debug("写入缓存: key={}, ttl={}s", key, ttl);
        }
        
        return data;
    }

    /**
     * 查询缓存（使用默认TTL）
     */
    public <T> T get(String key, Class<T> type, Supplier<T> dbLoader) {
        return get(key, type, dbLoader, DEFAULT_TTL);
    }

    /**
     * 直接设置缓存
     */
    public <T> void set(String key, T data, long ttl) {
        if (data == null) {
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_TTL, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(data), ttl, TimeUnit.SECONDS);
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("删除缓存: key={}", key);
    }

    /**
     * 批量删除缓存
     */
    public void delete(String... keys) {
        for (String key : keys) {
            redisTemplate.delete(key);
        }
        log.debug("批量删除缓存: count={}", keys.length);
    }

    /**
     * 模式匹配删除
     */
    public void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("模式删除缓存: pattern={}, count={}", pattern, keys.size());
        }
    }

    /**
     * 获取分布式锁
     */
    public boolean tryLock(String lockKey, String value, long expireSeconds) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(lockKey, value, expireSeconds, TimeUnit.SECONDS)
        );
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String lockKey, String value) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (value.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 使用分布式锁执行任务
     */
    public <T> T executeWithLock(String lockKey, long lockExpireSeconds, Supplier<T> task) {
        String lockValue = String.valueOf(System.currentTimeMillis());
        
        if (!tryLock(lockKey, lockValue, lockExpireSeconds)) {
            log.warn("获取锁失败: key={}", lockKey);
            return null;
        }
        
        try {
            return task.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }
}
