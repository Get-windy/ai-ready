package com.aiready.permission.service;

import com.aiready.permission.entity.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Permission> {
    
    /**
     * 根据用户ID获取权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);
    
    /**
     * 根据用户ID获取权限编码集合
     */
    Set<String> getPermissionCodesByUserId(Long userId);
    
    /**
     * 根据角色ID获取权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);
    
    /**
     * 获取权限树
     */
    List<Permission> getPermissionTree();
    
    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(Long userId, String permissionCode);
    
    /**
     * 检查用户是否有任意权限
     */
    boolean hasAnyPermission(Long userId, String... permissionCodes);
    
    /**
     * 检查用户是否有所有权限
     */
    boolean hasAllPermissions(Long userId, String... permissionCodes);
    
    /**
     * 分配角色权限
     */
    void assignRolePermissions(Long roleId, List<Long> permissionIds);
    
    /**
     * 获取角色的权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);
}
