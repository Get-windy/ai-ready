package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.service.SysMenuService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "菜单管理", description = "菜单CRUD接口")
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /**
     * 创建菜单
     */
    @Operation(summary = "创建菜单")
    @PostMapping
    @SaCheckPermission("menu:create")
    public Result<Long> createMenu(@RequestBody SysMenu menu) {
        Long menuId = menuService.createMenu(menu);
        return Result.ok("创建成功", menuId);
    }

    /**
     * 更新菜单
     */
    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    @SaCheckPermission("menu:update")
    public Result<Void> updateMenu(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.updateMenu(menu);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除菜单
     */
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("menu:delete")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 获取菜单树
     */
    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    @SaCheckPermission("menu:list")
    public Result<List<SysMenu>> getMenuTree(@RequestParam Long tenantId) {
        List<SysMenu> tree = menuService.getMenuTree(tenantId);
        return Result.ok(tree);
    }

    /**
     * 获取用户菜单树
     */
    @Operation(summary = "获取用户菜单树")
    @GetMapping("/user/tree")
    @SaCheckLogin
    public Result<List<SysMenu>> getUserMenuTree(@RequestParam Long userId) {
        List<SysMenu> tree = menuService.getUserMenuTree(userId);
        return Result.ok(tree);
    }

    /**
     * 获取子菜单列表
     */
    @Operation(summary = "获取子菜单列表")
    @GetMapping("/children/{parentId}")
    @SaCheckPermission("menu:list")
    public Result<List<SysMenu>> getChildrenMenus(
            @PathVariable Long parentId,
            @RequestParam Long tenantId) {
        List<SysMenu> children = menuService.getChildrenMenus(parentId, tenantId);
        return Result.ok(children);
    }

    /**
     * 获取菜单详情
     */
    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("menu:detail")
    public Result<SysMenu> getMenuDetail(@PathVariable Long id) {
        SysMenu menu = menuService.getMenuDetail(id);
        return Result.ok(menu);
    }

    /**
     * 获取所有菜单列表
     */
    @Operation(summary = "获取所有菜单列表")
    @GetMapping("/list")
    @SaCheckPermission("menu:list")
    public Result<List<SysMenu>> listAllMenus(@RequestParam Long tenantId) {
        List<SysMenu> menus = menuService.listAllMenus(tenantId);
        return Result.ok(menus);
    }

    /**
     * 更新菜单排序
     */
    @Operation(summary = "更新菜单排序")
    @PutMapping("/{id}/sort")
    @SaCheckPermission("menu:update")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sort) {
        menuService.updateMenuSort(id, sort);
        return Result.ok("排序更新成功", null);
    }

    /**
     * 更新菜单状态
     */
    @Operation(summary = "更新菜单状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("menu:update-status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        menuService.updateMenuStatus(id, status);
        return Result.ok("状态更新成功", null);
    }

    /**
     * 检查菜单编码是否存在
     */
    @Operation(summary = "检查菜单编码是否存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkMenuCodeExists(
            @RequestParam String menuCode,
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = menuService.checkMenuCodeExists(menuCode, tenantId, excludeId);
        return Result.ok(exists);
    }
}