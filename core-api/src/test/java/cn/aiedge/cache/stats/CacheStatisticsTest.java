package cn.aiedge.cache.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheStatistics 测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class CacheStatisticsTest {

    private CacheStatistics cacheStatistics;

    @BeforeEach
    void setUp() {
        cacheStatistics = new CacheStatistics();
    }

    @Test
    @DisplayName("记录缓存命中")
    void testRecordHit() {
        // When
        cacheStatistics.recordHit("test-cache");
        cacheStatistics.recordHit("test-cache");
        cacheStatistics.recordHit("test-cache");

        // Then
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");
        assertEquals(3, summary.getHits());
        assertEquals(0, summary.getMisses());
    }

    @Test
    @DisplayName("记录缓存未命中")
    void testRecordMiss() {
        // When
        cacheStatistics.recordMiss("test-cache");
        cacheStatistics.recordMiss("test-cache");

        // Then
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");
        assertEquals(0, summary.getHits());
        assertEquals(2, summary.getMisses());
    }

    @Test
    @DisplayName("计算命中率 - 只有命中")
    void testHitRate_OnlyHits() {
        // When
        for (int i = 0; i < 10; i++) {
            cacheStatistics.recordHit("test-cache");
        }

        // Then
        assertEquals(1.0, cacheStatistics.getHitRate("test-cache"), 0.001);
    }

    @Test
    @DisplayName("计算命中率 - 只有未命中")
    void testHitRate_OnlyMisses() {
        // When
        for (int i = 0; i < 10; i++) {
            cacheStatistics.recordMiss("test-cache");
        }

        // Then
        assertEquals(0.0, cacheStatistics.getHitRate("test-cache"), 0.001);
    }

    @Test
    @DisplayName("计算命中率 - 混合情况")
    void testHitRate_Mixed() {
        // When - 7次命中，3次未命中
        for (int i = 0; i < 7; i++) {
            cacheStatistics.recordHit("test-cache");
        }
        for (int i = 0; i < 3; i++) {
            cacheStatistics.recordMiss("test-cache");
        }

        // Then
        assertEquals(0.7, cacheStatistics.getHitRate("test-cache"), 0.001);
    }

    @Test
    @DisplayName("记录缓存加载")
    void testRecordLoad() {
        // When
        cacheStatistics.recordLoad("test-cache");
        cacheStatistics.recordLoad("test-cache");

        // Then
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");
        assertEquals(2, summary.getLoads());
    }

    @Test
    @DisplayName("记录缓存加载失败")
    void testRecordLoadFailure() {
        // When
        cacheStatistics.recordLoad("test-cache");
        cacheStatistics.recordLoadFailure("test-cache");

        // Then
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");
        assertEquals(1, summary.getLoads());
        assertEquals(1, summary.getLoadFailures());
        assertEquals(0.5, summary.getLoadSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("记录缓存驱逐")
    void testRecordEviction() {
        // When
        cacheStatistics.recordEviction("test-cache");
        cacheStatistics.recordEviction("test-cache");
        cacheStatistics.recordEviction("test-cache");

        // Then
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");
        assertEquals(3, summary.getEvictions());
    }

    @Test
    @DisplayName("获取所有统计摘要")
    void testGetAllSummaries() {
        // When
        cacheStatistics.recordHit("cache1");
        cacheStatistics.recordMiss("cache1");
        cacheStatistics.recordHit("cache2");
        cacheStatistics.recordHit("cache2");
        cacheStatistics.recordMiss("cache2");

        // Then
        Map<String, CacheStatistics.CacheStatsSummary> summaries = cacheStatistics.getAllSummaries();
        assertEquals(2, summaries.size());
        assertTrue(summaries.containsKey("cache1"));
        assertTrue(summaries.containsKey("cache2"));
    }

    @Test
    @DisplayName("重置指定缓存统计")
    void testReset() {
        // Given
        cacheStatistics.recordHit("test-cache");
        cacheStatistics.recordMiss("test-cache");

        // When
        cacheStatistics.reset("test-cache");

        // Then
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");
        assertEquals(0, summary.getHits());
        assertEquals(0, summary.getMisses());
    }

    @Test
    @DisplayName("重置所有统计")
    void testResetAll() {
        // Given
        cacheStatistics.recordHit("cache1");
        cacheStatistics.recordHit("cache2");

        // When
        cacheStatistics.resetAll();

        // Then
        Map<String, CacheStatistics.CacheStatsSummary> summaries = cacheStatistics.getAllSummaries();
        assertTrue(summaries.isEmpty());
    }

    @Test
    @DisplayName("统计摘要 - toString")
    void testStatsSummary_ToString() {
        // Given
        cacheStatistics.recordHit("test-cache");
        cacheStatistics.recordMiss("test-cache");

        // When
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");

        // Then
        String str = summary.toString();
        assertTrue(str.contains("test-cache"));
        assertTrue(str.contains("hits=1"));
        assertTrue(str.contains("misses=1"));
    }

    @Test
    @DisplayName("统计摘要 - 总请求数")
    void testStatsSummary_TotalRequests() {
        // Given
        cacheStatistics.recordHit("test-cache");
        cacheStatistics.recordHit("test-cache");
        cacheStatistics.recordMiss("test-cache");

        // When
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("test-cache");

        // Then
        assertEquals(3, summary.getTotalRequests());
    }

    @Test
    @DisplayName("不存在的缓存 - 返回空统计")
    void testNonExistentCache() {
        // When
        CacheStatistics.CacheStatsSummary summary = cacheStatistics.getSummary("non-existent");

        // Then
        assertEquals("non-existent", summary.getCacheName());
        assertEquals(0, summary.getHits());
        assertEquals(0, summary.getMisses());
        assertEquals(0.0, summary.getHitRate(), 0.001);
    }
}
