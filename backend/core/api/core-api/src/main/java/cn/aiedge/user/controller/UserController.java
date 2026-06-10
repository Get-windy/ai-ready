package cn.aiedge.user.controller;

import cn.aiedge.base.service.UserService;
import cn.aiedge.common.dto.user.*;
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
 * 用户管理控制器
 */
@Tag(name = "用户管理V2", description = "用户增删改查接口(新版)")
@RestController
@RequestMapping("/api/v2/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @SaCheckPermission("system:user:list")
    @GetMapping("/page")
    public ApiResponse<PageResult<UserVO>> pageList(UserQueryRequest request) {
        return ApiResponse.ok(userService.pageList(request));
    }

    @Operation(summary = "获取用户详情")
    @SaCheckPermission("system:user:detail")
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getDetail(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        return ApiResponse.ok(userService.getDetail(id));
    }

    @Operation(summary = "创建用户")
    @SaCheckPermission("system:user:create")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody UserCreateRequest request) {
        Long userId = userService.create(request);
        return ApiResponse.ok(userId, "创建成功");
    }

    @Operation(summary = "更新用户")
    @SaCheckPermission("system:user:update")
    @PutMapping
    public ApiResponse<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        userService.update(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除用户")
    @SaCheckPermission("system:user:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除用户")
    @SaCheckPermission("system:user:delete")
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        userService.batchDelete(ids);
        return ApiResponse.ok();
    }

    @Operation(summary = "修改密码")
    @SaCheckPermission("system:user:update")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> changePassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "原密码") @RequestParam String oldPassword,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        userService.changePassword(id, oldPassword, newPassword);
        return ApiResponse.ok();
    }

    @Operation(summary = "重置密码")
    @SaCheckPermission("system:user:update")
    @PutMapping("/{id}/password/reset")
    public ApiResponse<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return ApiResponse.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @SaCheckPermission("system:user:update")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @Operation(summary = "导出用户")
    @SaCheckPermission("system:user:export")
    @GetMapping("/export")
    public ApiResponse<List<cn.aiedge.base.entity.User>> export() {
        return ApiResponse.ok(userService.list());
    }

    @Operation(summary = "分配角色")
    @SaCheckPermission("system:role:assign")
    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return ApiResponse.ok();
    }
}
