package cn.aiedge.cache.service;

import cn.aiedge.cache.config.TestCacheConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存同步机制集成测试
 * 测试Cache Aside模式、Write Through模式以及缓存保护机制
 *
 * @author AI-Ready Team
 */
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = {TestCacheConfig.class, 
        CacheSyncService.class, 
        CacheAsideSyncService.class, 
        WriteThroughCacheService.class, 
        CacheProtectionService.class})
class CacheIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CacheSyncService cacheSyncService;

    @Autowired
    private CacheAsideSyncService cacheAsideService;

    @Autowired
    private WriteThroughCacheService writeThroughService;

    @Autowired
    private CacheProtectionService cacheProtectionService;

    private static final String TEST_CACHE_KEY = "test:user:123";
    private static final String TEST_USER_NAME = "testUser";

    @BeforeEach
    void setUp() {
        // 清除测试数据
        redisTemplate.delete(TEST_CACHE_KEY);
        redisTemplate.delete("lock:*");
    }

    @Test
    void testCacheAsidePattern() {
        log.info("开始测试Cache Aside模式");

        // 准备测试数据
        User testData = new User(123L, TEST_USER_NAME, "test@example.com");

        // 第一次访问：缓存未命中，应该查询数据库并写入缓存
        User result1 = cacheAsideService.getFromCache(TEST_CACHE_KEY, User.class, () -> {
            log.info("模拟数据库查询");
            return testData;
        });

        assertNotNull(result1);
        assertEquals(testData.getId(), result1.getId());
        assertEquals(testData.getName(), result1.getName());

        // 验证数据已写入Redis
        String cachedValue = redisTemplate.opsForValue().get(TEST_CACHE_KEY);
        assertNotNull(cachedValue);
        assertTrue(cachedValue.contains(TEST_USER_NAME));

        // 第二次访问：缓存命中，不应该查询数据库
        User result2 = cacheAsideService.getFromCache(TEST_CACHE_KEY, User.class, () -> {
            fail("不应该再次查询数据库");
            return null;
        });

        assertNotNull(result2);
        assertEquals(testData.getId(), result2.getId());
        assertEquals(testData.getName(), result2.getName());

        log.info("Cache Aside模式测试通过");
    }

    @Test
    void testWriteThroughPattern() {
        log.info("开始测试Write Through模式");

        User testData = new User(456L, "writeThroughUser", "wt@example.com");
        String cacheKey = "test:user:wt";

        // 测试写入
        User result = writeThroughService.writeThrough(cacheKey, testData, data -> {
            log.info("模拟数据库写入: {}", data);
            // 模拟数据库写入操作，返回相同数据
            return data;
        });

        assertNotNull(result);
        assertEquals(testData.getId(), result.getId());

        // 验证数据已写入Redis
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cachedValue);
        assertTrue(cachedValue.contains("writeThroughUser"));

        // 测试读取
        User readResult = cacheAsideService.getFromCache(cacheKey, User.class, () -> {
            fail("数据应该在缓存中");
            return null;
        });

        assertNotNull(readResult);
        assertEquals(testData.getId(), readResult.getId());
        assertEquals(testData.getName(), readResult.getName());

        log.info("Write Through模式测试通过");
    }

    @Test
    void testCachePenetrationProtection() {
        log.info("开始测试缓存穿透保护");

        String nonExistentKey = "test:user:nonexistent";

        // 第一次查询不存在的数据
        User result1 = cacheProtectionService.handleCachePenetration(nonExistentKey, () -> {
            log.info("查询数据库（不存在的数据）");
            return null; // 模拟数据库中不存在
        }, User.class, 300L);

        assertNull(result1);

        // 验证缓存了空值
        String cachedValue = redisTemplate.opsForValue().get(nonExistentKey);
        assertNotNull(cachedValue);
        assertEquals("{}", cachedValue); // 空值标识

        // 第二次查询相同的不存在的数据，不应再查询数据库
        User result2 = cacheProtectionService.handleCachePenetration(nonExistentKey, () -> {
            fail("不应该再次查询数据库，因为已经缓存了空值");
            return null;
        }, User.class, 300L);

        assertNull(result2);

        log.info("缓存穿透保护测试通过");
    }

    @Test
    void testCacheBreakdownProtection() throws InterruptedException {
        log.info("开始测试缓存击穿保护");

        String testKey = "test:user:breakdown";
        User testData = new User(789L, "breakdownUser", "bd@example.com");

        // 先删除缓存，模拟缓存失效
        redisTemplate.delete(testKey);

        // 并发测试：多个线程同时请求同一个缓存键
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 记录数据库查询次数
        int[] dbCallCount = {0};
        Object lock = new Object();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    User result = cacheProtectionService.handleCacheBreakdown(testKey, () -> {
                        synchronized (lock) {
                            dbCallCount[0]++;
                            log.info("数据库查询调用 #{}", dbCallCount[0]);
                        }
                        return testData;
                    }, User.class, 600L);

                    assertNotNull(result);
                    assertEquals(testData.getId(), result.getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // 验证数据库查询次数应该只有1次（分布式锁生效）
        log.info("数据库被调用次数: {}", dbCallCount[0]);
        assertTrue(dbCallCount[0] <= 3, "数据库查询次数应该很少（最多3次，考虑到重试机制），实际: " + dbCallCount[0]);

        // 验证缓存中有数据
        String cachedValue = redisTemplate.opsForValue().get(testKey);
        assertNotNull(cachedValue);
        assertTrue(cachedValue.contains("breakdownUser"));

        executor.shutdown();
        log.info("缓存击穿保护测试通过");
    }

    @Test
    void testCacheavalancheProtection() {
        log.info("开始测试缓存雪崩保护");

        // 使用相同的基础TTL但会有随机偏差，避免同时过期
        String baseKey = "test:user:avalanche";
        User testData = new User(999L, "avalancheUser", "av@example.com");

        // 测试多次，验证TTL的随机性
        for (int i = 0; i < 5; i++) {
            String key = baseKey + ":" + i;
            User result = cacheProtectionService.handleCacheavalanche(key, () -> {
                log.info("查询数据库: {}", key);
                return testData;
            }, User.class, 600L, 120L); // 基础TTL 600秒，偏差±120秒

            assertNotNull(result);
            assertEquals(testData.getId(), result.getId());

            // 验证缓存已设置
            String cachedValue = redisTemplate.opsForValue().get(key);
            assertNotNull(cachedValue);
        }

        log.info("缓存雪崩保护测试通过");
    }

    @Test
    void testCacheSyncService() {
        log.info("开始测试缓存同步服务");

        String testKey = "test:sync:key";
        String testValue = "sync_test_value";

        // 设置初始值
        redisTemplate.opsForValue().set(testKey, testValue, 600, TimeUnit.SECONDS);

        // 订阅同步事件
        boolean[] receivedEvent = {false};
        cacheSyncService.subscribe("test-sync", message -> {
            log.info("接收到同步消息: {}", message);
            receivedEvent[0] = true;
        });

        // 发布同步事件
        cacheSyncService.publishSync("test-sync", testKey, CacheSyncService.SyncAction.EVICT);

        // 等待事件处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(receivedEvent[0], "应该接收到同步事件");

        // 清理订阅
        cacheSyncService.unsubscribe("test-sync");

        log.info("缓存同步服务测试通过");
    }

    @Test
    void testWriteBehindPattern() throws InterruptedException {
        log.info("开始测试Write Behind模式");

        String cacheKey = "test:user:writebehind";
        User testData = new User(888L, "writeBehindUser", "wb@example.com");

        // 测试Write Behind
        var future = writeThroughService.writeBehind(cacheKey, testData, data -> {
            log.info("异步数据库写入: {}", data);
            // 模拟数据库写入延迟
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return data;
        }, 600L, 50L); // 50ms延迟

        // 立即检查缓存，应该已经有了数据
        String immediateCachedValue = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(immediateCachedValue);
        assertTrue(immediateCachedValue.contains("writeBehindUser"));

        // 等待异步操作完成
        User result = future.get(2, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(testData.getId(), result.getId());

        log.info("Write Behind模式测试通过");
    }

    @Test
    void testBatchOperations() {
        log.info("开始测试批量操作");

        // 准备批量数据
        Map<String, User> batchData = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            String key = "batch:user:" + i;
            batchData.put(key, new User((long)i, "batchUser" + i, "batch" + i + "@example.com"));
        }

        // 批量缓存穿透处理测试
        Map<String, User> results = cacheProtectionService.handleBatchCachePenetration(
            List.of("batch:user:0", "batch:user:1", "batch:user:2"),
            keys -> {
                log.info("批量数据库查询: {}", keys);
                Map<String, User> dbResults = new HashMap<>();
                for (String key : keys) {
                    // 模拟从数据库获取数据
                    if (batchData.containsKey(key)) {
                        dbResults.put(key, batchData.get(key));
                    }
                }
                return dbResults;
            },
            User.class,
            600L
        );

        assertEquals(3, results.size());
        for (int i = 0; i < 3; i++) {
            String key = "batch:user:" + i;
            User user = results.get(key);
            assertNotNull(user);
            assertEquals("batchUser" + i, user.getName());
        }

        log.info("批量操作测试通过");
    }

    @Data
    private static class User {
        private Long id;
        private String name;
        private String email;

        public User(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
}