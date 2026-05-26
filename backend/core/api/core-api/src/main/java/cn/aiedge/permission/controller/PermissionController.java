package cn.aiedge.permission.controller;

import cn.aiedge.permission.annotation.RequirePermission;
import cn.aiedge.permission.dto.PermissionDTO;
import cn.aiedge.permission.dto.RoleDTO;
import cn.aiedge.permission.dto.UserRoleDTO;
import cn.aiedge.permission.service.PermissionService;
import cn.aiedge.permission.vo.PermissionVO;
import cn.aiedge.permission.vo.RoleVO;
import cn.aiedge.permission.vo.UserPermissionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 权限管理控制器
 * 提供权限、角色、用户权限的管理接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "权限管理", description = "权限、角色、用户权限的管理接口")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // ==================== 当前用户权限查询 ====================

    @Operation(summary = "获取当前用户权限")
    @GetMapping("/current/permissions")
    public Set<String> getCurrentUserPermissions() {
        return permissionService.getCurrentUserPermissions();
    }

    @Operation(summary = "获取当前用户角色")
    @GetMapping("/current/roles")
    public Set<String> getCurrentUserRoles() {
        return permissionService.getCurrentUserRoles();
    }

    @Operation(summary = "检查当前用户是否有指定权限")
    @GetMapping("/current/check-permission")
    public boolean checkCurrentUserPermission(
            @Parameter(description = "权限编码") @RequestParam String permissionCode) {
        return permissionService.hasPermission(permissionCode);
    }

    @Operation(summary = "检查当前用户是否有指定角色")
    @GetMapping("/current/check-role")
    public boolean checkCurrentUserRole(
            @Parameter(description = "角色编码") @RequestParam String roleCode) {
        return permissionService.hasRole(roleCode);
    }

    // ==================== 用户权限管理 ====================

    @Operation(summary = "获取用户权限列表")
    @GetMapping("/user/{userId}/permissions")
    @RequirePermission("system:permission:view")
    public List<String> getUserPermissions(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        return permissionService.getUserPermissionCodes(userId);
    }

    @Operation(summary = "获取用户角色列表")
    @GetMapping("/user/{userId}/roles")
    @RequirePermission("system:role:view")
    public List<String> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        return permissionService.getUserRoleCodes(userId);
    }

    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/user/{userId}/role-ids")
    @RequirePermission("system:role:view")
    public List<Long> getUserRoleIds(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        return permissionService.getUserRoleIds(userId);
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/user/assign-roles")
    @RequirePermission("system:role:assign")
    public void assignUserRoles(@RequestBody UserRoleDTO dto) {
        permissionService.assignUserRoles(dto.getUserId(), dto.getRoleIds());
    }

    @Operation(summary = "清除用户角色")
    @DeleteMapping("/user/{userId}/roles")
    @RequirePermission("system:role:clear")
    public void clearUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        permissionService.clearUserRoles(userId);
    }

    // ==================== 角色权限管理 ====================

    @Operation(summary = "获取角色权限ID列表")
    @GetMapping("/role/{roleId}/permissions")
    @RequirePermission("system:permission:view")
    public List<Long> getRolePermissions(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        return permissionService.getRolePermissionIds(roleId);
    }

    @Operation(summary = "分配角色权限")
    @PostMapping("/role/assign-permissions")
    @RequirePermission("system:permission:assign")
    public void assignRolePermissions(@RequestBody RoleDTO dto) {
        permissionService.assignRolePermissions(dto.getRoleId(), dto.getPermissionIds());
    }

    @Operation(summary = "清除角色权限")
    @DeleteMapping("/role/{roleId}/permissions")
    @RequirePermission("system:permission:clear")
    public void clearRolePermissions(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        permissionService.clearRolePermissions(roleId);
    }

    // ==================== 权限验证 ====================

    @Operation(summary = "验证API访问权限")
    @GetMapping("/check/api")
    @RequirePermission("system:permission:check")
    public boolean checkApiPermission(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "API路径") @RequestParam String apiPath,
            @Parameter(description = "HTTP方法") @RequestParam String method) {
        return permissionService.checkApiPermission(userId, apiPath, method);
    }

    @Operation(summary = "验证数据访问权限")
    @GetMapping("/check/data")
    @RequirePermission("system:permission:check")
    public boolean checkDataPermission(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "数据租户ID") @RequestParam(required = false) Long dataTenantId,
            @Parameter(description = "数据创建人") @RequestParam(required = false) Long dataCreateBy) {
        return permissionService.checkDataPermission(userId, dataTenantId, dataCreateBy);
    }

    @Operation(summary = "验证租户访问权限")
    @GetMapping("/check/tenant")
    @RequirePermission("system:permission:check")
    public boolean checkTenantPermission(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "租户ID") @RequestParam Long tenantId) {
        return permissionService.checkTenantPermission(userId, tenantId);
    }

    // ==================== 缓存管理 ====================

    @Operation(summary = "刷新用户权限缓存")
    @PostMapping("/cache/refresh/{userId}")
    @RequirePermission("system:permission:cache")
    public void refreshUserPermissionCache(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        permissionService.refreshUserPermissionCache(userId);
    }

    @Operation(summary = "清除所有权限缓存")
    @DeleteMapping("/cache/clear")
    @RequirePermission("system:permission:cache")
    public void clearPermissionCache() {
        permissionService.clearPermissionCache();
    }
}
