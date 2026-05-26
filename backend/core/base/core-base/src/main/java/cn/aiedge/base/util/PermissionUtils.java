package cn.aiedge.base.util;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.SysRoleService;
import cn.aiedge.base.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限管理工具类
 * 提供通用的权限管理功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionUtils {

    private final SysUserService userService;
    private final SysRoleService roleService;

    /**
     * 检查用户是否拥有指定角色
     */
    public boolean userHasRole(Long userId, String roleCode) {
        if (userId == null || roleCode == null) {
            return false;
        }

        List<String> userRoles = userService.getUserRoleCodes(userId);
        return userRoles.contains(roleCode);
    }

    /**
     * 检查用户是否拥有指定权限
     */
    public boolean userHasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }

        List<String> userPermissions = userService.getUserPermissionCodes(userId);
        return userPermissions.contains(permissionCode);
    }

    /**
     * 检查用户是否拥有任意一个指定权限
     */
    public boolean userHasAnyPermission(Long userId, List<String> permissionCodes) {
        if (userId == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }

        List<String> userPermissions = userService.getUserPermissionCodes(userId);
        return permissionCodes.stream()
                .anyMatch(userPermissions::contains);
    }

    /**
     * 检查用户是否拥有所有指定权限
     */
    public boolean userHasAllPermissions(Long userId, List<String> permissionCodes) {
        if (userId == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }

        List<String> userPermissions = userService.getUserPermissionCodes(userId);
        return permissionCodes.stream()
                .allMatch(userPermissions::contains);
    }

    /**
     * 获取用户的角色信息
     */
    public List<SysRole> getUserRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return roleService.getUserRoles(userId);
    }

    /**
     * 获取用户的角色编码列表
     */
    public List<String> getUserRoleCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userService.getUserRoleCodes(userId);
    }

    /**
     * 获取用户的信息
     */
    public SysUser getUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        return userService.getUserDetail(userId);
    }

    /**
     * 检查用户是否是超级管理员
     */
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }

        List<String> roles = getUserRoleCodes(userId);
        return roles.contains("SUPER_ADMIN") || roles.contains("admin");
    }

    /**
     * 检查用户是否是系统管理员
     */
    public boolean isSystemAdmin(Long userId) {
        if (userId == null) {
            return false;
        }

        List<String> roles = getUserRoleCodes(userId);
        return roles.contains("SYSTEM_ADMIN") || isSuperAdmin(userId);
    }

    /**
     * 检查用户是否是部门管理员
     */
    public boolean isDepartmentAdmin(Long userId) {
        if (userId == null) {
            return false;
        }

        List<String> roles = getUserRoleCodes(userId);
        return roles.contains("DEPT_ADMIN") || isSystemAdmin(userId);
    }

    /**
     * 获取用户的所有权限（包括角色权限和直接分配的权限）
     */
    public List<String> getAllUserPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }

        // 获取用户直接分配的权限
        List<String> directPermissions = userService.getUserPermissionCodes(userId);

        // 获取用户角色的权限
        List<SysRole> userRoles = getUserRoles(userId);
        List<String> rolePermissions = userRoles.stream()
                .map(role -> {
                    // 这里需要通过角色获取权限，可能需要额外的调用
                    // 实际实现可能会根据具体业务逻辑有所不同
                    return roleService.getRolePermissionIds(role.getId());
                })
                .flatMap(List::stream)
                .map(String::valueOf) // 假设权限ID转换为字符串形式
                .collect(Collectors.toList());

        // 合并所有权限并去重
        List<String> allPermissions = directPermissions;
        allPermissions.addAll(rolePermissions);

        return allPermissions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 检查权限编码是否符合规范
     */
    public boolean isValidPermissionCode(String permissionCode) {
        if (permissionCode == null || permissionCode.trim().isEmpty()) {
            return false;
        }

        // 权限编码规范：字母、数字、冒号、横线、下划线组成
        return permissionCode.matches("^[a-zA-Z0-9:_\\-]+$");
    }

    /**
     * 检查角色编码是否符合规范
     */
    public boolean isValidRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return false;
        }

        // 角色编码规范：字母、数字、下划线组成
        return roleCode.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * 格式化权限编码（确保符合命名规范）
     */
    public String formatPermissionCode(String permissionCode) {
        if (permissionCode == null) {
            return null;
        }

        // 转换为小写，替换空格为下划线
        return permissionCode.toLowerCase().trim().replaceAll("\\s+", "_");
    }

    /**
     * 格式化角色编码（确保符合命名规范）
     */
    public String formatRoleCode(String roleCode) {
        if (roleCode == null) {
            return null;
        }

        // 转换为大写，替换空格为下划线
        return roleCode.toUpperCase().trim().replaceAll("\\s+", "_");
    }
}