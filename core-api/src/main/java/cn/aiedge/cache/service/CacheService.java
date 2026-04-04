package cn.aiedge.cache.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 提供统一的缓存读写接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 基础操作 ====================

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置缓存（秒为单位）
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间（秒）
     */
    public void setEx(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存（如果不存在）
     *
     * @param key   键
     * @param value 值
     * @return 是否设置成功
     */
    public Boolean setIfAbsent(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * 设置缓存（如果不存在，带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存（指定类型）
     *
     * @param key   键
     * @param clazz 值类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 键集合
     * @return 删除数量
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间（秒），-1表示永不过期，-2表示已过期或不存在
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ==================== 自增/自减操作 ====================

    /**
     * 自增
     *
     * @param key 键
     * @return 自增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增指定值
     *
     * @param key   键
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减
     *
     * @param key 键
     * @return 自减后的值
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 自减指定值
     *
     * @param key   键
     * @param delta 减量
     * @return 自减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ==================== Hash操作 ====================

    /**
     * 设置Hash字段
     *
     * @param key     键
     * @param field   字段
     * @param value   值
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 批量设置Hash字段
     *
     * @param key 键
     * @param map 字段值映射
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取Hash字段值
     *
     * @param key   键
     * @param field 字段
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取所有Hash字段值
     *
     * @param key 键
     * @return 字段值映射
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除Hash字段
     *
     * @param key    键
     * @param fields 字段
     * @return 删除数量
     */
    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 判断Hash字段是否存在
     *
     * @param key   键
     * @param field 字段
     * @return 是否存在
     */
    public Boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    // ==================== List操作 ====================

    /**
     * 从列表左侧推入
     *
     * @param key   键
     * @param value 值
     * @return 列表长度
     */
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从列表右侧推入
     *
     * @param key   键
     * @param value 值
     * @return 列表长度
     */
    public Long rPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 从列表左侧弹出
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T lPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从列表右侧弹出
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表范围内的元素
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key 键
     * @return 长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ==================== Set操作 ====================

    /**
     * 添加Set成员
     *
     * @param key    键
     * @param values 值
     * @return 添加数量
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 移除Set成员
     *
     * @param key    键
     * @param values 值
     * @return 移除数量
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 获取Set所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断是否是Set成员
     *
     * @param key   键
     * @param value 值
     * @return 是否是成员
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取Set大小
     *
     * @param key 键
     * @return 大小
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // ==================== ZSet操作 ====================

    /**
     * 添加ZSet成员
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return 是否添加成功
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 移除ZSet成员
     *
     * @param key    键
     * @param values 值
     * @return 移除数量
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * 获取ZSet范围内的成员（按分数升序）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 成员集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取ZSet范围内的成员（按分数降序）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 成员集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取ZSet大小
     *
     * @param key 键
     * @return 大小
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    // ==================== 模式匹配删除 ====================

    /**
     * 根据模式删除缓存
     *
     * @param pattern 模式
     * @return 删除数量
     */
    public Long deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            return redisTemplate.delete(keys);
        }
        return 0L;
    }

    /**
     * 根据模式获取所有键
     *
     * @param pattern 模式
     * @return 键集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}
