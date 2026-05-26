package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.service.SysRoleService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "创建角色")
    @PostMapping
    @SaCheckPermission("role:create")
    public Result<Long> createRole(@RequestBody SysRole role) {
        Long roleId = roleService.createRole(role);
        return Result.ok("创建成功", roleId);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @SaCheckPermission("role:update")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateRole(role);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @SaCheckPermission("role:delete")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "分配权限")
    @PostMapping("/{id}/permissions")
    @SaCheckPermission("role:assign-permission")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.ok("分配成功", null);
    }

    @Operation(summary = "分配菜单")
    @PostMapping("/{id}/menus")
    @SaCheckPermission("role:assign-menu")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.ok("分配成功", null);
    }

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @SaCheckPermission("role:list")
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

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    @SaCheckPermission("role:detail")
    public Result<SysRole> getRoleDetail(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        return Result.ok(role);
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    @SaCheckPermission("role:detail")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissionIds(id);
        return Result.ok(permissionIds);
    }

    @Operation(summary = "更新角色状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("role:update-status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        roleService.updateRoleStatus(id, status);
        return Result.ok("状态更新成功", null);
    }
}