package cn.aiedge.base.controller;

import cn.aiedge.base.entity.DataPermission;
import cn.aiedge.base.service.DataPermissionService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "数据权限管理", description = "数据权限CRUD及验证接口")
@RestController
@RequestMapping("/api/data-permission")
@RequiredArgsConstructor
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;

    /**
     * 创建数据权限
     */
    @Operation(summary = "创建数据权限")
    @PostMapping
    @SaCheckPermission("data-permission:create")
    public Result<Long> createDataPermission(@RequestBody DataPermission dataPermission) {
        Long permissionId = dataPermissionService.createDataPermission(dataPermission);
        return Result.ok("创建成功", permissionId);
    }

    /**
     * 更新数据权限
     */
    @Operation(summary = "更新数据权限")
    @PutMapping("/{id}")
    @SaCheckPermission("data-permission:update")
    public Result<Void> updateDataPermission(@PathVariable Long id, @RequestBody DataPermission dataPermission) {
        dataPermission.setId(id);
        dataPermissionService.updateDataPermission(dataPermission);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除数据权限
     */
    @Operation(summary = "删除数据权限")
    @DeleteMapping("/{id}")
    @SaCheckPermission("data-permission:delete")
    public Result<Void> deleteDataPermission(@PathVariable Long id) {
        dataPermissionService.deleteDataPermission(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 分页查询数据权限
     */
    @Operation(summary = "分页查询数据权限")
    @GetMapping("/page")
    @SaCheckPermission("data-permission:list")
    public Result<Page<DataPermission>> pageDataPermissions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String permissionName,
            @RequestParam(required = false) Integer scopeType,
            @RequestParam(required = false) Integer status) {
        Page<DataPermission> page = new Page<>(current, size);
        Page<DataPermission> result = dataPermissionService.pageDataPermissions(page, tenantId, 
                permissionName, scopeType, status);
        return Result.ok(result);
    }

    /**
     * 获取用户的数据权限
     */
    @Operation(summary = "获取用户的数据权限")
    @GetMapping("/user/{userId}")
    @SaCheckPermission("data-permission:view")
    public Result<List<DataPermission>> getUserDataPermissions(@PathVariable Long userId) {
        List<DataPermission> permissions = dataPermissionService.getUserDataPermissions(userId);
        return Result.ok(permissions);
    }

    /**
     * 获取角色的数据权限
     */
    @Operation(summary = "获取角色的数据权限")
    @GetMapping("/role/{roleId}")
    @SaCheckPermission("data-permission:view")
    public Result<List<DataPermission>> getRoleDataPermissions(@PathVariable Long roleId) {
        List<DataPermission> permissions = dataPermissionService.getRoleDataPermissions(roleId);
        return Result.ok(permissions);
    }

    /**
     * 检查用户是否有数据访问权限
     */
    @Operation(summary = "检查用户是否有数据访问权限")
    @GetMapping("/check/{userId}")
    @SaCheckPermission("data-permission:check")
    public Result<Boolean> checkUserDataPermission(
            @PathVariable Long userId,
            @RequestParam String resourceType,
            @RequestParam String resourceId,
            @RequestParam String action) {
        boolean hasPermission = dataPermissionService.checkUserDataPermission(userId, resourceType, resourceId, action);
        return Result.ok(hasPermission);
    }

    /**
     * 批量分配数据权限给用户
     */
    @Operation(summary = "批量分配数据权限给用户")
    @PostMapping("/assign-to-user")
    @SaCheckPermission("data-permission:assign")
    public Result<Void> batchAssignDataPermissionToUser(
            @RequestParam Long userId,
            @RequestBody List<Long> dataPermissionIds) {
        dataPermissionService.batchAssignDataPermissionToUser(userId, dataPermissionIds);
        return Result.ok("分配成功", null);
    }

    /**
     * 批量分配数据权限给角色
     */
    @Operation(summary = "批量分配数据权限给角色")
    @PostMapping("/assign-to-role")
    @SaCheckPermission("data-permission:assign")
    public Result<Void> batchAssignDataPermissionToRole(
            @RequestParam Long roleId,
            @RequestBody List<Long> dataPermissionIds) {
        dataPermissionService.batchAssignDataPermissionToRole(roleId, dataPermissionIds);
        return Result.ok("分配成功", null);
    }

    /**
     * 更新数据权限状态
     */
    @Operation(summary = "更新数据权限状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("data-permission:update-status")
    public Result<Void> updateDataPermissionStatus(@PathVariable Long id, @RequestParam Integer status) {
        dataPermissionService.updateDataPermissionStatus(id, status);
        return Result.ok("状态更新成功", null);
    }
}