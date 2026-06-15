package cn.aiedge.base.controller;

import cn.aiedge.base.dto.BatchAssignRolesRequest;
import cn.aiedge.base.dto.UserCreateRequest;
import cn.aiedge.base.dto.UserDTO;
import cn.aiedge.base.dto.UserLoginRequest;
import cn.aiedge.base.dto.UserUpdateRequest;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "用户管理", description = "用户CRUD接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid UserLoginRequest dto,
                                HttpServletRequest request) {
        String loginIp = getClientIp(request);
        String token = userService.login(dto.getUsername(), dto.getPassword(), dto.getTenantId(), loginIp);
        return Result.ok("登录成功", token);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        userService.logout();
        return Result.ok("登出成功", null);
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户")
    @PostMapping
    @SaCheckPermission("user:create")
    @OperationLog(module = "用户管理", type = "CREATE", desc = "创建用户")
    public Result<Long> createUser(@RequestBody @Valid UserCreateRequest dto) {
        SysUser user = convertToEntity(dto);
        Long userId = userService.createUser(user);
        return Result.ok("创建成功", userId);
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @SaCheckPermission("user:update")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest dto) {
        SysUser user = convertToEntity(dto);
        user.setId(id);
        userService.updateUser(user);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @SaCheckPermission("user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 批量删除用户
     */
    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @SaCheckPermission("user:delete")
    public Result<Void> batchDeleteUsers(@RequestBody List<Long> ids) {
        userService.batchDeleteUsers(ids);
        return Result.ok("批量删除成功", null);
    }

    /**
     * 分页查询用户
     */
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @SaCheckPermission("user:list")
    public Result<Page<SysUser>> pageUsers(UserDTO.Query query) {
        int pageNum = query.pageNum() != null ? query.pageNum() : 1;
        int pageSize = query.pageSize() != null ? query.pageSize() : 10;
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> result = userService.pageUsers(page, query.tenantId(),
                query.username(), query.status(), query.deptId());
        return Result.ok(result);
    }

    /**
     * 获取用户列表（无分页，供下拉选择器等场景使用）
     */
    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    @SaCheckPermission("user:list")
    public Result<List<SysUser>> listUsers(UserDTO.Query query) {
        Page<SysUser> page = new Page<>(1, query.pageSize() != null ? query.pageSize() : 1000);
        Page<SysUser> result = userService.pageUsers(page, query.tenantId(),
                query.username(), query.status(), query.deptId());
        return Result.ok(result.getRecords());
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @SaCheckPermission("user:detail")
    public Result<SysUser> getUserDetail(@PathVariable Long id) {
        SysUser user = userService.getUserDetail(id);
        return Result.ok(user);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password/reset")
    @SaCheckPermission("user:reset-password")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "重置用户密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.ok("密码重置成功", null);
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PutMapping("/{id}/password/change")
    @SaCheckLogin
    public Result<Void> changePassword(@PathVariable Long id,
                                        @RequestParam String oldPassword,
                                        @RequestParam String newPassword) {
        userService.changePassword(id, oldPassword, newPassword);
        return Result.ok("密码修改成功", null);
    }

    /**
     * 分配角色
     */
    @Operation(summary = "分配角色")
    @PostMapping("/{id}/roles")
    @SaCheckPermission("user:assign-role")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "分配用户角色", saveParams = true)
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.ok("角色分配成功", null);
    }

    /**
     * 批量分配角色
     */
    @Operation(summary = "批量分配角色")
    @PostMapping("/batch-assign-roles")
    @SaCheckPermission("user:assign-role")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "批量分配角色", saveParams = true)
    public Result<Void> batchAssignRoles(@RequestBody @Valid BatchAssignRolesRequest dto) {
        userService.batchAssignRoles(dto.getUserIds(), dto.getRoleIds());
        return Result.ok("批量角色分配成功", null);
    }

    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("user:update-status")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "更新用户状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.ok("状态更新成功", null);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private SysUser convertToEntity(UserCreateRequest dto) {
        return new SysUser()
                .setTenantId(dto.getTenantId())
                .setUsername(dto.getUsername())
                .setPassword(dto.getPassword())
                .setNickname(dto.getNickname())
                .setEmail(dto.getEmail())
                .setPhone(dto.getPhone())
                .setAvatar(dto.getAvatar())
                .setGender(dto.getGender())
                .setUserType(dto.getUserType())
                .setDeptId(dto.getDeptId())
                .setPostId(dto.getPostId());
    }

    private SysUser convertToEntity(UserUpdateRequest dto) {
        return new SysUser()
                .setNickname(dto.getNickname())
                .setEmail(dto.getEmail())
                .setPhone(dto.getPhone())
                .setAvatar(dto.getAvatar())
                .setGender(dto.getGender())
                .setDeptId(dto.getDeptId())
                .setPostId(dto.getPostId());
    }
}