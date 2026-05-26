package cn.aiedge.cache.builder;

import java.util.StringJoiner;

/**
 * 缓存键构建器
 * 提供流畅的API构建缓存键
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class CacheKeyBuilder {

    private static final String DEFAULT_SEPARATOR = ":";
    private static final String DEFAULT_PREFIX = "ai-ready";

    private final StringJoiner joiner;
    private final String separator;

    private CacheKeyBuilder(String prefix, String separator) {
        this.separator = separator;
        this.joiner = new StringJoiner(separator);
        if (prefix != null && !prefix.isEmpty()) {
            joiner.add(prefix);
        }
    }

    /**
     * 创建缓存键构建器（使用默认前缀和分隔符）
     *
     * @return CacheKeyBuilder
     */
    public static CacheKeyBuilder create() {
        return new CacheKeyBuilder(DEFAULT_PREFIX, DEFAULT_SEPARATOR);
    }

    /**
     * 创建缓存键构建器（使用指定前缀）
     *
     * @param prefix 缓存键前缀
     * @return CacheKeyBuilder
     */
    public static CacheKeyBuilder create(String prefix) {
        return new CacheKeyBuilder(prefix, DEFAULT_SEPARATOR);
    }

    /**
     * 创建缓存键构建器（不使用前缀）
     *
     * @return CacheKeyBuilder
     */
    public static CacheKeyBuilder createWithoutPrefix() {
        return new CacheKeyBuilder(null, DEFAULT_SEPARATOR);
    }

    /**
     * 添加键部分
     *
     * @param part 键部分
     * @return CacheKeyBuilder
     */
    public CacheKeyBuilder append(Object part) {
        if (part != null) {
            joiner.add(String.valueOf(part));
        }
        return this;
    }

    /**
     * 添加多个键部分
     *
     * @param parts 键部分数组
     * @return CacheKeyBuilder
     */
    public CacheKeyBuilder append(Object... parts) {
        if (parts != null) {
            for (Object part : parts) {
                append(part);
            }
        }
        return this;
    }

    /**
     * 添加条件键部分
     *
     * @param condition 条件
     * @param part      键部分
     * @return CacheKeyBuilder
     */
    public CacheKeyBuilder appendIf(boolean condition, Object part) {
        if (condition && part != null) {
            joiner.add(String.valueOf(part));
        }
        return this;
    }

    /**
     * 构建最终的缓存键
     *
     * @return 缓存键
     */
    public String build() {
        return joiner.toString();
    }

    @Override
    public String toString() {
        return build();
    }

    // ==================== 预定义键构建方法 ====================

    /**
     * 构建用户信息缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String userKey(Long userId) {
        return create().append("user").append(userId).build();
    }

    /**
     * 构建用户权限缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String userPermissionsKey(Long userId) {
        return create().append("user").append(userId).append("permissions").build();
    }

    /**
     * 构建用户Token缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String userTokenKey(Long userId) {
        return create().append("user").append(userId).append("token").build();
    }

    /**
     * 构建产品缓存键
     *
     * @param productId 产品ID
     * @return 缓存键
     */
    public static String productKey(Long productId) {
        return create().append("product").append(productId).build();
    }

    /**
     * 构建客户缓存键
     *
     * @param customerId 客户ID
     * @return 缓存键
     */
    public static String customerKey(Long customerId) {
        return create().append("customer").append(customerId).build();
    }

    /**
     * 构建订单缓存键
     *
     * @param orderId 订单ID
     * @return 缓存键
     */
    public static String orderKey(Long orderId) {
        return create().append("order").append(orderId).build();
    }

    /**
     * 构建库存缓存键
     *
     * @param productId   产品ID
     * @param warehouseId 仓库ID
     * @return 缓存键
     */
    public static String stockKey(Long productId, Long warehouseId) {
        return create().append("stock").append(warehouseId).append(productId).build();
    }

    /**
     * 构建系统配置缓存键
     *
     * @param configKey 配置键
     * @return 缓存键
     */
    public static String sysConfigKey(String configKey) {
        return create().append("sys").append("config").append(configKey).build();
    }

    /**
     * 构建字典缓存键
     *
     * @param dictType 字典类型
     * @return 缓存键
     */
    public static String dictKey(String dictType) {
        return create().append("dict").append(dictType).build();
    }
}
