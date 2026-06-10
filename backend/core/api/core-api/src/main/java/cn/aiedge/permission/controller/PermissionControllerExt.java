package cn.aiedge.permission.controller;

import cn.aiedge.base.service.PermissionService;
import cn.aiedge.common.dto.permission.*;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@Tag(name = "权限管理", description = "权限增删改查接口")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionControllerExt {

    private final PermissionService permissionService;

    @Operation(summary = "分页查询权限")
    @GetMapping("/page")
    @SaCheckPermission("system:permission:list")
    public ApiResponse<PageResult<PermissionDetailVO>> pageList(PermissionQueryRequest request) {
        return ApiResponse.ok(permissionService.pageList(request));
    }

    @Operation(summary = "获取权限树形结构")
    @GetMapping("/tree")
    @SaCheckPermission("system:permission:list")
    public ApiResponse<List<PermissionDetailVO>> getTree() {
        return ApiResponse.ok(permissionService.getTree());
    }

    @Operation(summary = "获取用户菜单权限树")
    @GetMapping("/menu-tree")
    @SaCheckPermission("system:permission:list")
    public ApiResponse<List<PermissionDetailVO>> getUserMenuTree(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        return ApiResponse.ok(permissionService.getUserMenuTree(userId));
    }

    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    @SaCheckPermission("system:permission:detail")
    public ApiResponse<PermissionDetailVO> getDetail(
            @Parameter(description = "权限ID") @PathVariable Long id) {
        return ApiResponse.ok(permissionService.getDetail(id));
    }

    @Operation(summary = "创建权限")
    @PostMapping
    @SaCheckPermission("system:permission:create")
    public ApiResponse<Long> create(@Valid @RequestBody PermissionCreateRequest request) {
        return ApiResponse.okWithId("创建成功", permissionService.create(request));
    }

    @Operation(summary = "更新权限")
    @PutMapping
    @SaCheckPermission("system:permission:update")
    public ApiResponse<Void> update(@Valid @RequestBody PermissionUpdateRequest request) {
        permissionService.update(request);
        return ApiResponse.ok("更新成功");
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:permission:delete")
    public ApiResponse<Void> delete(
            @Parameter(description = "权限ID") @PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.ok("删除成功");
    }

    @Operation(summary = "批量删除权限")
    @DeleteMapping("/batch")
    @SaCheckPermission("system:permission:delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        permissionService.batchDelete(ids);
        return ApiResponse.ok("批量删除成功");
    }

    @Operation(summary = "导出权限")
    @GetMapping("/export")
    @SaCheckPermission("system:permission:export")
    public ApiResponse<List<cn.aiedge.base.entity.Permission>> export() {
        return ApiResponse.ok(permissionService.list());
    }

    @Operation(summary = "启用/禁用权限")
    @PutMapping("/{id}/status")
    @SaCheckPermission("system:permission:update")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "权限ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        permissionService.updateStatus(id, status);
        return ApiResponse.ok("状态更新成功");
    }
}
