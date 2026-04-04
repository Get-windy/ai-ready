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
     */
    void assignUserRoles(Long userId, List<Long> roleIds);

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
     */
    void assignRolePermissions(Long roleId, List<Long> permissionIds);

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
