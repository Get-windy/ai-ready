package cn.aiedge.cache.service;

import cn.aiedge.cache.config.CacheNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * 缓存失效管理服务
 * 提供缓存清除策略
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class CacheEvictService {

    @Autowired
    private CacheService cacheService;

    // ==================== 用户相关缓存清除 ====================

    /**
     * 清除用户信息缓存
     *
     * @param userId 用户ID
     */
    public void evictUserInfo(Long userId) {
        cacheService.delete(CacheNames.USER_INFO_KEY_PREFIX + userId);
    }

    /**
     * 批量清除用户信息缓存
     *
     * @param userIds 用户ID集合
     */
    public void evictUserInfoBatch(Collection<Long> userIds) {
        userIds.forEach(this::evictUserInfo);
    }

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    public void evictUserPermissions(Long userId) {
        cacheService.delete(CacheNames.USER_PERMISSIONS_KEY_PREFIX + userId);
    }

    /**
     * 清除用户所有相关缓存
     *
     * @param userId 用户ID
     */
    public void evictUserAll(Long userId) {
        evictUserInfo(userId);
        evictUserPermissions(userId);
        cacheService.delete(CacheNames.USER_ROLES + ":" + userId);
        cacheService.delete(CacheNames.USER_TOKEN + ":" + userId);
    }

    /**
     * 清除角色权限缓存
     *
     * @param roleId 角色ID
     */
    public void evictRolePermissions(Long roleId) {
        cacheService.delete(CacheNames.ROLE_PERMISSIONS + ":" + roleId);
    }

    // ==================== 业务数据缓存清除 ====================

    /**
     * 清除产品缓存
     *
     * @param productId 产品ID
     */
    public void evictProduct(Long productId) {
        cacheService.delete(CacheNames.PRODUCT_KEY_PREFIX + productId);
    }

    /**
     * 清除所有产品缓存
     */
    public void evictAllProducts() {
        cacheService.deleteByPattern(CacheNames.PRODUCT_KEY_PREFIX + "*");
    }

    /**
     * 清除客户缓存
     *
     * @param customerId 客户ID
     */
    public void evictCustomer(Long customerId) {
        cacheService.delete(CacheNames.CUSTOMER_KEY_PREFIX + customerId);
    }

    /**
     * 清除所有客户缓存
     */
    public void evictAllCustomers() {
        cacheService.deleteByPattern(CacheNames.CUSTOMER_KEY_PREFIX + "*");
    }

    /**
     * 清除订单缓存
     *
     * @param orderId 订单ID
     */
    public void evictOrder(Long orderId) {
        cacheService.delete(CacheNames.ORDER_KEY_PREFIX + orderId);
    }

    /**
     * 清除所有订单缓存
     */
    public void evictAllOrders() {
        cacheService.deleteByPattern(CacheNames.ORDER_KEY_PREFIX + "*");
    }

    /**
     * 清除库存缓存
     *
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     */
    public void evictStock(Long productId, Long warehouseId) {
        String key = CacheNames.STOCK_KEY_PREFIX + warehouseId + ":" + productId;
        cacheService.delete(key);
    }

    /**
     * 清除产品在所有仓库的库存缓存
     *
     * @param productId 产品ID
     */
    public void evictStockByProduct(Long productId) {
        cacheService.deleteByPattern(CacheNames.STOCK_KEY_PREFIX + "*:" + productId);
    }

    /**
     * 清除仓库的所有库存缓存
     *
     * @param warehouseId 仓库ID
     */
    public void evictStockByWarehouse(Long warehouseId) {
        cacheService.deleteByPattern(CacheNames.STOCK_KEY_PREFIX + warehouseId + ":*");
    }

    // ==================== 系统缓存清除 ====================

    /**
     * 清除系统配置缓存
     *
     * @param configKey 配置键
     */
    public void evictSysConfig(String configKey) {
        cacheService.delete(CacheNames.SYS_CONFIG + ":" + configKey);
    }

    /**
     * 清除所有系统配置缓存
     */
    public void evictAllSysConfig() {
        cacheService.deleteByPattern(CacheNames.SYS_CONFIG + ":*");
    }

    /**
     * 清除字典缓存
     *
     * @param dictType 字典类型
     */
    public void evictDict(String dictType) {
        cacheService.delete(CacheNames.DICT_DATA + ":" + dictType);
    }

    /**
     * 清除所有字典缓存
     */
    public void evictAllDict() {
        cacheService.deleteByPattern(CacheNames.DICT_DATA + ":*");
    }

    /**
     * 清除菜单缓存
     */
    public void evictMenu() {
        cacheService.deleteByPattern(CacheNames.MENU_DATA + ":*");
    }

    /**
     * 清除部门缓存
     *
     * @param deptId 部门ID
     */
    public void evictDept(Long deptId) {
        cacheService.delete(CacheNames.DEPT_DATA + ":" + deptId);
    }

    /**
     * 清除所有部门缓存
     */
    public void evictAllDept() {
        cacheService.deleteByPattern(CacheNames.DEPT_DATA + ":*");
    }

    // ==================== 批量清除 ====================

    /**
     * 清除所有业务缓存
     */
    public void evictAllBusiness() {
        evictAllProducts();
        evictAllCustomers();
        evictAllOrders();
        cacheService.deleteByPattern(CacheNames.STOCK_KEY_PREFIX + "*");
    }

    /**
     * 清除所有缓存
     */
    public void evictAll() {
        cacheService.deleteByPattern("ai-ready:*");
    }

    /**
     * 根据前缀清除缓存
     *
     * @param prefix 缓存键前缀
     */
    public void evictByPrefix(String prefix) {
        cacheService.deleteByPattern(prefix + "*");
    }
}
