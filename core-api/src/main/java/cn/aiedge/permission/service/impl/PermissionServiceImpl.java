package cn.aiedge.permission.service.impl;

import cn.aiedge.permission.service.PermissionService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 * 基于 Sa-Token 和 Redis 缓存
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String PERMISSION_CACHE_KEY = "permission:user:";
    private static final String ROLE_CACHE_KEY = "role:user:";
    private static final long CACHE_EXPIRE_HOURS = 2;

    // ==================== 当前用户信息 ====================

    @Override
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long getCurrentTenantId() {
        try {
            Object tenantId = StpUtil.getSession().get("tenantId");
            return tenantId != null ? Long.valueOf(tenantId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long getCurrentUserDeptId() {
        try {
            Object deptId = StpUtil.getSession().get("deptId");
            return deptId != null ? Long.valueOf(deptId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<Long> getCurrentUserDeptAndChildIds() {
        try {
            Object deptIds = StpUtil.getSession().get("deptAndChildIds");
            if (deptIds instanceof Set) {
                return (Set<Long>) deptIds;
            }
        } catch (Exception e) {
            log.debug("获取用户部门及子部门失败: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    @Override
    public Integer getCurrentUserDataScope() {
        try {
            Object dataScope = StpUtil.getSession().get("dataScope");
            return dataScope != null ? Integer.valueOf(dataScope.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 权限查询 ====================

    @Override
    public Set<String> getCurrentUserPermissions() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        
        List<String> permissions = getUserPermissionCodes(userId);
        return new HashSet<>(permissions);
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        
        List<String> roles = getUserRoleCodes(userId);
        return new HashSet<>(roles);
    }

    @Override
    public boolean isSuperAdmin() {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains("SUPER_ADMIN") || roles.contains("admin") || roles.contains("super_admin");
    }

    @Override
    public boolean hasPermission(String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            return true;
        }
        
        if (isSuperAdmin()) {
            return true;
        }
        
        return getCurrentUserPermissions().contains(permissionCode);
    }

    @Override
    public boolean hasRole(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return true;
        }
        
        if (isSuperAdmin()) {
            return true;
        }
        
        return getCurrentUserRoles().contains(roleCode);
    }

    // ==================== 用户权限管理 ====================

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        // 尝试从缓存获取
        String cacheKey = PERMISSION_CACHE_KEY + userId;
        List<String> cached = getCachedPermissions(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从 Sa-Token 获取权限列表
        List<String> permissions = StpUtil.getPermissionList(userId);
        
        // 缓存结果
        cachePermissions(cacheKey, permissions);
        
        return permissions != null ? permissions : Collections.emptyList();
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        // 尝试从缓存获取
        String cacheKey = ROLE_CACHE_KEY + userId;
        List<String> cached = getCachedPermissions(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从 Sa-Token 获取角色列表
        List<String> roles = StpUtil.getRoleList(userId);
        
        // 缓存结果
        cachePermissions(cacheKey, roles);
        
        return roles != null ? roles : Collections.emptyList();
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        // TODO: 从数据库查询用户角色ID列表
        return Collections.emptyList();
    }

    @Override
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null || CollUtil.isEmpty(roleIds)) {
            return;
        }
        
        // TODO: 实现用户角色分配逻辑
        // 1. 删除现有用户角色关联
        // 2. 插入新的用户角色关联
        // 3. 刷新权限缓存
        
        refreshUserPermissionCache(userId);
        log.info("用户 {} 角色分配完成: {}", userId, roleIds);
    }

    @Override
    public void clearUserRoles(Long userId) {
        if (userId == null) {
            return;
        }
        
        // TODO: 实现清除用户角色逻辑
        
        refreshUserPermissionCache(userId);
        log.info("用户 {} 角色已清除", userId);
    }

    // ==================== 角色权限管理 ====================

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        // TODO: 从数据库查询角色权限ID列表
        return Collections.emptyList();
    }

    @Override
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null || CollUtil.isEmpty(permissionIds)) {
            return;
        }
        
        // TODO: 实现角色权限分配逻辑
        // 1. 删除现有角色权限关联
        // 2. 插入新的角色权限关联
        // 3. 刷新相关用户权限缓存
        
        log.info("角色 {} 权限分配完成: {}", roleId, permissionIds);
    }

    @Override
    public void clearRolePermissions(Long roleId) {
        if (roleId == null) {
            return;
        }
        
        // TODO: 实现清除角色权限逻辑
        
        log.info("角色 {} 权限已清除", roleId);
    }

    // ==================== 权限验证 ====================

    @Override
    public boolean checkApiPermission(Long userId, String apiPath, String method) {
        if (userId == null || StrUtil.isBlank(apiPath)) {
            return false;
        }
        
        // 超级管理员有所有权限
        if (isSuperAdmin()) {
            return true;
        }
        
        // TODO: 实现API权限检查逻辑
        // 从数据库查询用户是否有该API的访问权限
        
        return true;
    }

    @Override
    public boolean checkDataPermission(Long userId, Long dataTenantId, Long dataCreateBy) {
        if (userId == null) {
            return false;
        }
        
        // 超级管理员有所有权限
        if (isSuperAdmin()) {
            return true;
        }
        
        // 检查租户权限
        if (dataTenantId != null && !checkTenantPermission(userId, dataTenantId)) {
            return false;
        }
        
        // 检查数据权限范围
        Integer dataScope = getCurrentUserDataScope();
        if (dataScope == null || dataScope == 0) {
            return true; // 全部数据权限
        }
        
        if (dataScope == 3) {
            // 仅本人数据权限
            return userId.equals(dataCreateBy);
        }
        
        // TODO: 实现部门级别数据权限检查
        
        return true;
    }

    @Override
    public boolean checkTenantPermission(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        
        // 超级管理员有所有权限
        if (isSuperAdmin()) {
            return true;
        }
        
        // 普通用户只能访问自己所属租户
        Long userTenantId = getCurrentTenantId();
        return tenantId.equals(userTenantId);
    }

    // ==================== 缓存管理 ====================

    @Override
    public void refreshUserPermissionCache(Long userId) {
        if (userId == null) {
            return;
        }
        
        // 清除Redis缓存
        redisTemplate.delete(PERMISSION_CACHE_KEY + userId);
        redisTemplate.delete(ROLE_CACHE_KEY + userId);
        
        // 清除Sa-Token会话缓存
        try {
            StpUtil.getSessionByLoginId(userId).delete("Permission_List");
            StpUtil.getSessionByLoginId(userId).delete("Role_List");
        } catch (Exception e) {
            log.debug("清除Sa-Token缓存失败: {}", e.getMessage());
        }
        
        log.info("用户 {} 权限缓存已刷新", userId);
    }

    @Override
    public void clearPermissionCache() {
        // 清除所有权限缓存
        Set<String> keys = redisTemplate.keys(PERMISSION_CACHE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        Set<String> roleKeys = redisTemplate.keys(ROLE_CACHE_KEY + "*");
        if (roleKeys != null && !roleKeys.isEmpty()) {
            redisTemplate.delete(roleKeys);
        }
        
        log.info("权限缓存已全部清除");
    }

    // ==================== 私有方法 ====================

    @SuppressWarnings("unchecked")
    private List<String> getCachedPermissions(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.debug("获取缓存失败: {}", e.getMessage());
        }
        return null;
    }

    private void cachePermissions(String cacheKey, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        
        try {
            redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("缓存权限失败: {}", e.getMessage());
        }
    }
}
