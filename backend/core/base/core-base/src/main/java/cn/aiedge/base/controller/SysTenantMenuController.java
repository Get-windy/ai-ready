package cn.aiedge.base.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.event.PermissionChangeEvent;
import cn.aiedge.base.service.SysTenantMenuService;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 租户菜单授权控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "租户菜单授权", description = "系统管理员授权菜单给租户")
@RestController
@RequestMapping("/api/tenant-menu")
@RequiredArgsConstructor
public class SysTenantMenuController {

    private final SysTenantMenuService tenantMenuService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 平台管理员硬校验。
     *
     * <p>本控制器所有端点都作用于「指定 tenantId 的菜单授权」，而 {@code sys_tenant_menu} 在
     * 多租户忽略清单里（写入走标量 {@code #{tenantId}}），**没有兜底过滤**。</p>
     *
     * <p>只靠 {@code tenant:menu:*} 权限码不够：权限码可被授予租户角色
     * （`sys_permission` / `sys_role_permission` 均为全局表、{@code assignPermissions} 不做归属校验），
     * 租户管理员一旦拿到该码即可改写**任意租户**（含平台租户）的菜单授权，形成完整提权链（2026-09-20）。</p>
     */
    private void assertPlatformAdmin() {
        if (!MyBatisPlusConfig.isTenantScopeExempt()) {
            throw BusinessException.forbidden("仅平台管理员可操作租户菜单授权");
        }
    }

    /**
     * 查询租户已授权的菜单ID列表
     */
    @Operation(summary = "查询租户授权菜单ID列表")
    @GetMapping("/{tenantId}")
    @SaCheckPermission("tenant:menu:query")
    public Result<Set<Long>> getAuthorizedMenuIds(@PathVariable Long tenantId) {
        assertPlatformAdmin();
        Set<Long> menuIds = tenantMenuService.getAuthorizedMenuIds(tenantId);
        return Result.ok(menuIds);
    }

    /**
     * 批量设置租户授权菜单（全量覆盖）
     */
    @Operation(summary = "批量设置租户授权菜单")
    @PutMapping("/{tenantId}")
    @SaCheckPermission("tenant:menu:assign")
    public Result<Void> assignMenus(@PathVariable Long tenantId, @RequestBody List<Long> menuIds) {
        assertPlatformAdmin();
        tenantMenuService.assignMenus(tenantId, menuIds);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.TENANT_MENU_ASSIGNED, tenantId, "租户菜单授权: tenantId=" + tenantId));
        return Result.ok("授权成功", null);
    }

    /**
     * 批量移除租户授权菜单
     */
    @Operation(summary = "批量移除租户授权菜单")
    @DeleteMapping("/{tenantId}/remove")
    @SaCheckPermission("tenant:menu:remove")
    public Result<Void> removeMenus(@PathVariable Long tenantId, @RequestBody List<Long> menuIds) {
        assertPlatformAdmin();
        tenantMenuService.removeMenus(tenantId, menuIds);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.TENANT_MENU_ASSIGNED, tenantId, "租户菜单移除: tenantId=" + tenantId));
        return Result.ok("移除成功", null);
    }

    /**
     * 清除租户所有菜单授权
     */
    @Operation(summary = "清除租户所有菜单授权")
    @DeleteMapping("/{tenantId}")
    @SaCheckPermission("tenant:menu:remove")
    public Result<Void> clearByTenantId(@PathVariable Long tenantId) {
        assertPlatformAdmin();
        tenantMenuService.clearByTenantId(tenantId);
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.TENANT_MENU_ASSIGNED, tenantId, "清空租户菜单: tenantId=" + tenantId));
        return Result.ok("清除成功", null);
    }
}
