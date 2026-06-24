package cn.aiedge.base.context;

/**
 * 数据权限上下文持有者
 * 使用 ThreadLocal 存储当前请求的数据权限SQL条件
 *
 * 工作流程：
 * 1. DataScopeAspect 在方法执行前解析用户数据权限，生成SQL条件并存入ThreadLocal
 * 2. DataScopeInterceptor 在SQL执行时读取ThreadLocal中的条件并注入SQL
 * 3. 方法执行完成后清除ThreadLocal
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class DataScopeContextHolder {

    /** 数据权限SQL条件 */
    private static final ThreadLocal<String> DATA_SCOPE_SQL = new ThreadLocal<>();

    /** 是否启用数据权限过滤 */
    private static final ThreadLocal<Boolean> DATA_SCOPE_ENABLED = new ThreadLocal<>();

    /** 目标表名（用于精确匹配） */
    private static final ThreadLocal<String> TARGET_TABLE = new ThreadLocal<>();

    /**
     * 设置数据权限SQL条件
     */
    public static void setDataScopeSql(String sql) {
        DATA_SCOPE_SQL.set(sql);
    }

    /**
     * 获取数据权限SQL条件
     */
    public static String getDataScopeSql() {
        return DATA_SCOPE_SQL.get();
    }

    /**
     * 设置是否启用数据权限
     */
    public static void setDataScopeEnabled(Boolean enabled) {
        DATA_SCOPE_ENABLED.set(enabled);
    }

    /**
     * 获取是否启用数据权限
     */
    public static Boolean isDataScopeEnabled() {
        Boolean enabled = DATA_SCOPE_ENABLED.get();
        return enabled != null && enabled;
    }

    /**
     * 设置目标表名
     */
    public static void setTargetTable(String tableName) {
        TARGET_TABLE.set(tableName);
    }

    /**
     * 获取目标表名
     */
    public static String getTargetTable() {
        return TARGET_TABLE.get();
    }

    /**
     * 清除所有上下文数据
     */
    public static void clear() {
        DATA_SCOPE_SQL.remove();
        DATA_SCOPE_ENABLED.remove();
        TARGET_TABLE.remove();
    }
}