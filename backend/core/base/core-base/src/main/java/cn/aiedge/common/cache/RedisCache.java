package cn.aiedge.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 提供统一的缓存操作接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class RedisCache {

    private final StringRedisTemplate redisTemplate;

    public RedisCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== String操作 ====================

    /**
     * 设置缓存
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置缓存（秒）
     */
    public void setEx(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存（如果不存在）
     */
    public boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    /**
     * 获取缓存
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取并删除
     */
    public String getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    // ==================== Hash操作 ====================

    /**
     * Hash设置
     */
    public void hSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash获取
     */
    public String hGet(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    /**
     * Hash删除字段
     */
    public long hDelete(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * Hash是否存在字段
     */
    public boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    // ==================== List操作 ====================

    /**
     * List左推入
     */
    public long lPush(String key, String value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * List右推入
     */
    public long rPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * List左弹出
     */
    public String lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * List右弹出
     */
    public String rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    // ==================== Set操作 ====================

    /**
     * Set添加
     */
    public long sAdd(String key, String... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * Set移除
     */
    public long sRemove(String key, String... values) {
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }

    /**
     * Set是否包含
     */
    public boolean sIsMember(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    // ==================== 过期时间 ====================

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取过期时间（秒）
     */
    public long getExpire(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -2;
    }

    // ==================== 删除操作 ====================

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除
     */
    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0;
    }

    /**
     * 模式删除
     */
    public long deleteByPattern(String pattern) {
        Collection<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0;
    }

    // ==================== 存在判断 ====================

    /**
     * 判断key是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== 计数器 ====================

    /**
     * 递增
     */
    public long increment(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        return value != null ? value : 0;
    }

    /**
     * 递增指定值
     */
    public long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0;
    }

    /**
     * 递减
     */
    public long decrement(String key) {
        Long value = redisTemplate.opsForValue().decrement(key);
        return value != null ? value : 0;
    }

    /**
     * 递减指定值
     */
    public long decrement(String key, long delta) {
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value != null ? value : 0;
    }
}
