package cn.aiedge.base.controller;

import cn.aiedge.base.service.RoleInheritanceService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色继承关系控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "角色继承管理", description = "角色继承关系管理接口")
@RestController
@RequestMapping("/api/role-inheritance")
@RequiredArgsConstructor
public class RoleInheritanceController {

    private final RoleInheritanceService roleInheritanceService;

    /**
     * 设置角色继承关系
     */
    @Operation(summary = "设置角色继承关系")
    @PostMapping
    @SaCheckPermission("role-inheritance:manage")
    public Result<Void> setRoleInheritance(
            @RequestParam Long parentRoleId,
            @RequestParam Long childRoleId,
            @RequestParam(required = false) Integer inheritanceType) {
        roleInheritanceService.setRoleInheritance(parentRoleId, childRoleId, inheritanceType);
        return Result.ok("设置成功", null);
    }

    /**
     * 批量设置角色继承关系
     */
    @Operation(summary = "批量设置角色继承关系")
    @PostMapping("/batch")
    @SaCheckPermission("role-inheritance:manage")
    public Result<Void> batchSetRoleInheritance(
            @RequestParam Long parentRoleId,
            @RequestBody List<Long> childRoleIds,
            @RequestParam(required = false) Integer inheritanceType) {
        roleInheritanceService.batchSetRoleInheritance(parentRoleId, childRoleIds, inheritanceType);
        return Result.ok("批量设置成功", null);
    }

    /**
     * 删除角色继承关系
     */
    @Operation(summary = "删除角色继承关系")
    @DeleteMapping
    @SaCheckPermission("role-inheritance:manage")
    public Result<Void> removeRoleInheritance(
            @RequestParam Long parentRoleId,
            @RequestParam Long childRoleId) {
        roleInheritanceService.removeRoleInheritance(parentRoleId, childRoleId);
        return Result.ok("删除成功", null);
    }

    /**
     * 获取角色的所有父角色
     */
    @Operation(summary = "获取角色的所有父角色")
    @GetMapping("/parents/{childRoleId}")
    @SaCheckPermission("role-inheritance:view")
    public Result<List<Long>> getParentRoleIds(@PathVariable Long childRoleId) {
        List<Long> parentRoleIds = roleInheritanceService.getParentRoleIds(childRoleId);
        return Result.ok(parentRoleIds);
    }

    /**
     * 获取角色的所有子角色
     */
    @Operation(summary = "获取角色的所有子角色")
    @GetMapping("/children/{parentRoleId}")
    @SaCheckPermission("role-inheritance:view")
    public Result<List<Long>> getChildRoleIds(@PathVariable Long parentRoleId) {
        List<Long> childRoleIds = roleInheritanceService.getChildRoleIds(parentRoleId);
        return Result.ok(childRoleIds);
    }

    /**
     * 获取角色继承的所有权限（包括继承的权限）
     */
    @Operation(summary = "获取角色继承的所有权限")
    @GetMapping("/permissions/{roleId}")
    @SaCheckPermission("role-inheritance:view")
    public Result<List<Long>> getAllRolePermissionsWithInheritance(@PathVariable Long roleId) {
        List<Long> allPermissions = roleInheritanceService.getAllRolePermissionsWithInheritance(roleId);
        return Result.ok(allPermissions);
    }
}