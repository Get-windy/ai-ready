package com.aiready.permission.service;

import com.aiready.permission.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<Role> {
    
    /**
     * 根据用户ID获取角色列表
     */
    List<Role> getRolesByUserId(Long userId);
    
    /**
     * 根据用户ID获取角色编码集合
     */
    Set<String> getRoleCodesByUserId(Long userId);
    
    /**
     * 检查用户是否有指定角色
     */
    boolean hasRole(Long userId, String roleCode);
    
    /**
     * 检查用户是否有任意角色
     */
    boolean hasAnyRole(Long userId, String... roleCodes);
    
    /**
     * 检查用户是否有所有角色
     */
    boolean hasAllRoles(Long userId, String... roleCodes);
    
    /**
     * 获取用户的数据权限范围
     */
    Integer getDataScopeByUserId(Long userId);
    
    /**
     * 分配用户角色
     */
    void assignUserRoles(Long userId, List<Long> roleIds);
    
    /**
     * 获取用户的角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);
    
    /**
     * 根据角色编码获取角色
     */
    Role getByRoleCode(String roleCode);
}
