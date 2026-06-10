package cn.aiedge.role.controller;

import cn.aiedge.base.service.RoleService;
import cn.aiedge.common.dto.role.*;
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
 * 角色管理控制器
 */
@Tag(name = "角色管理", description = "角色增删改查接口")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @SaCheckPermission("system:role:list")
    @GetMapping("/page")
    public ApiResponse<PageResult<RoleDetailVO>> pageList(RoleQueryRequest request) {
        return ApiResponse.ok(roleService.pageList(request));
    }

    @Operation(summary = "获取所有角色列表")
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public ApiResponse<List<RoleDetailVO>> listAll(
            @Parameter(description = "角色作用域过滤（PLATFORM/TENANT）") @RequestParam(required = false) String scope) {
        return ApiResponse.ok(roleService.listAll(scope));
    }

    @Operation(summary = "获取角色详情")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}")
    public ApiResponse<RoleDetailVO> getDetail(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        return ApiResponse.ok(roleService.getDetail(id));
    }

    @Operation(summary = "创建角色")
    @SaCheckPermission("system:role:create")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        Long roleId = roleService.create(request);
        return ApiResponse.ok(roleId, "创建成功");
    }

    @Operation(summary = "更新角色")
    @SaCheckPermission("system:role:update")
    @PutMapping
    public ApiResponse<Void> update(@Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除角色")
    @SaCheckPermission("system:role:delete")
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        roleService.batchDelete(ids);
        return ApiResponse.ok();
    }

    @Operation(summary = "导出角色")
    @SaCheckPermission("system:role:export")
    @GetMapping("/export")
    public ApiResponse<List<cn.aiedge.base.entity.Role>> export() {
        return ApiResponse.ok(roleService.list());
    }

    @Operation(summary = "启用/禁用角色")
    @SaCheckPermission("system:role:update")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        roleService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @Operation(summary = "分配权限")
    @SaCheckPermission("system:permission:assign")
    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return ApiResponse.ok();
    }
}
