package cn.aiedge.base.controller;

import cn.aiedge.base.dto.BatchAssignRolesRequest;
import cn.aiedge.base.event.PermissionChangeEvent;
import cn.aiedge.base.dto.UserCreateRequest;
import cn.aiedge.base.dto.UserDTO;
import cn.aiedge.base.dto.UserLoginRequest;
import cn.aiedge.base.dto.UserUpdateRequest;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

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
    private final ApplicationEventPublisher eventPublisher;

    /** 平台超级管理员角色码（与 SysMenuServiceImpl、SysUserServiceImpl 同源，不新造判定） */
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    /**
     * 校验目标账号与调用方属于同一租户（平台超管豁免）。
     *
     * <p>`sys_user` 在多租户忽略表内（登录要跨租户按用户名查账号），`getById` / `updateById` 等
     * **不会**自动带租户条件 —— 所以「按 id 操作某个账号」的越权校验必须在入口显式补，
     * 否则一个租户管理员只要知道 id，就能读 / 改 / 删 / 重置 / 分配角色到别的租户的账号。
     *
     * <p>口径与列表一致：**超管豁免、其余强制限本租户**。
     */
    private void assertSameTenant(Long targetUserId) {
        if (SecurityUtils.hasRole(SUPER_ADMIN_ROLE)) {
            return;
        }
        SysUser target = userService.getById(targetUserId);
        if (target == null) {
            throw BusinessException.notFound("用户不存在");
        }
        Long currentTenantId = SecurityUtils.getCurrentTenantId();
        if (currentTenantId == null || !Objects.equals(target.getTenantId(), currentTenantId)) {
            throw BusinessException.forbidden("无权操作其它租户的账号");
        }
    }

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
    @SaCheckPermission("tenant-admin:user:create")
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
    @SaCheckPermission("tenant-admin:user:update")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest dto) {
        assertSameTenant(id);
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
    @SaCheckPermission("tenant-admin:user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        assertSameTenant(id);
        userService.deleteUser(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 批量删除用户
     */
    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @SaCheckPermission("tenant-admin:user:delete")
    public Result<Void> batchDeleteUsers(@RequestBody List<Long> ids) {
        // 逐个校验归属：批量入口同样不能成为越权的旁路
        if (ids != null) {
            for (Long id : ids) {
                assertSameTenant(id);
            }
        }
        userService.batchDeleteUsers(ids);
        return Result.ok("批量删除成功", null);
    }

    /**
     * 分页查询用户
     */
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @SaCheckPermission("tenant-admin:user:list")
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
    @SaCheckPermission("tenant-admin:user:list")
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
    @SaCheckPermission("tenant-admin:user:detail")
    public Result<SysUser> getUserDetail(@PathVariable Long id) {
        assertSameTenant(id);
        SysUser user = userService.getUserDetail(id);
        return Result.ok(user);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password/reset")
    @SaCheckPermission("tenant-admin:user:reset-password")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "重置用户密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        assertSameTenant(id);
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
        // 改密是自助动作：只允许改**自己**的密码（超管除外）；否则等于给了「知道他人旧密码即可代改」的旁路
        boolean isSelf = Objects.equals(id, SecurityUtils.getCurrentUserId());
        if (!isSelf && !SecurityUtils.hasRole(SUPER_ADMIN_ROLE)) {
            throw BusinessException.forbidden("只能修改本人的密码");
        }
        userService.changePassword(id, oldPassword, newPassword);
        return Result.ok("密码修改成功", null);
    }

    /**
     * 分配角色
     */
    @Operation(summary = "分配角色")
    @PostMapping("/{id}/roles")
    @SaCheckPermission("tenant-admin:user:assign-role")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "分配用户角色", saveParams = true)
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        assertSameTenant(id);
        userService.assignRoles(id, roleIds);
        eventPublisher.publishEvent(PermissionChangeEvent.targeted(this,
                PermissionChangeEvent.ChangeType.USER_ROLE_ASSIGNED,
                java.util.Set.of(id), null, "分配用户角色: userId=" + id));
        return Result.ok("角色分配成功", null);
    }

    /**
     * 批量分配角色
     */
    @Operation(summary = "批量分配角色")
    @PostMapping("/batch-assign-roles")
    @SaCheckPermission("tenant-admin:user:assign-role")
    @OperationLog(module = "用户管理", type = "UPDATE", desc = "批量分配角色", saveParams = true)
    public Result<Void> batchAssignRoles(@RequestBody @Valid BatchAssignRolesRequest dto) {
        userService.batchAssignRoles(dto.getUserIds(), dto.getRoleIds());
        eventPublisher.publishEvent(PermissionChangeEvent.targeted(this,
                PermissionChangeEvent.ChangeType.USER_ROLE_ASSIGNED,
                new java.util.HashSet<>(dto.getUserIds()), null, "批量分配角色: users=" + dto.getUserIds()));
        return Result.ok("批量角色分配成功", null);
    }

    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("tenant-admin:user:update-status")
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