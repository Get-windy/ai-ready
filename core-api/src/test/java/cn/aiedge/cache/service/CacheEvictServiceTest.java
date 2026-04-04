package cn.aiedge.cache.service;

import cn.aiedge.cache.config.CacheNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheEvictService 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CacheEvictServiceTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CacheEvictService cacheEvictService;

    // ==================== 用户相关缓存清除测试 ====================

    @Test
    @DisplayName("清除用户信息缓存 - 成功")
    void testEvictUserInfo_Success() {
        // Given
        Long userId = 1L;

        // When
        cacheEvictService.evictUserInfo(userId);

        // Then
        verify(cacheService).delete(CacheNames.USER_INFO_KEY_PREFIX + userId);
    }

    @Test
    @DisplayName("批量清除用户信息缓存 - 成功")
    void testEvictUserInfoBatch_Success() {
        // Given
        Set<Long> userIds = new HashSet<>(Arrays.asList(1L, 2L, 3L));

        // When
        cacheEvictService.evictUserInfoBatch(userIds);

        // Then
        verify(cacheService).delete(CacheNames.USER_INFO_KEY_PREFIX + 1L);
        verify(cacheService).delete(CacheNames.USER_INFO_KEY_PREFIX + 2L);
        verify(cacheService).delete(CacheNames.USER_INFO_KEY_PREFIX + 3L);
    }

    @Test
    @DisplayName("清除用户权限缓存 - 成功")
    void testEvictUserPermissions_Success() {
        // Given
        Long userId = 1L;

        // When
        cacheEvictService.evictUserPermissions(userId);

        // Then
        verify(cacheService).delete(CacheNames.USER_PERMISSIONS_KEY_PREFIX + userId);
    }

    @Test
    @DisplayName("清除用户所有相关缓存 - 成功")
    void testEvictUserAll_Success() {
        // Given
        Long userId = 1L;

        // When
        cacheEvictService.evictUserAll(userId);

        // Then
        verify(cacheService).delete(CacheNames.USER_INFO_KEY_PREFIX + userId);
        verify(cacheService).delete(CacheNames.USER_PERMISSIONS_KEY_PREFIX + userId);
        verify(cacheService).delete(CacheNames.USER_ROLES + ":" + userId);
        verify(cacheService).delete(CacheNames.USER_TOKEN + ":" + userId);
    }

    @Test
    @DisplayName("清除角色权限缓存 - 成功")
    void testEvictRolePermissions_Success() {
        // Given
        Long roleId = 1L;

        // When
        cacheEvictService.evictRolePermissions(roleId);

        // Then
        verify(cacheService).delete(CacheNames.ROLE_PERMISSIONS + ":" + roleId);
    }

    // ==================== 业务数据缓存清除测试 ====================

    @Test
    @DisplayName("清除产品缓存 - 成功")
    void testEvictProduct_Success() {
        // Given
        Long productId = 1L;

        // When
        cacheEvictService.evictProduct(productId);

        // Then
        verify(cacheService).delete(CacheNames.PRODUCT_KEY_PREFIX + productId);
    }

    @Test
    @DisplayName("清除所有产品缓存 - 成功")
    void testEvictAllProducts_Success() {
        // When
        cacheEvictService.evictAllProducts();

        // Then
        verify(cacheService).deleteByPattern(CacheNames.PRODUCT_KEY_PREFIX + "*");
    }

    @Test
    @DisplayName("清除客户缓存 - 成功")
    void testEvictCustomer_Success() {
        // Given
        Long customerId = 1L;

        // When
        cacheEvictService.evictCustomer(customerId);

        // Then
        verify(cacheService).delete(CacheNames.CUSTOMER_KEY_PREFIX + customerId);
    }

    @Test
    @DisplayName("清除所有客户缓存 - 成功")
    void testEvictAllCustomers_Success() {
        // When
        cacheEvictService.evictAllCustomers();

        // Then
        verify(cacheService).deleteByPattern(CacheNames.CUSTOMER_KEY_PREFIX + "*");
    }

    @Test
    @DisplayName("清除订单缓存 - 成功")
    void testEvictOrder_Success() {
        // Given
        Long orderId = 1L;

        // When
        cacheEvictService.evictOrder(orderId);

        // Then
        verify(cacheService).delete(CacheNames.ORDER_KEY_PREFIX + orderId);
    }

    @Test
    @DisplayName("清除库存缓存 - 成功")
    void testEvictStock_Success() {
        // Given
        Long productId = 1L;
        Long warehouseId = 2L;

        // When
        cacheEvictService.evictStock(productId, warehouseId);

        // Then
        verify(cacheService).delete(CacheNames.STOCK_KEY_PREFIX + warehouseId + ":" + productId);
    }

    @Test
    @DisplayName("清除产品在所有仓库的库存缓存 - 成功")
    void testEvictStockByProduct_Success() {
        // Given
        Long productId = 1L;

        // When
        cacheEvictService.evictStockByProduct(productId);

        // Then
        verify(cacheService).deleteByPattern(CacheNames.STOCK_KEY_PREFIX + "*:" + productId);
    }

    // ==================== 系统缓存清除测试 ====================

    @Test
    @DisplayName("清除系统配置缓存 - 成功")
    void testEvictSysConfig_Success() {
        // Given
        String configKey = "app.name";

        // When
        cacheEvictService.evictSysConfig(configKey);

        // Then
        verify(cacheService).delete(CacheNames.SYS_CONFIG + ":" + configKey);
    }

    @Test
    @DisplayName("清除所有系统配置缓存 - 成功")
    void testEvictAllSysConfig_Success() {
        // When
        cacheEvictService.evictAllSysConfig();

        // Then
        verify(cacheService).deleteByPattern(CacheNames.SYS_CONFIG + ":*");
    }

    @Test
    @DisplayName("清除字典缓存 - 成功")
    void testEvictDict_Success() {
        // Given
        String dictType = "status";

        // When
        cacheEvictService.evictDict(dictType);

        // Then
        verify(cacheService).delete(CacheNames.DICT_DATA + ":" + dictType);
    }

    @Test
    @DisplayName("清除所有字典缓存 - 成功")
    void testEvictAllDict_Success() {
        // When
        cacheEvictService.evictAllDict();

        // Then
        verify(cacheService).deleteByPattern(CacheNames.DICT_DATA + ":*");
    }

    // ==================== 批量清除测试 ====================

    @Test
    @DisplayName("清除所有业务缓存 - 成功")
    void testEvictAllBusiness_Success() {
        // When
        cacheEvictService.evictAllBusiness();

        // Then
        verify(cacheService).deleteByPattern(CacheNames.PRODUCT_KEY_PREFIX + "*");
        verify(cacheService).deleteByPattern(CacheNames.CUSTOMER_KEY_PREFIX + "*");
        verify(cacheService).deleteByPattern(CacheNames.ORDER_KEY_PREFIX + "*");
        verify(cacheService).deleteByPattern(CacheNames.STOCK_KEY_PREFIX + "*");
    }

    @Test
    @DisplayName("清除所有缓存 - 成功")
    void testEvictAll_Success() {
        // When
        cacheEvictService.evictAll();

        // Then
        verify(cacheService).deleteByPattern("ai-ready:*");
    }

    @Test
    @DisplayName("根据前缀清除缓存 - 成功")
    void testEvictByPrefix_Success() {
        // Given
        String prefix = "custom:";

        // When
        cacheEvictService.evictByPrefix(prefix);

        // Then
        verify(cacheService).deleteByPattern(prefix + "*");
    }
}
