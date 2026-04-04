package cn.aiedge.cache.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存过期策略测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class CacheExpirationPolicyTest {

    // ==================== SimpleExpirationPolicy 测试 ====================

    @Test
    @DisplayName("简单过期策略 - 基本属性")
    void testSimplePolicy_BasicProperties() {
        // Given
        SimpleExpirationPolicy policy = new SimpleExpirationPolicy(3600);

        // Then
        assertEquals(3600, policy.getTtlSeconds());
        assertEquals("simple-3600s", policy.getName());
        assertFalse(policy.isSliding());
        assertFalse(policy.isRefreshAhead());
    }

    @Test
    @DisplayName("简单过期策略 - 时间单位转换")
    void testSimplePolicy_TimeUnitConversion() {
        // Given
        SimpleExpirationPolicy policy = new SimpleExpirationPolicy(1, TimeUnit.HOURS);

        // Then
        assertEquals(3600, policy.getTtlSeconds());
    }

    @Test
    @DisplayName("预定义简单策略 - 验证")
    void testSimplePolicy_Predefined() {
        assertEquals(300, SimpleExpirationPolicy.SHORT.getTtlSeconds());
        assertEquals(1800, SimpleExpirationPolicy.MEDIUM.getTtlSeconds());
        assertEquals(7200, SimpleExpirationPolicy.LONG.getTtlSeconds());
        assertEquals(86400, SimpleExpirationPolicy.PERMANENT.getTtlSeconds());
        assertEquals(-1, SimpleExpirationPolicy.NEVER_EXPIRE.getTtlSeconds());
    }

    // ==================== SlidingExpirationPolicy 测试 ====================

    @Test
    @DisplayName("滑动过期策略 - 基本属性")
    void testSlidingPolicy_BasicProperties() {
        // Given
        SlidingExpirationPolicy policy = new SlidingExpirationPolicy(1800);

        // Then
        assertEquals(1800, policy.getTtlSeconds());
        assertEquals("sliding-1800s", policy.getName());
        assertTrue(policy.isSliding());
        assertFalse(policy.isRefreshAhead());
    }

    @Test
    @DisplayName("滑动过期策略 - 时间单位转换")
    void testSlidingPolicy_TimeUnitConversion() {
        // Given
        SlidingExpirationPolicy policy = new SlidingExpirationPolicy(30, TimeUnit.MINUTES);

        // Then
        assertEquals(1800, policy.getTtlSeconds());
    }

    @Test
    @DisplayName("预定义滑动策略 - 验证")
    void testSlidingPolicy_Predefined() {
        assertEquals(1800, SlidingExpirationPolicy.USER_SESSION.getTtlSeconds());
        assertEquals(3600, SlidingExpirationPolicy.USER_TOKEN.getTtlSeconds());
        assertEquals(600, SlidingExpirationPolicy.HOT_DATA.getTtlSeconds());
    }

    // ==================== RefreshAheadPolicy 测试 ====================

    @Test
    @DisplayName("预刷新策略 - 基本属性")
    void testRefreshAheadPolicy_BasicProperties() {
        // Given
        RefreshAheadPolicy policy = new RefreshAheadPolicy(3600, 0.75);

        // Then
        assertEquals(3600, policy.getTtlSeconds());
        assertEquals(0.75, policy.getRefreshThreshold());
        assertFalse(policy.isSliding());
        assertTrue(policy.isRefreshAhead());
    }

    @Test
    @DisplayName("预刷新策略 - 默认阈值")
    void testRefreshAheadPolicy_DefaultThreshold() {
        // Given
        RefreshAheadPolicy policy = new RefreshAheadPolicy(3600);

        // Then
        assertEquals(0.75, policy.getRefreshThreshold());
    }

    @Test
    @DisplayName("预刷新策略 - 刷新时间计算")
    void testRefreshAheadPolicy_RefreshAfterCalculation() {
        // Given
        RefreshAheadPolicy policy = new RefreshAheadPolicy(1000, 0.75);

        // Then
        assertEquals(750, policy.getRefreshAfterSeconds());
    }

    @Test
    @DisplayName("预刷新策略 - 阈值边界检查")
    void testRefreshAheadPolicy_ThresholdBounds() {
        // 阈值太低，应被限制到0.5
        RefreshAheadPolicy policyLow = new RefreshAheadPolicy(100, 0.1);
        assertEquals(0.5, policyLow.getRefreshThreshold());

        // 阈值太高，应被限制到0.95
        RefreshAheadPolicy policyHigh = new RefreshAheadPolicy(100, 1.0);
        assertEquals(0.95, policyHigh.getRefreshThreshold());
    }

    @Test
    @DisplayName("预定义预刷新策略 - 验证")
    void testRefreshAheadPolicy_Predefined() {
        assertEquals(3600, RefreshAheadPolicy.CONFIG.getTtlSeconds());
        assertEquals(0.75, RefreshAheadPolicy.CONFIG.getRefreshThreshold());

        assertEquals(7200, RefreshAheadPolicy.DICT.getTtlSeconds());
        assertEquals(0.80, RefreshAheadPolicy.DICT.getRefreshThreshold());

        assertEquals(1800, RefreshAheadPolicy.MENU.getTtlSeconds());
        assertEquals(0.70, RefreshAheadPolicy.MENU.getRefreshThreshold());
    }

    // ==================== CacheExpirationPolicy 接口默认方法测试 ====================

    @Test
    @DisplayName("接口默认方法 - getRefreshThreshold")
    void testInterface_DefaultRefreshThreshold() {
        CacheExpirationPolicy policy = new CacheExpirationPolicy() {
            @Override
            public long getTtlSeconds() {
                return 1000;
            }

            @Override
            public boolean isSliding() {
                return false;
            }

            @Override
            public boolean isRefreshAhead() {
                return false;
            }

            @Override
            public String getName() {
                return "test";
            }
        };

        assertEquals(0.75, policy.getRefreshThreshold());
    }
}
