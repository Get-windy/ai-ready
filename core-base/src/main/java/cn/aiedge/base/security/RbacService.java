package cn.aiedge.base.security;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.service.SysPermissionService;
import cn.aiedge.base.service.SysRoleService;
import cn.aiedge.base.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 权限服务
 * 基于角色的访问控制核心业务逻辑
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacService {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysPermissionService permissionService;
    private final SecurityContext securityContext;

    /**
     * 检查用户是否有权限访问指定 API
     * 
     * @param userId 用户ID
     * @param apiPath API路径
     * @param method HTTP方法
     * @return 是否有权限
     */
    public boolean hasApiPermission(Long userId, String apiPath, String method) {
        // 获取用户所有权限
        List<String> permissionCodes = userService.getUserPermissionCodes(userId);
        
        // 获取匹配的权限
        List<SysPermission> permissions = permissionService.lambdaQuery()
                .eq(SysPermission::getApiPath, apiPath)
                .eq(SysPermission::getMethod, method)
                .eq(SysPermission::getStatus, 0)
                .list();
        
        // 检查是否匹配
        return permissions.stream()
                .anyMatch(p -> permissionCodes.contains(p.getPermissionCode()));
    }

    /**
     * 获取用户的所有权限编码
     * 
     * @param userId 用户ID
     * @return 权限编码集合
     */
    public Set<String> getUserPermissionCodes(Long userId) {
        return userService.getUserPermissionCodes(userId).stream()
                .collect(Collectors.toSet());
    }

    /**
     * 获取用户的所有角色编码
     * 
     * @param userId 用户ID
     * @return 角色编码集合
     */
    public Set<String> getUserRoleCodes(Long userId) {
        return userService.getUserRoleCodes(userId).stream()
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否是超级管理员
     * 
     * @param userId 用户ID
     * @return 是否是超级管理员
     */
    public boolean isSuperAdmin(Long userId) {
        Set<String> roles = getUserRoleCodes(userId);
        return roles.contains("SUPER_ADMIN") || roles.contains("admin");
    }

    /**
     * 检查用户是否可以访问指定租户的数据
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 是否可以访问
     */
    public boolean canAccessTenant(Long userId, Long tenantId) {
        // 超级管理员可以访问所有租户
        if (isSuperAdmin(userId)) {
            return true;
        }
        
        // 普通用户只能访问自己所属租户
        Long userTenantId = securityContext.getCurrentTenantId();
        return userTenantId != null && userTenantId.equals(tenantId);
    }

    /**
     * 验证数据权限
     * 
     * @param userId 用户ID
     * @param dataTenantId 数据所属租户ID
     * @param dataCreateBy 数据创建人
     * @param dataScope 数据权限范围 (0-全部 1-本部门 2-本部门及下级 3-仅本人)
     * @return 是否有权限
     */
    public boolean checkDataPermission(Long userId, Long dataTenantId, Long dataCreateBy, Integer dataScope) {
        // 1. 检查租户权限
        if (!canAccessTenant(userId, dataTenantId)) {
            return false;
        }

        // 2. 超级管理员有全部权限
        if (isSuperAdmin(userId)) {
            return true;
        }

        // 3. 根据数据权限范围判断
        if (dataScope == null || dataScope == 0) {
            return true; // 全部数据权限
        }

        // 4. 仅本人数据权限
        if (dataScope == 3) {
            return userId.equals(dataCreateBy);
        }

        // TODO: 实现部门级别数据权限（需要部门层级关系）
        return true;
    }

    /**
     * 获取用户的菜单树（用于前端路由）
     * 
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<?> getUserMenus(Long userId) {
        // 获取用户角色
        Set<String> roles = getUserRoleCodes(userId);
        
        // 超级管理员获取所有菜单
        if (roles.contains("SUPER_ADMIN") || roles.contains("admin")) {
            return permissionService.getPermissionTree(securityContext.getCurrentTenantId());
        }

        // 普通用户获取分配的权限菜单
        return permissionService.getUserPermissions(userId);
    }

    /**
     * 刷新用户权限缓存
     * 
     * @param userId 用户ID
     */
    @Transactional(readOnly = true)
    public void refreshUserPermissionCache(Long userId) {
        // Sa-Token 会自动刷新权限缓存
        // 如果需要手动清除，可以调用：
        // StpUtil.getSessionByLoginId(userId).delete("Permission_List");
        // StpUtil.getSessionByLoginId(userId).delete("Role_List");
        log.info("用户 {} 权限缓存已刷新", userId);
    }
}