package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.event.PermissionChangeEvent;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.SysPermissionService;
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
 * 权限控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "权限管理", description = "权限CRUD接口")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建权限
     */
    @Operation(summary = "创建权限")
    @PostMapping
    @SaCheckPermission("system:permission:create")
    @OperationLog(module = "权限管理", type = "CREATE", desc = "创建权限")
    public Result<Long> createPermission(@RequestBody SysPermission permission) {
        Long permissionId = permissionService.createPermission(permission);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.PERMISSION_CREATED, permission.getTenantId(), "创建权限: " + permission.getPermissionName()));
        return Result.ok("创建成功", permissionId);
    }

    /**
     * 更新权限
     */
    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    @SaCheckPermission("system:permission:update")
    @OperationLog(module = "权限管理", type = "UPDATE", desc = "更新权限")
    public Result<Void> updatePermission(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setId(id);
        permissionService.updatePermission(permission);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.PERMISSION_UPDATED, permission.getTenantId(), "更新权限: id=" + id));
        return Result.ok("更新成功", null);
    }

    /**
     * 删除权限
     */
    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:permission:delete")
    @OperationLog(module = "权限管理", type = "DELETE", desc = "删除权限")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.PERMISSION_DELETED, null, "删除权限: id=" + id));
        return Result.ok("删除成功", null);
    }

    /**
     * 批量删除权限
     */
    @Operation(summary = "批量删除权限")
    @DeleteMapping("/batch")
    @SaCheckPermission("system:permission:delete")
    @OperationLog(module = "权限管理", type = "DELETE", desc = "批量删除权限")
    public Result<Void> batchDeletePermissions(@RequestBody List<Long> ids) {
        permissionService.batchDeletePermissions(ids);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.PERMISSION_DELETED, null, "批量删除权限: ids=" + ids));
        return Result.ok("批量删除成功", null);
    }

    /**
     * 分页查询权限
     */
    @Operation(summary = "分页查询权限")
    @GetMapping("/page")
    @SaCheckPermission("system:permission:list")
    public Result<Page<SysPermission>> pagePermissions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String permissionName,
            @RequestParam(required = false) Integer permissionType,
            @RequestParam(required = false) Integer status) {
        Page<SysPermission> page = new Page<>(current, size);
        Page<SysPermission> result = permissionService.pagePermissions(page, tenantId, permissionName, permissionType, status);
        return Result.ok(result);
    }

    /**
     * 获取权限详情
     */
    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    @SaCheckPermission("system:permission:detail")
    public Result<SysPermission> getPermissionDetail(@PathVariable Long id) {
        SysPermission permission = permissionService.getPermissionDetail(id);
        return Result.ok(permission);
    }

    /**
     * 获取权限树
     */
    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    @SaCheckPermission("system:permission:list")
    public Result<List<SysPermission>> getPermissionTree(@RequestParam Long tenantId) {
        List<SysPermission> tree = permissionService.getPermissionTree(tenantId);
        return Result.ok(tree);
    }

    /**
     * 获取子权限列表
     */
    @Operation(summary = "获取子权限列表")
    @GetMapping("/children/{parentId}")
    @SaCheckPermission("system:permission:list")
    public Result<List<SysPermission>> getChildrenPermissions(
            @PathVariable Long parentId,
            @RequestParam Long tenantId) {
        List<SysPermission> children = permissionService.getChildrenPermissions(parentId, tenantId);
        return Result.ok(children);
    }

    /**
     * 更新权限状态
     */
    @Operation(summary = "更新权限状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("system:permission:update-status")
    @OperationLog(module = "权限管理", type = "UPDATE", desc = "更新权限状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        permissionService.updatePermissionStatus(id, status);
        return Result.ok("状态更新成功", null);
    }

    /**
     * 更新权限排序
     */
    @Operation(summary = "更新权限排序")
    @PutMapping("/{id}/sort")
    @SaCheckPermission("system:permission:update")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sort) {
        permissionService.updatePermissionSort(id, sort);
        return Result.ok("排序更新成功", null);
    }

    /**
     * 检查权限编码是否存在
     */
    @Operation(summary = "检查权限编码是否存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkPermissionCodeExists(
            @RequestParam String permissionCode,
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = permissionService.checkPermissionCodeExists(permissionCode, tenantId, excludeId);
        return Result.ok(exists);
    }
}