package cn.aiedge.permission.service;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PermissionService {

    // ==================== 当前用户信息 ====================

    /**
     * 获取当前用户ID
     */
    Long getCurrentUserId();

    /**
     * 获取当前租户ID
     */
    Long getCurrentTenantId();

    /**
     * 获取当前用户部门ID
     */
    Long getCurrentUserDeptId();

    /**
     * 获取当前用户部门及子部门ID
     */
    Set<Long> getCurrentUserDeptAndChildIds();

    /**
     * 获取当前用户数据权限范围
     */
    Integer getCurrentUserDataScope();

    /**
     * 获取当前用户的自定义数据权限规则（CUSTOM 类型）
     *
     * @param tableName 目标表名
     * @return 自定义数据权限范围列表（包含部门ID、自定义SQL等）
     */
    java.util.List<cn.aiedge.base.entity.SysDataScope> getUserCustomDataScopes(String tableName);

    /**
     * 获取「已启用数据权限控制」的表名集合。
     *
     * <p>对标用友的「数据权限控制设置」（先勾选哪些业务对象要控制，再分配范围）：
     * 管理员在「角色 → 数据范围」里配置过的表进入本集合，行级数据权限拦截器据此决定
     * 是否对某条 SQL 注入过滤条件。</p>
     *
     * <p><b>集合为空时拦截器零开销直接返回</b> —— 即「没配置 = 不影响任何查询」，
     * 这是本机制可以默认开启的前提。</p>
     */
    Set<String> getEnabledDataScopeTables();

    // ==================== 权限查询 ====================

    /**
     * 获取当前用户权限编码集合
     */
    Set<String> getCurrentUserPermissions();

    /**
     * 获取当前用户角色编码集合
     */
    Set<String> getCurrentUserRoles();

    /**
     * 判断当前用户是否是超级管理员
     */
    boolean isSuperAdmin();

    /**
     * 检查当前用户是否有指定权限
     */
    boolean hasPermission(String permissionCode);

    /**
     * 检查当前用户是否有指定角色
     */
    boolean hasRole(String roleCode);

    // ==================== 用户权限管理 ====================

    /**
     * 获取用户权限编码列表
     */
    List<String> getUserPermissionCodes(Long userId);

    /**
     * 获取用户角色编码列表
     */
    List<String> getUserRoleCodes(Long userId);

    /**
     * 获取用户角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 分配用户角色
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param roleIds  角色ID列表
     */
    void assignUserRoles(Long userId, Long tenantId, List<Long> roleIds);

    /**
     * 清除用户角色
     */
    void clearUserRoles(Long userId);

    // ==================== 角色权限管理 ====================

    /**
     * 获取角色权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);

    /**
     * 分配角色权限
     *
     * @param roleId        角色ID
     * @param tenantId      租户ID
     * @param permissionIds 权限ID列表
     */
    void assignRolePermissions(Long roleId, Long tenantId, List<Long> permissionIds);

    /**
     * 清除角色权限
     */
    void clearRolePermissions(Long roleId);

    // ==================== 权限验证 ====================

    /**
     * 验证API访问权限
     */
    boolean checkApiPermission(Long userId, String apiPath, String method);

    /**
     * 验证数据访问权限
     */
    boolean checkDataPermission(Long userId, Long dataTenantId, Long dataCreateBy);

    /**
     * 验证租户访问权限
     */
    boolean checkTenantPermission(Long userId, Long tenantId);

    // ==================== 缓存管理 ====================

    /**
     * 刷新用户权限缓存
     */
    void refreshUserPermissionCache(Long userId);

    /**
     * 清除权限缓存
     */
    void clearPermissionCache();
}
