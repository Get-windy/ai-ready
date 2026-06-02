package com.aiready.menu.service.impl;

import com.aiready.menu.dto.*;
import com.aiready.menu.entity.Menu;
import com.aiready.menu.mapper.MenuMapper;
import com.aiready.menu.service.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private final MenuMapper menuMapper;

    @Override
    @Transactional
    public MenuDTO createMenu(MenuSaveRequest request, Long operatorId) {
        // 验证菜单编码唯一性
        if (!validateMenuCode(request.getMenuCode(), null)) {
            throw new RuntimeException("菜单编码已存在：" + request.getMenuCode());
        }

        Menu menu = new Menu();
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setMenuCode(request.getMenuCode());
        menu.setIcon(request.getIcon());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPermissions(request.getPermissions());
        menu.setMenuType(request.getMenuType());
        menu.setSort(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : 1);
        menu.setIsCache(request.getKeepAlive() != null ? request.getKeepAlive() : 0);
        menu.setIsExternal(request.getExternal() != null ? request.getExternal() : 0);
        menu.setRemark(request.getRemark());

        menu.setCreateBy(operatorId);
        menu.setUpdateBy(operatorId);

        menuMapper.insert(menu);

        return convertToDTO(menu);
    }

    @Override
    @Transactional
    public MenuDTO updateMenu(MenuUpdateRequest request, Long operatorId) {
        // 验证菜单编码唯一性
        if (!validateMenuCode(request.getMenuCode(), request.getId())) {
            throw new RuntimeException("菜单编码已存在：" + request.getMenuCode());
        }

        Menu menu = menuMapper.selectById(request.getId());
        if (menu == null) {
            throw new RuntimeException("菜单不存在，ID：" + request.getId());
        }

        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setIcon(request.getIcon());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPermissions(request.getPermissions());
        menu.setMenuType(request.getMenuType());
        menu.setSort(request.getSortOrder());
        menu.setStatus(request.getStatus());
        menu.setVisible(request.getVisible());
        menu.setIsCache(request.getKeepAlive());
        menu.setIsExternal(request.getExternal());
        menu.setRemark(request.getRemark());

        menu.setUpdateBy(operatorId);
        menu.setUpdateTime(LocalDateTime.now());

        menuMapper.updateById(menu);

        return convertToDTO(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long menuId, Long operatorId) {
        // 检查是否有子菜单
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getParentId, menuId);
        Long count = menuMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("删除失败，该菜单下还有子菜单！");
        }

        Menu menu = menuMapper.selectById(menuId);
        if (menu != null) {
            menuMapper.deleteById(menuId);
        }
    }

    @Override
    @Transactional
    public void batchDeleteMenus(List<Long> menuIds, Long operatorId) {
        for (Long menuId : menuIds) {
            deleteMenu(menuId, operatorId);
        }
    }

    @Override
    public MenuDTO getMenuById(Long menuId) {
        Menu menu = menuMapper.selectById(menuId);
        return menu != null ? convertToDTO(menu) : null;
    }

    @Override
    public List<MenuDTO> getMenuTree(MenuQueryRequest request) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(request.getMenuName())) {
            wrapper.like(Menu::getMenuName, request.getMenuName());
        }
        if (StringUtils.isNotBlank(request.getMenuCode())) {
            wrapper.eq(Menu::getMenuCode, request.getMenuCode());
        }
        if (request.getMenuType() != null) {
            wrapper.eq(Menu::getMenuType, request.getMenuType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Menu::getStatus, request.getStatus());
        }
        if (request.getOnlyEnabled()) {
            wrapper.eq(Menu::getStatus, 1);
        }
        wrapper.eq(Menu::getDeleted, 0);
        wrapper.orderByAsc(Menu::getSort).orderByAsc(Menu::getId);

        List<Menu> menus = menuMapper.selectList(wrapper);
        List<MenuDTO> menuDTOs = menus.stream().map(this::convertToDTO).collect(Collectors.toList());

        if (request.getBuildTree()) {
            return buildTree(menuDTOs);
        }

        return menuDTOs;
    }

    @Override
    public List<MenuDTO> getAllMenus() {
        MenuQueryRequest request = new MenuQueryRequest();
        request.setBuildTree(true);
        request.setOnlyEnabled(false);
        return getMenuTree(request);
    }

    @Override
    public List<MenuDTO> getUserMenus(Long userId) {
        List<Menu> menus = menuMapper.selectMenusByUserId(userId);
        List<MenuDTO> menuDTOs = menus.stream().map(this::convertToDTO).collect(Collectors.toList());
        return buildTree(menuDTOs);
    }

    @Override
    public List<MenuDTO> getUserClientMenus(Long userId, String clientType, Long tenantId) {
        List<Menu> menus = menuMapper.selectMenusByUserId(userId);
        List<MenuDTO> menuDTOs = menus.stream()
                .filter(menu -> menu.getMenuType() != 3)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return buildTree(menuDTOs);
    }

    @Override
    public List<MenuDTO> getRoleMenus(Long roleId) {
        List<Menu> menus = menuMapper.selectMenusByRoleId(roleId);
        return menus.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignRoleMenus(Long roleId, List<Long> menuIds, Long operatorId) {
        // 实际的分配逻辑需要关联角色菜单表，这里只是占位符
        // 在真实实现中，需要清空角色的菜单关联，然后重新分配
        log.info("分配角色菜单：roleId={}, menuIds={}, operator={}", roleId, menuIds, operatorId);
    }

    @Override
    @Transactional
    public void updateStatus(Long menuId, Integer status, Long operatorId) {
        Menu menu = menuMapper.selectById(menuId);
        if (menu != null) {
            menu.setStatus(status);
            menu.setUpdateBy(operatorId);
            menu.setUpdateTime(LocalDateTime.now());
            menuMapper.updateById(menu);
        }
    }

    @Override
    public MenuDTO moveMenu(Long menuId, Long newParentId, Long operatorId) {
        Menu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new RuntimeException("菜单不存在，ID：" + menuId);
        }

        // 检查是否形成循环引用
        if (isParentChildConflict(menuId, newParentId)) {
            throw new RuntimeException("不能将菜单移动到自己的子菜单下！");
        }

        menu.setParentId(newParentId);
        menu.setUpdateBy(operatorId);
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menu);

        return convertToDTO(menu);
    }

    @Override
    public void updateSortOrder(List<Long> menuIds, Long operatorId) {
        List<Menu> menus = new ArrayList<>();
        for (int i = 0; i < menuIds.size(); i++) {
            Long menuId = menuIds.get(i);
            Menu menu = menuMapper.selectById(menuId);
            if (menu != null) {
                menu.setSort(i);
                menu.setUpdateBy(operatorId);
                menu.setUpdateTime(LocalDateTime.now());
                menus.add(menu);
            }
        }
        if (!menus.isEmpty()) {
            // 批量更新排序
            for (Menu menu : menus) {
                menuMapper.updateById(menu);
            }
        }
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        // 获取用户所有菜单
        List<Menu> userMenus = menuMapper.selectMenusByUserId(userId);
        // 检查是否包含指定权限
        return userMenus.stream()
                .anyMatch(menu -> menu.getPermissions() != null && 
                        Arrays.asList(menu.getPermissions().split(",")).contains(permission));
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        List<Menu> userMenus = menuMapper.selectMenusByUserId(userId);
        Set<String> permissions = new HashSet<>();
        for (Menu menu : userMenus) {
            if (menu.getPermissions() != null && !menu.getPermissions().isEmpty()) {
                permissions.addAll(Arrays.asList(menu.getPermissions().split(",")));
            }
        }
        return new ArrayList<>(permissions);
    }

    @Override
    public boolean validateMenuCode(String menuCode, Long excludeId) {
        Integer count = menuMapper.checkMenuCodeExists(menuCode, excludeId);
        return count == 0;
    }

    /**
     * 构建菜单树
     */
    private List<MenuDTO> buildTree(List<MenuDTO> menuList) {
        Map<Long, MenuDTO> menuMap = new HashMap<>();
        List<MenuDTO> rootMenus = new ArrayList<>();

        // 将所有菜单放入map中，便于查找
        for (MenuDTO menu : menuList) {
            menuMap.put(menu.getId(), menu);
        }

        // 构建树形结构
        for (MenuDTO menu : menuList) {
            if (menu.getParentId() == 0 || menu.getParentId() == null) {
                // 根菜单
                rootMenus.add(menu);
            } else {
                // 查找父菜单
                MenuDTO parentMenu = menuMap.get(menu.getParentId());
                if (parentMenu != null) {
                    if (parentMenu.getChildren() == null) {
                        parentMenu.setChildren(new ArrayList<>());
                    }
                    parentMenu.getChildren().add(menu);
                }
            }
        }

        return rootMenus;
    }

    /**
     * 检查父子关系冲突（防止循环引用）
     */
    private boolean isParentChildConflict(Long menuId, Long newParentId) {
        if (newParentId == null || newParentId == 0) {
            return false; // 根节点不会有冲突
        }

        // 递归检查新父节点是否是当前菜单的子节点
        return checkChildRecursive(newParentId, menuId);
    }

    /**
     * 递归检查是否为子节点
     */
    private boolean checkChildRecursive(Long parentId, Long targetId) {
        List<Menu> children = menuMapper.selectChildrenByParentId(parentId);
        for (Menu child : children) {
            if (child.getId().equals(targetId)) {
                return true; // 发现循环引用
            }
            if (checkChildRecursive(child.getId(), targetId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 转换为DTO
     */
    private MenuDTO convertToDTO(Menu menu) {
        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setParentId(menu.getParentId());
        dto.setMenuName(menu.getMenuName());
        dto.setMenuCode(menu.getMenuCode());
        dto.setIcon(menu.getIcon());
        dto.setPath(menu.getPath());
        dto.setComponent(menu.getComponent());
        dto.setRouteName(menu.getRouteName());
        dto.setPermissions(menu.getPermissions());
        if (menu.getPermissions() != null && !menu.getPermissions().isEmpty()) {
            dto.setPermissionList(Arrays.asList(menu.getPermissions().split(",")));
        }
        dto.setMenuType(menu.getMenuType());
        dto.setMenuTypeDesc(getMenuTypeDesc(menu.getMenuType()));
        dto.setSortOrder(menu.getSort());
        dto.setStatus(menu.getStatus());
        dto.setStatusDesc(menu.getStatus() != null && menu.getStatus() == 1 ? "启用" : "禁用");
        dto.setVisible(menu.getVisible());
        dto.setKeepAlive(menu.getIsCache());
        dto.setExternal(menu.getIsExternal());
        dto.setClientType(menu.getClientType());
        dto.setRemark(menu.getRemark());
        dto.setCreateTime(menu.getCreateTime());
        dto.setUpdateTime(menu.getUpdateTime());
        return dto;
    }

    /**
     * 获取菜单类型描述
     */
    private String getMenuTypeDesc(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "目录";
            case 2: return "菜单";
            case 3: return "按钮";
            default: return "未知";
        }
    }
}
