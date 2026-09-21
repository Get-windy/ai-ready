package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.event.PermissionChangeEvent;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.SysRoleService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "角色管理", description = "角色CRUD接口")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "创建角色")
    @PostMapping
    @SaCheckPermission("tenant-admin:role:create")
    @OperationLog(module = "角色管理", type = "CREATE", desc = "创建角色")
    public Result<Long> createRole(@RequestBody SysRole role) {
        Long roleId = roleService.createRole(role);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.ROLE_CREATED, role.getTenantId(), "创建角色: " + role.getRoleName()));
        return Result.ok("创建成功", roleId);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @SaCheckPermission("tenant-admin:role:update")
    @OperationLog(module = "角色管理", type = "UPDATE", desc = "更新角色")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateRole(role);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.ROLE_UPDATED, role.getTenantId(), "更新角色: id=" + id));
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @SaCheckPermission("tenant-admin:role:delete")
    @OperationLog(module = "角色管理", type = "DELETE", desc = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.ROLE_DELETED, null, "删除角色: id=" + id));
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "分配权限")
    @PostMapping("/{id}/permissions")
    @SaCheckPermission("tenant-admin:role:assign-permission")
    @OperationLog(module = "权限管理", type = "UPDATE", desc = "分配角色权限", saveParams = true)
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.ROLE_PERMISSION_ASSIGNED, null, "分配角色权限: roleId=" + id));
        return Result.ok("分配成功", null);
    }

    @Operation(summary = "分配菜单")
    @PostMapping("/{id}/menus")
    @SaCheckPermission("tenant-admin:role:assign-menu")
    @OperationLog(module = "权限管理", type = "UPDATE", desc = "分配角色菜单", saveParams = true)
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.ROLE_MENU_ASSIGNED, null, "分配角色菜单: roleId=" + id));
        return Result.ok("分配成功", null);
    }

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @SaCheckPermission("tenant-admin:role:list")
    public Result<Page<SysRole>> pageRoles(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Integer status) {
        Page<SysRole> page = new Page<>(current, size);
        Page<SysRole> result = roleService.pageRoles(page, tenantId, roleName, status);
        return Result.ok(result);
    }

    @Operation(summary = "获取所有角色列表")
    @GetMapping("/list")
    public Result<List<SysRole>> listAll() {
        return Result.ok(roleService.list());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<SysRole> getRoleDetail(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        return Result.ok(role);
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissionIds(id);
        return Result.ok(permissionIds);
    }

    @Operation(summary = "获取角色菜单")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> getRoleMenus(@PathVariable Long id) {
        // 与 POST /{id}/menus（assignMenus）配对：前端「菜单管理 → 角色」弹窗
        // 需先回显该角色已分配的菜单，此前只实现了写、漏了读 → 前端 getMenus() 恒 404。
        return Result.ok(roleService.getRoleMenuIds(id));
    }

    @Operation(summary = "更新角色状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("tenant-admin:role:update-status")
    @OperationLog(module = "角色管理", type = "UPDATE", desc = "更新角色状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        roleService.updateRoleStatus(id, status);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.ROLE_UPDATED, null, "更新角色状态: id=" + id));
        return Result.ok("状态更新成功", null);
    }

    @Operation(summary = "复制角色权限", description = "从源角色复制权限和菜单配置到目标角色")
    @PostMapping("/{id}/copy-from/{sourceRoleId}")
    @SaCheckPermission("tenant-admin:role:assign-permission")
    @OperationLog(module = "权限管理", type = "UPDATE", desc = "复制角色权限")
    public Result<Void> copyPermissions(@PathVariable Long id, @PathVariable Long sourceRoleId) {
        roleService.copyPermissions(id, sourceRoleId);
        return Result.ok("复制成功", null);
    }
}