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
 * CacheService 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

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

    // ==================== 基础操作测试 ====================

    @Test
    @DisplayName("设置缓存 - 成功")
    void testSet_Success() {
        // Given
        String key = "test:key";
        String value = "test-value";

        // When
        cacheService.set(key, value);

        // Then
        verify(valueOperations).set(key, value);
    }

    @Test
    @DisplayName("设置缓存（带过期时间） - 成功")
    void testSetWithTtl_Success() {
        // Given
        String key = "test:key";
        String value = "test-value";
        long timeout = 60L;
        TimeUnit unit = TimeUnit.SECONDS;

        // When
        cacheService.set(key, value, timeout, unit);

        // Then
        verify(valueOperations).set(key, value, timeout, unit);
    }

    @Test
    @DisplayName("获取缓存 - 成功")
    void testGet_Success() {
        // Given
        String key = "test:key";
        String expectedValue = "test-value";
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // When
        String actualValue = cacheService.get(key);

        // Then
        assertEquals(expectedValue, actualValue);
        verify(valueOperations).get(key);
    }

    @Test
    @DisplayName("获取缓存 - 不存在")
    void testGet_NotFound() {
        // Given
        String key = "test:key";
        when(valueOperations.get(key)).thenReturn(null);

        // When
        String actualValue = cacheService.get(key);

        // Then
        assertNull(actualValue);
    }

    @Test
    @DisplayName("删除缓存 - 成功")
    void testDelete_Success() {
        // Given
        String key = "test:key";
        when(redisTemplate.delete(key)).thenReturn(true);

        // When
        Boolean result = cacheService.delete(key);

        // Then
        assertTrue(result);
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("批量删除缓存 - 成功")
    void testDeleteBatch_Success() {
        // Given
        Collection<String> keys = Arrays.asList("key1", "key2", "key3");
        when(redisTemplate.delete(keys)).thenReturn(3L);

        // When
        Long result = cacheService.delete(keys);

        // Then
        assertEquals(3L, result);
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("判断缓存是否存在 - 存在")
    void testHasKey_Exists() {
        // Given
        String key = "test:key";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        Boolean result = cacheService.hasKey(key);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("设置过期时间 - 成功")
    void testExpire_Success() {
        // Given
        String key = "test:key";
        long timeout = 60L;
        when(redisTemplate.expire(key, timeout, TimeUnit.SECONDS)).thenReturn(true);

        // When
        Boolean result = cacheService.expire(key, timeout, TimeUnit.SECONDS);

        // Then
        assertTrue(result);
    }

    // ==================== 自增/自减操作测试 ====================

    @Test
    @DisplayName("自增 - 成功")
    void testIncrement_Success() {
        // Given
        String key = "counter";
        when(valueOperations.increment(key)).thenReturn(1L);

        // When
        Long result = cacheService.increment(key);

        // Then
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("自增指定值 - 成功")
    void testIncrementByDelta_Success() {
        // Given
        String key = "counter";
        long delta = 10L;
        when(valueOperations.increment(key, delta)).thenReturn(10L);

        // When
        Long result = cacheService.increment(key, delta);

        // Then
        assertEquals(10L, result);
    }

    @Test
    @DisplayName("自减 - 成功")
    void testDecrement_Success() {
        // Given
        String key = "counter";
        when(valueOperations.decrement(key)).thenReturn(-1L);

        // When
        Long result = cacheService.decrement(key);

        // Then
        assertEquals(-1L, result);
    }

    // ==================== Hash操作测试 ====================

    @Test
    @DisplayName("设置Hash字段 - 成功")
    void testHSet_Success() {
        // Given
        String key = "hash:key";
        String field = "field1";
        String value = "value1";

        // When
        cacheService.hSet(key, field, value);

        // Then
        verify(hashOperations).put(key, field, value);
    }

    @Test
    @DisplayName("获取Hash字段 - 成功")
    void testHGet_Success() {
        // Given
        String key = "hash:key";
        String field = "field1";
        String expectedValue = "value1";
        when(hashOperations.get(key, field)).thenReturn(expectedValue);

        // When
        String actualValue = cacheService.hGet(key, field);

        // Then
        assertEquals(expectedValue, actualValue);
    }

    @Test
    @DisplayName("获取所有Hash字段 - 成功")
    void testHGetAll_Success() {
        // Given
        String key = "hash:key";
        Map<Object, Object> expectedMap = new HashMap<>();
        expectedMap.put("field1", "value1");
        expectedMap.put("field2", "value2");
        when(hashOperations.entries(key)).thenReturn(expectedMap);

        // When
        Map<Object, Object> actualMap = cacheService.hGetAll(key);

        // Then
        assertEquals(expectedMap, actualMap);
    }

    @Test
    @DisplayName("删除Hash字段 - 成功")
    void testHDelete_Success() {
        // Given
        String key = "hash:key";
        String field = "field1";
        when(hashOperations.delete(key, field)).thenReturn(1L);

        // When
        Long result = cacheService.hDelete(key, field);

        // Then
        assertEquals(1L, result);
    }

    // ==================== List操作测试 ====================

    @Test
    @DisplayName("从列表左侧推入 - 成功")
    void testLPush_Success() {
        // Given
        String key = "list:key";
        String value = "value1";
        when(listOperations.leftPush(key, value)).thenReturn(1L);

        // When
        Long result = cacheService.lPush(key, value);

        // Then
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("从列表右侧推入 - 成功")
    void testRPush_Success() {
        // Given
        String key = "list:key";
        String value = "value1";
        when(listOperations.rightPush(key, value)).thenReturn(1L);

        // When
        Long result = cacheService.rPush(key, value);

        // Then
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("获取列表范围 - 成功")
    void testLRange_Success() {
        // Given
        String key = "list:key";
        List<Object> expectedList = Arrays.asList("value1", "value2", "value3");
        when(listOperations.range(key, 0, -1)).thenReturn(expectedList);

        // When
        List<Object> actualList = cacheService.lRange(key, 0, -1);

        // Then
        assertEquals(expectedList, actualList);
    }

    // ==================== Set操作测试 ====================

    @Test
    @DisplayName("添加Set成员 - 成功")
    void testSAdd_Success() {
        // Given
        String key = "set:key";
        String value = "value1";
        when(setOperations.add(key, value)).thenReturn(1L);

        // When
        Long result = cacheService.sAdd(key, value);

        // Then
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("获取Set所有成员 - 成功")
    void testSMembers_Success() {
        // Given
        String key = "set:key";
        Set<Object> expectedSet = new HashSet<>(Arrays.asList("value1", "value2"));
        when(setOperations.members(key)).thenReturn(expectedSet);

        // When
        Set<Object> actualSet = cacheService.sMembers(key);

        // Then
        assertEquals(expectedSet, actualSet);
    }

    @Test
    @DisplayName("判断是否是Set成员 - 是")
    void testSIsMember_True() {
        // Given
        String key = "set:key";
        String value = "value1";
        when(setOperations.isMember(key, value)).thenReturn(true);

        // When
        Boolean result = cacheService.sIsMember(key, value);

        // Then
        assertTrue(result);
    }

    // ==================== ZSet操作测试 ====================

    @Test
    @DisplayName("添加ZSet成员 - 成功")
    void testZAdd_Success() {
        // Given
        String key = "zset:key";
        String value = "value1";
        double score = 100.0;
        when(zSetOperations.add(key, value, score)).thenReturn(true);

        // When
        Boolean result = cacheService.zAdd(key, value, score);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("获取ZSet范围 - 成功")
    void testZRange_Success() {
        // Given
        String key = "zset:key";
        Set<Object> expectedSet = new LinkedHashSet<>(Arrays.asList("value1", "value2"));
        when(zSetOperations.range(key, 0, 1)).thenReturn(expectedSet);

        // When
        Set<Object> actualSet = cacheService.zRange(key, 0, 1);

        // Then
        assertEquals(expectedSet, actualSet);
    }

    // ==================== 模式匹配测试 ====================

    @Test
    @DisplayName("根据模式删除缓存 - 成功")
    void testDeleteByPattern_Success() {
        // Given
        String pattern = "test:*";
        Set<String> keys = new HashSet<>(Arrays.asList("test:key1", "test:key2"));
        when(redisTemplate.keys(pattern)).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L);

        // When
        Long result = cacheService.deleteByPattern(pattern);

        // Then
        assertEquals(2L, result);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("根据模式获取所有键 - 成功")
    void testKeys_Success() {
        // Given
        String pattern = "test:*";
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("test:key1", "test:key2"));
        when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

        // When
        Set<String> actualKeys = cacheService.keys(pattern);

        // Then
        assertEquals(expectedKeys, actualKeys);
    }
}
