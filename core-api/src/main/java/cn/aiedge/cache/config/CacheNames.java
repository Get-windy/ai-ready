package cn.aiedge.cache.config;

/**
 * 缓存名称常量定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class CacheNames {

    private CacheNames() {}

    // ==================== 用户相关缓存 ====================
    
    /**
     * 用户信息缓存
     */
    public static final String USER_INFO = "user:info";
    
    /**
     * 用户Token缓存
     */
    public static final String USER_TOKEN = "user:token";
    
    /**
     * 用户权限缓存
     */
    public static final String USER_PERMISSIONS = "user:permissions";
    
    /**
     * 用户角色缓存
     */
    public static final String USER_ROLES = "user:roles";
    
    /**
     * 角色权限缓存
     */
    public static final String ROLE_PERMISSIONS = "role:permissions";

    // ==================== 业务数据缓存 ====================
    
    /**
     * 产品数据缓存
     */
    public static final String PRODUCT_DATA = "product:data";
    
    /**
     * 客户数据缓存
     */
    public static final String CUSTOMER_DATA = "customer:data";
    
    /**
     * 订单数据缓存
     */
    public static final String ORDER_DATA = "order:data";
    
    /**
     * 库存数据缓存
     */
    public static final String STOCK_DATA = "stock:data";
    
    /**
     * 仓库数据缓存
     */
    public static final String WAREHOUSE_DATA = "warehouse:data";

    // ==================== 系统缓存 ====================
    
    /**
     * 系统配置缓存
     */
    public static final String SYS_CONFIG = "sys:config";
    
    /**
     * 字典数据缓存
     */
    public static final String DICT_DATA = "dict:data";
    
    /**
     * 菜单缓存
     */
    public static final String MENU_DATA = "menu:data";
    
    /**
     * 部门缓存
     */
    public static final String DEPT_DATA = "dept:data";

    // ==================== 缓存Key前缀 ====================
    
    /**
     * 用户信息Key前缀
     */
    public static final String USER_INFO_KEY_PREFIX = "user:info:";
    
    /**
     * 用户权限Key前缀
     */
    public static final String USER_PERMISSIONS_KEY_PREFIX = "user:perms:";
    
    /**
     * 产品数据Key前缀
     */
    public static final String PRODUCT_KEY_PREFIX = "product:";
    
    /**
     * 客户数据Key前缀
     */
    public static final String CUSTOMER_KEY_PREFIX = "customer:";
    
    /**
     * 订单数据Key前缀
     */
    public static final String ORDER_KEY_PREFIX = "order:";
    
    /**
     * 库存数据Key前缀
     */
    public static final String STOCK_KEY_PREFIX = "stock:";

    // ==================== 缓存过期时间（秒） ====================
    
    /**
     * 默认过期时间：1小时
     */
    public static final long DEFAULT_TTL = 3600;
    
    /**
     * 短期缓存：5分钟
     */
    public static final long SHORT_TTL = 300;
    
    /**
     * 中期缓存：30分钟
     */
    public static final long MEDIUM_TTL = 1800;
    
    /**
     * 长期缓存：2小时
     */
    public static final long LONG_TTL = 7200;
    
    /**
     * 永久缓存：1天
     */
    public static final long PERMANENT_TTL = 86400;
}
