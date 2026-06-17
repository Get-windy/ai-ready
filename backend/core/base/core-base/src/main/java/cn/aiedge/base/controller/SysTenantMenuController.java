package cn.aiedge.base.controller;

import cn.aiedge.base.service.SysTenantMenuService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    /**
     * 查询租户已授权的菜单ID列表
     */
    @Operation(summary = "查询租户授权菜单ID列表")
    @GetMapping("/{tenantId}")
    @SaCheckPermission("tenant:menu:query")
    public Result<Set<Long>> getAuthorizedMenuIds(@PathVariable Long tenantId) {
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
        tenantMenuService.assignMenus(tenantId, menuIds);
        return Result.ok("授权成功", null);
    }

    /**
     * 批量移除租户授权菜单
     */
    @Operation(summary = "批量移除租户授权菜单")
    @DeleteMapping("/{tenantId}/remove")
    @SaCheckPermission("tenant:menu:remove")
    public Result<Void> removeMenus(@PathVariable Long tenantId, @RequestBody List<Long> menuIds) {
        tenantMenuService.removeMenus(tenantId, menuIds);
        return Result.ok("移除成功", null);
    }

    /**
     * 清除租户所有菜单授权
     */
    @Operation(summary = "清除租户所有菜单授权")
    @DeleteMapping("/{tenantId}")
    @SaCheckPermission("tenant:menu:remove")
    public Result<Void> clearByTenantId(@PathVariable Long tenantId) {
        tenantMenuService.clearByTenantId(tenantId);
        return Result.ok("清除成功", null);
    }
}
