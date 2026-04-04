package cn.aiedge.cache.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheKeyBuilder 测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class CacheKeyBuilderTest {

    @Test
    @DisplayName("构建缓存键 - 默认前缀")
    void testCreate_DefaultPrefix() {
        // When
        String key = CacheKeyBuilder.create()
                .append("user")
                .append("123")
                .build();

        // Then
        assertEquals("ai-ready:user:123", key);
    }

    @Test
    @DisplayName("构建缓存键 - 自定义前缀")
    void testCreate_CustomPrefix() {
        // When
        String key = CacheKeyBuilder.create("my-app")
                .append("data")
                .append("456")
                .build();

        // Then
        assertEquals("my-app:data:456", key);
    }

    @Test
    @DisplayName("构建缓存键 - 无前缀")
    void testCreateWithoutPrefix() {
        // When
        String key = CacheKeyBuilder.createWithoutPrefix()
                .append("simple")
                .append("key")
                .build();

        // Then
        assertEquals("simple:key", key);
    }

    @Test
    @DisplayName("构建缓存键 - 多部分")
    void testAppend_MultipleParts() {
        // When
        String key = CacheKeyBuilder.create()
                .append("module")
                .append("submodule")
                .append("entity")
                .append("123")
                .build();

        // Then
        assertEquals("ai-ready:module:submodule:entity:123", key);
    }

    @Test
    @DisplayName("构建缓存键 - 条件追加")
    void testAppendIf_Conditional() {
        // When - 条件为真
        String key1 = CacheKeyBuilder.create()
                .append("base")
                .appendIf(true, "conditional")
                .build();

        // When - 条件为假
        String key2 = CacheKeyBuilder.create()
                .append("base")
                .appendIf(false, "conditional")
                .build();

        // Then
        assertEquals("ai-ready:base:conditional", key1);
        assertEquals("ai-ready:base", key2);
    }

    @Test
    @DisplayName("构建缓存键 - 空值跳过")
    void testAppend_SkipNull() {
        // When
        String key = CacheKeyBuilder.create()
                .append("valid")
                .append(null)
                .append("also-valid")
                .build();

        // Then
        assertEquals("ai-ready:valid:also-valid", key);
    }

    @Test
    @DisplayName("toString 返回构建的键")
    void testToString() {
        // Given
        CacheKeyBuilder builder = CacheKeyBuilder.create()
                .append("test")
                .append("key");

        // Then
        assertEquals("ai-ready:test:key", builder.toString());
    }

    // ==================== 预定义键构建方法测试 ====================

    @Test
    @DisplayName("预定义方法 - 用户键")
    void testPredefined_UserKey() {
        assertEquals("ai-ready:user:123", CacheKeyBuilder.userKey(123L));
    }

    @Test
    @DisplayName("预定义方法 - 用户权限键")
    void testPredefined_UserPermissionsKey() {
        assertEquals("ai-ready:user:123:permissions", CacheKeyBuilder.userPermissionsKey(123L));
    }

    @Test
    @DisplayName("预定义方法 - 用户Token键")
    void testPredefined_UserTokenKey() {
        assertEquals("ai-ready:user:123:token", CacheKeyBuilder.userTokenKey(123L));
    }

    @Test
    @DisplayName("预定义方法 - 产品键")
    void testPredefined_ProductKey() {
        assertEquals("ai-ready:product:456", CacheKeyBuilder.productKey(456L));
    }

    @Test
    @DisplayName("预定义方法 - 客户键")
    void testPredefined_CustomerKey() {
        assertEquals("ai-ready:customer:789", CacheKeyBuilder.customerKey(789L));
    }

    @Test
    @DisplayName("预定义方法 - 订单键")
    void testPredefined_OrderKey() {
        assertEquals("ai-ready:order:101", CacheKeyBuilder.orderKey(101L));
    }

    @Test
    @DisplayName("预定义方法 - 库存键")
    void testPredefined_StockKey() {
        assertEquals("ai-ready:stock:1:100", CacheKeyBuilder.stockKey(100L, 1L));
    }

    @Test
    @DisplayName("预定义方法 - 系统配置键")
    void testPredefined_SysConfigKey() {
        assertEquals("ai-ready:sys:config:app.name", CacheKeyBuilder.sysConfigKey("app.name"));
    }

    @Test
    @DisplayName("预定义方法 - 字典键")
    void testPredefined_DictKey() {
        assertEquals("ai-ready:dict:status", CacheKeyBuilder.dictKey("status"));
    }
}
