package cn.aiedge.common.cache;

/**
 * 缓存Key常量定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public final class CacheKeys {

    private CacheKeys() {}

    // ==================== 用户相关 ====================
    
    /** 用户信息 */
    public static final String USER_INFO = "user:info:";
    
    /** 用户权限 */
    public static final String USER_PERMISSIONS = "user:permission:";
    
    /** 用户角色 */
    public static final String USER_ROLES = "user:role:";
    
    /** 用户Token */
    public static final String USER_TOKEN = "user:token:";

    // ==================== 角色相关 ====================
    
    /** 角色信息 */
    public static final String ROLE_INFO = "role:info:";
    
    /** 角色权限列表 */
    public static final String ROLE_PERMISSIONS = "role:permission:";

    // ==================== 权限相关 ====================
    
    /** 权限树 */
    public static final String PERMISSION_TREE = "permission:tree:";
    
    /** 权限信息 */
    public static final String PERMISSION_INFO = "permission:info:";

    // ==================== 系统配置 ====================
    
    /** 系统配置 */
    public static final String SYS_CONFIG = "sys:config:";
    
    /** 系统菜单 */
    public static final String SYS_MENU = "sys:menu:";

    // ==================== 业务缓存 ====================
    
    /** 客户信息 */
    public static final String CUSTOMER_INFO = "crm:customer:";
    
    /** 订单信息 */
    public static final String ORDER_INFO = "erp:order:";
    
    /** 产品信息 */
    public static final String PRODUCT_INFO = "erp:product:";
    
    /** 产品库存 */
    public static final String PRODUCT_STOCK = "erp:stock:";

    // ==================== Token黑名单 ====================
    
    /** Token黑名单 */
    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    // ==================== 分布式锁 ====================
    
    /** 订单锁 */
    public static final String LOCK_ORDER = "lock:order:";
    
    /** 库存锁 */
    public static final String LOCK_STOCK = "lock:stock:";
    
    /** 支付锁 */
    public static final String LOCK_PAYMENT = "lock:payment:";

    // ==================== 计数器 ====================
    
    /** API访问计数 */
    public static final String API_COUNT = "api:count:";
    
    /** 登录失败计数 */
    public static final String LOGIN_FAIL_COUNT = "login:fail:";

    // ==================== 构建方法 ====================
    
    /**
     * 构建带租户的Key
     */
    public static String withTenant(String prefix, Long tenantId, Object... parts) {
        StringBuilder sb = new StringBuilder(prefix).append(tenantId);
        for (Object part : parts) {
            sb.append(":").append(part);
        }
        return sb.toString();
    }
    
    /**
     * 构建用户信息Key
     */
    public static String userInfo(Long tenantId, Long userId) {
        return withTenant(USER_INFO, tenantId, userId);
    }
    
    /**
     * 构建客户信息Key
     */
    public static String customerInfo(Long tenantId, Long customerId) {
        return withTenant(CUSTOMER_INFO, tenantId, customerId);
    }
    
    /**
     * 构建订单锁Key
     */
    public static String orderLock(Long orderId) {
        return LOCK_ORDER + orderId;
    }
    
    /**
     * 构建库存锁Key
     */
    public static String stockLock(Long productId) {
        return LOCK_STOCK + productId;
    }
}
