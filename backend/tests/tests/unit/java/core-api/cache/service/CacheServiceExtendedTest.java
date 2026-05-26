package cn.aiedge.cache.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存服务扩展单元测试 - 覆盖率提升
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("缓存服务扩展测试")
class CacheServiceExtendedTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    // ==================== setEx ====================

    @Test
    @DisplayName("设置缓存（秒级过期） - 成功")
    void testSetEx_Success() {
        // Given
        String key = "test:key";
        String value = "test-value";
        long seconds = 3600L;

        // When
        cacheService.setEx(key, value, seconds);

        // Then
        verify(valueOperations).set(key, value, seconds, TimeUnit.SECONDS);
    }

    // ==================== setIfAbsent ====================

    @Test
    @DisplayName("设置缓存（如果不存在） - 成功")
    void testSetIfAbsent_Success() {
        // Given
        String key = "test:key";
        String value = "test-value";
        when(valueOperations.setIfAbsent(key, value)).thenReturn(true);

        // When
        Boolean result = cacheService.setIfAbsent(key, value);

        // Then
        assertTrue(result);
        verify(valueOperations).setIfAbsent(key, value);
    }

    @Test
    @DisplayName("设置缓存（如果不存在，带过期） - 成功")
    void testSetIfAbsentWithTtl_Success() {
        // Given
        String key = "test:key";
        String value = "test-value";
        long timeout = 60L;
        when(valueOperations.setIfAbsent(key, value, timeout, TimeUnit.SECONDS)).thenReturn(true);

        // When
        Boolean result = cacheService.setIfAbsent(key, value, timeout, TimeUnit.SECONDS);

        // Then
        assertTrue(result);
        verify(valueOperations).setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("设置缓存（如果不存在） - 已存在")
    void testSetIfAbsent_AlreadyExists() {
        // Given
        String key = "test:key";
        String value = "test-value";
        when(valueOperations.setIfAbsent(key, value)).thenReturn(false);

        // When
        Boolean result = cacheService.setIfAbsent(key, value);

        // Then
        assertFalse(result);
    }

    // ==================== getExpire ====================

    @Test
    @DisplayName("获取过期时间 - 成功")
    void testGetExpire_Success() {
        // Given
        String key = "test:key";
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(3600L);

        // When
        Long result = cacheService.getExpire(key);

        // Then
        assertEquals(3600L, result);
    }

    @Test
    @DisplayName("获取过期时间 - 永不过期")
    void testGetExpire_NeverExpire() {
        // Given
        String key = "test:key";
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-1L);

        // When
        Long result = cacheService.getExpire(key);

        // Then
        assertEquals(-1L, result);
    }

    @Test
    @DisplayName("获取过期时间 - 已过期或不存在")
    void testGetExpire_ExpiredOrNotExist() {
        // Given
        String key = "test:key";
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

        // When
        Long result = cacheService.getExpire(key);

        // Then
        assertEquals(-2L, result);
    }

    // ==================== get with Class ====================

    @Test
    @DisplayName("获取缓存（带类型） - 成功")
    void testGetWithClass_Success() {
        // Given
        String key = "test:key";
        String expectedValue = "test-value";
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // When
        String result = cacheService.get(key, String.class);

        // Then
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("获取缓存（带类型） - 类型不匹配")
    void testGetWithClass_TypeMismatch() {
        // Given
        String key = "test:key";
        Integer value = 123;
        when(valueOperations.get(key)).thenReturn(value);

        // When
        String result = cacheService.get(key, String.class);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("获取缓存（带类型） - 不存在")
    void testGetWithClass_NotFound() {
        // Given
        String key = "test:key";
        when(valueOperations.get(key)).thenReturn(null);

        // When
        String result = cacheService.get(key, String.class);

        // Then
        assertNull(result);
    }

    // ==================== hHasKey ====================

    @Test
    @DisplayName("判断Hash字段是否存在 - 存在")
    void testHHasKey_Exists() {
        // Given
        String key = "hash:key";
        String field = "field1";
        when(hashOperations.hasKey(key, field)).thenReturn(true);

        // When
        Boolean result = cacheService.hHasKey(key, field);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("判断Hash字段是否存在 - 不存在")
    void testHHasKey_NotExists() {
        // Given
        String key = "hash:key";
        String field = "field1";
        when(hashOperations.hasKey(key, field)).thenReturn(false);

        // When
        Boolean result = cacheService.hHasKey(key, field);

        // Then
        assertFalse(result);
    }

    // ==================== lPop / rPop ====================

    @Test
    @DisplayName("从列表左侧弹出 - 成功")
    void testLPop_Success() {
        // Given
        String key = "list:key";
        String expectedValue = "value1";
        when(listOperations.leftPop(key)).thenReturn(expectedValue);

        // When
        String result = cacheService.lPop(key);

        // Then
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("从列表左侧弹出 - 空列表")
    void testLPop_Empty() {
        // Given
        String key = "list:key";
        when(listOperations.leftPop(key)).thenReturn(null);

        // When
        String result = cacheService.lPop(key);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("从列表右侧弹出 - 成功")
    void testRPop_Success() {
        // Given
        String key = "list:key";
        String expectedValue = "value1";
        when(listOperations.rightPop(key)).thenReturn(expectedValue);

        // When
        String result = cacheService.rPop(key);

        // Then
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("从列表右侧弹出 - 空列表")
    void testRPop_Empty() {
        // Given
        String key = "list:key";
        when(listOperations.rightPop(key)).thenReturn(null);

        // When
        String result = cacheService.rPop(key);

        // Then
        assertNull(result);
    }

    // ==================== lSize ====================

    @Test
    @DisplayName("获取列表长度 - 成功")
    void testLSize_Success() {
        // Given
        String key = "list:key";
        when(listOperations.size(key)).thenReturn(5L);

        // When
        Long result = cacheService.lSize(key);

        // Then
        assertEquals(5L, result);
    }

    @Test
    @DisplayName("获取列表长度 - 空列表")
    void testLSize_Empty() {
        // Given
        String key = "list:key";
        when(listOperations.size(key)).thenReturn(0L);

        // When
        Long result = cacheService.lSize(key);

        // Then
        assertEquals(0L, result);
    }

    // ==================== sRemove ====================

    @Test
    @DisplayName("从Set移除成员 - 成功")
    void testSRemove_Success() {
        // Given
        String key = "set:key";
        String value = "value1";
        when(setOperations.remove(key, value)).thenReturn(1L);

        // When
        Long result = cacheService.sRemove(key, value);

        // Then
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("从Set移除成员 - 不存在")
    void testSRemove_NotExists() {
        // Given
        String key = "set:key";
        String value = "value1";
        when(setOperations.remove(key, value)).thenReturn(0L);

        // When
        Long result = cacheService.sRemove(key, value);

        // Then
        assertEquals(0L, result);
    }

    // ==================== sSize ====================

    @Test
    @DisplayName("获取Set大小 - 成功")
    void testSSize_Success() {
        // Given
        String key = "set:key";
        when(setOperations.size(key)).thenReturn(3L);

        // When
        Long result = cacheService.sSize(key);

        // Then
        assertEquals(3L, result);
    }

    // ==================== zRemove ====================

    @Test
    @DisplayName("从ZSet移除成员 - 成功")
    void testZRemove_Success() {
        // Given
        String key = "zset:key";
        String value = "value1";
        when(zSetOperations.remove(key, value)).thenReturn(1L);

        // When
        Long result = cacheService.zRemove(key, value);

        // Then
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("从ZSet移除多个成员 - 成功")
    void testZRemove_Multiple() {
        // Given
        String key = "zset:key";
        String value1 = "value1";
        String value2 = "value2";
        when(zSetOperations.remove(key, value1, value2)).thenReturn(2L);

        // When
        Long result = cacheService.zRemove(key, value1, value2);

        // Then
        assertEquals(2L, result);
    }

    // ==================== zReverseRange ====================

    @Test
    @DisplayName("获取ZSet逆序范围 - 成功")
    void testZReverseRange_Success() {
        // Given
        String key = "zset:key";
        Set<Object> expectedSet = new LinkedHashSet<>(Arrays.asList("value3", "value2", "value1"));
        when(zSetOperations.reverseRange(key, 0, 2)).thenReturn(expectedSet);

        // When
        Set<Object> result = cacheService.zReverseRange(key, 0, 2);

        // Then
        assertEquals(expectedSet, result);
    }

    @Test
    @DisplayName("获取ZSet逆序范围 - 空")
    void testZReverseRange_Empty() {
        // Given
        String key = "zset:key";
        when(zSetOperations.reverseRange(key, 0, -1)).thenReturn(Collections.emptySet());

        // When
        Set<Object> result = cacheService.zReverseRange(key, 0, -1);

        // Then
        assertTrue(result.isEmpty());
    }

    // ==================== zSize ====================

    @Test
    @DisplayName("获取ZSet大小 - 成功")
    void testZSize_Success() {
        // Given
        String key = "zset:key";
        when(zSetOperations.size(key)).thenReturn(5L);

        // When
        Long result = cacheService.zSize(key);

        // Then
        assertEquals(5L, result);
    }

    // ==================== deleteByPattern ====================

    @Test
    @DisplayName("根据模式删除缓存 - 成功")
    void testDeleteByPattern_Success() {
        // Given
        String pattern = "test:*";
        Set<String> keys = new HashSet<>(Arrays.asList("test:key1", "test:key2", "test:key3"));
        when(redisTemplate.keys(pattern)).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(3L);

        // When
        Long result = cacheService.deleteByPattern(pattern);

        // Then
        assertEquals(3L, result);
    }

    @Test
    @DisplayName("根据模式删除缓存 - 无匹配")
    void testDeleteByPattern_NoMatch() {
        // Given
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(Collections.emptySet());

        // When
        Long result = cacheService.deleteByPattern(pattern);

        // Then
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("根据模式删除缓存 - null结果")
    void testDeleteByPattern_NullResult() {
        // Given
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(null);

        // When
        Long result = cacheService.deleteByPattern(pattern);

        // Then
        assertEquals(0L, result);
    }

    // ==================== keys ====================

    @Test
    @DisplayName("根据模式获取所有键 - 成功")
    void testKeys_Success() {
        // Given
        String pattern = "test:*";
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("test:key1", "test:key2"));
        when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

        // When
        Set<String> result = cacheService.keys(pattern);

        // Then
        assertEquals(expectedKeys, result);
    }

    @Test
    @DisplayName("根据模式获取所有键 - 无匹配")
    void testKeys_NoMatch() {
        // Given
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(Collections.emptySet());

        // When
        Set<String> result = cacheService.keys(pattern);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("根据模式获取所有键 - null结果")
    void testKeys_NullResult() {
        // Given
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(null);

        // When
        Set<String> result = cacheService.keys(pattern);

        // Then
        assertNull(result);
    }
}
