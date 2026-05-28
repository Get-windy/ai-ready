package com.aiready.menu.service;

import com.aiready.menu.dto.*;
import com.aiready.menu.entity.Menu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService extends IService<Menu> {
    
    /**
     * 创建菜单
     */
    MenuDTO createMenu(MenuSaveRequest request, Long operatorId);
    
    /**
     * 更新菜单
     */
    MenuDTO updateMenu(MenuUpdateRequest request, Long operatorId);
    
    /**
     * 删除菜单
     */
    void deleteMenu(Long menuId, Long operatorId);
    
    /**
     * 批量删除菜单
     */
    void batchDeleteMenus(List<Long> menuIds, Long operatorId);
    
    /**
     * 获取菜单详情
     */
    MenuDTO getMenuById(Long menuId);
    
    /**
     * 获取菜单树
     */
    List<MenuDTO> getMenuTree(MenuQueryRequest request);
    
    /**
     * 获取所有菜单（树形结构）
     */
    List<MenuDTO> getAllMenus();
    
    /**
     * 获取用户菜单树
     */
    List<MenuDTO> getUserMenus(Long userId);
    
    /**
     * 获取用户客户端菜单（前端动态路由）
     */
    List<MenuDTO> getUserClientMenus(Long userId, String clientType, Long tenantId);
    
    /**
     * 获取角色菜单
     */
    List<MenuDTO> getRoleMenus(Long roleId);
    
    /**
     * 分配角色菜单
     */
    void assignRoleMenus(Long roleId, List<Long> menuIds, Long operatorId);
    
    /**
     * 更新菜单状态
     */
    void updateStatus(Long menuId, Integer status, Long operatorId);
    
    /**
     * 移动菜单
     */
    MenuDTO moveMenu(Long menuId, Long newParentId, Long operatorId);
    
    /**
     * 更新菜单排序
     */
    void updateSortOrder(List<Long> menuIds, Long operatorId);
    
    /**
     * 检查用户是否有权限
     */
    boolean hasPermission(Long userId, String permission);
    
    /**
     * 获取用户权限列表
     */
    List<String> getUserPermissions(Long userId);
    
    /**
     * 验证菜单编码唯一性
     */
    boolean validateMenuCode(String menuCode, Long excludeId);
}
