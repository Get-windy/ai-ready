package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.mapper.SysMenuMapper;
import cn.aiedge.base.service.SysMenuService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
        implements SysMenuService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(SysMenu menu) {
        // 检查菜单编码是否存在
        if (checkMenuCodeExists(menu.getMenuCode(), menu.getTenantId(), null)) {
            throw new RuntimeException("菜单编码已存在");
        }

        menu.setStatus(0);
        menu.setVisible(1);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());

        save(menu);
        log.info("创建菜单成功: menuId={}, menuCode={}", menu.getId(), menu.getMenuCode());
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenu menu) {
        // 检查菜单编码是否存在（排除自身）
        if (menu.getMenuCode() != null && 
            checkMenuCodeExists(menu.getMenuCode(), menu.getTenantId(), menu.getId())) {
            throw new RuntimeException("菜单编码已存在");
        }

        menu.setUpdateTime(LocalDateTime.now());
        updateById(menu);
        log.info("更新菜单成功: menuId={}", menu.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        // 检查是否有子菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, menuId);
        long count = count(wrapper);
        if (count > 0) {
            throw new RuntimeException("存在子菜单，无法删除");
        }

        removeById(menuId);
        log.info("删除菜单成功: menuId={}", menuId);
    }

    @Override
    public List<SysMenu> getMenuTree(Long tenantId) {
        List<SysMenu> allMenus = listAllMenus(tenantId);
        return buildMenuTree(allMenus, 0L);
    }

    @Override
    public List<SysMenu> getUserMenuTree(Long userId) {
        // 获取用户角色ID列表
        List<Long> roleIds = baseMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取角色关联的菜单ID列表
        List<Long> menuIds = baseMapper.selectMenuIdsByRoleIds(roleIds);
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取菜单列表
        List<SysMenu> menus = baseMapper.selectMenusByIds(menuIds);
        return buildMenuTree(menus, 0L);
    }

    @Override
    public List<SysMenu> getChildrenMenus(Long parentId, Long tenantId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, parentId)
               .eq(SysMenu::getTenantId, tenantId)
               .orderByAsc(SysMenu::getSort);
        return list(wrapper);
    }

    @Override
    public SysMenu getMenuDetail(Long menuId) {
        return getById(menuId);
    }

    @Override
    public List<SysMenu> listAllMenus(Long tenantId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getTenantId, tenantId)
               .orderByAsc(SysMenu::getSort);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenuSort(Long menuId, Integer sort) {
        SysMenu menu = new SysMenu();
        menu.setId(menuId);
        menu.setSort(sort);
        menu.setUpdateTime(LocalDateTime.now());
        updateById(menu);
        log.info("更新菜单排序成功: menuId={}, sort={}", menuId, sort);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenuStatus(Long menuId, Integer status) {
        SysMenu menu = new SysMenu();
        menu.setId(menuId);
        menu.setStatus(status);
        menu.setUpdateTime(LocalDateTime.now());
        updateById(menu);
        log.info("更新菜单状态成功: menuId={}, status={}", menuId, status);
    }

    @Override
    public boolean checkMenuCodeExists(String menuCode, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuCode, menuCode)
               .eq(SysMenu::getTenantId, tenantId)
               .ne(excludeId != null, SysMenu::getId, excludeId);
        return count(wrapper) > 0;
    }

    // ==================== 私有方法 ====================

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : allMenus) {
            if (menu.getParentId().equals(parentId)) {
                // 递归获取子菜单（简化版，实际可添加children字段）
                tree.add(menu);
            }
        }
        return tree;
    }
}