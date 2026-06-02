package com.aiready.menu.controller;

import com.aiready.menu.dto.*;
import com.aiready.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 菜单控制器
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "菜单管理接口")
public class MenuController {

    private final MenuService menuService;

    /**
     * 创建菜单
     */
    @PostMapping("/create")
    @Operation(summary = "创建菜单")
    public MenuDTO createMenu(@Valid @RequestBody MenuSaveRequest request,
                              @RequestParam Long operatorId) {
        return menuService.createMenu(request, operatorId);
    }

    /**
     * 更新菜单
     */
    @PutMapping("/update")
    @Operation(summary = "更新菜单")
    public MenuDTO updateMenu(@Valid @RequestBody MenuUpdateRequest request,
                              @RequestParam Long operatorId) {
        return menuService.updateMenu(request, operatorId);
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/delete/{menuId}")
    @Operation(summary = "删除菜单")
    public void deleteMenu(@PathVariable Long menuId, @RequestParam Long operatorId) {
        menuService.deleteMenu(menuId, operatorId);
    }

    /**
     * 批量删除菜单
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除菜单")
    public void batchDeleteMenus(@RequestBody List<Long> menuIds, @RequestParam Long operatorId) {
        menuService.batchDeleteMenus(menuIds, operatorId);
    }

    /**
     * 获取菜单详情
     */
    @GetMapping("/detail/{menuId}")
    @Operation(summary = "获取菜单详情")
    public MenuDTO getMenuById(@PathVariable Long menuId) {
        return menuService.getMenuById(menuId);
    }

    /**
     * 获取菜单树
     */
    @PostMapping("/tree")
    @Operation(summary = "获取菜单树")
    public List<MenuDTO> getMenuTree(@RequestBody MenuQueryRequest request) {
        return menuService.getMenuTree(request);
    }

    /**
     * 获取所有菜单
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有菜单")
    public List<MenuDTO> getAllMenus() {
        return menuService.getAllMenus();
    }

    /**
     * 获取用户菜单
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户菜单")
    public List<MenuDTO> getUserMenus(@PathVariable Long userId) {
        return menuService.getUserMenus(userId);
    }

    /**
     * 获取用户客户端菜单（前端动态路由使用）
     */
    @GetMapping("/user/client/{clientType}")
    @Operation(summary = "获取用户客户端菜单")
    public List<MenuDTO> getUserClientMenus(
            @PathVariable String clientType,
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return menuService.getUserClientMenus(userId, clientType, tenantId);
    }

    /**
     * 获取角色菜单
     */
    @GetMapping("/role/{roleId}")
    @Operation(summary = "获取角色菜单")
    public List<MenuDTO> getRoleMenus(@PathVariable Long roleId) {
        return menuService.getRoleMenus(roleId);
    }

    /**
     * 分配角色菜单
     */
    @PostMapping("/assign-role-menus/{roleId}")
    @Operation(summary = "分配角色菜单")
    public void assignRoleMenus(@PathVariable Long roleId,
                                @RequestBody List<Long> menuIds,
                                @RequestParam Long operatorId) {
        menuService.assignRoleMenus(roleId, menuIds, operatorId);
    }

    /**
     * 更新菜单状态
     */
    @PostMapping("/update-status/{menuId}")
    @Operation(summary = "更新菜单状态")
    public void updateStatus(@PathVariable Long menuId,
                             @RequestParam Integer status,
                             @RequestParam Long operatorId) {
        menuService.updateStatus(menuId, status, operatorId);
    }

    /**
     * 移动菜单
     */
    @PostMapping("/move/{menuId}")
    @Operation(summary = "移动菜单")
    public MenuDTO moveMenu(@PathVariable Long menuId,
                            @RequestParam Long newParentId,
                            @RequestParam Long operatorId) {
        return menuService.moveMenu(menuId, newParentId, operatorId);
    }

    /**
     * 更新菜单排序
     */
    @PostMapping("/update-sort")
    @Operation(summary = "更新菜单排序")
    public void updateSortOrder(@RequestBody List<Long> menuIds,
                                @RequestParam Long operatorId) {
        menuService.updateSortOrder(menuIds, operatorId);
    }

    /**
     * 验证菜单编码
     */
    @GetMapping("/validate-code")
    @Operation(summary = "验证菜单编码")
    public boolean validateMenuCode(@RequestParam String menuCode,
                                    @RequestParam(required = false) Long excludeId) {
        return menuService.validateMenuCode(menuCode, excludeId);
    }
}