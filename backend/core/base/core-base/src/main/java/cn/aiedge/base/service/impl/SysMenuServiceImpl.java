package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.mapper.SysMenuMapper;
import cn.aiedge.base.service.SysMenuService;
import cn.aiedge.base.service.SysTenantMenuService;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.security.UnifiedPermissionCacheService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
        implements SysMenuService {

    private final SysUserService userService;
    private final UnifiedPermissionCacheService permissionCacheService;
    private final SysTenantMenuService tenantMenuService;

    // 系统租户ID（超级租户）
    private static final Long SYSTEM_TENANT_ID = 1L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(SysMenu menu) {
        // 检查菜单编码是否存在
        if (checkMenuCodeExists(menu.getMenuCode(), menu.getTenantId(), null)) {
            throw new RuntimeException("菜单编码已存在");
        }

        menu.setStatus(1); // 默认启用状态
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
    public List<SysMenu> getMenuByClientType(String clientType, Long tenantId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getClientType, clientType)
               .eq(SysMenu::getTenantId, tenantId)
               .eq(SysMenu::getStatus, 1) // status=1启用
               .eq(SysMenu::getVisible, 1) // visible=1可见
               .orderByAsc(SysMenu::getSort);
        List<SysMenu> menus = list(wrapper);
        return buildMenuTree(menus, 0L);
    }

    @Override
    public List<SysMenu> getUserMenuByClientType(String clientType, Long userId) {
        log.info("[菜单服务] getUserMenuByClientType 开始: clientType={}, userId={}", clientType, userId);

        // 检查用户是否是超级管理员
        List<String> roles = permissionCacheService.getRoles(userId);
        log.info("[菜单服务] 用户角色列表: {}", roles);

        boolean isSuperAdmin = roles != null && roles.contains("SUPER_ADMIN");
        log.info("[菜单服务] 是否超级管理员: {}", isSuperAdmin);

        if (isSuperAdmin) {
            // 超级管理员直接获取所有符合条件的菜单
            log.info("[菜单服务] 超级管理员，直接获取所有菜单");
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getClientType, clientType)
                   .eq(SysMenu::getStatus, 1) // status=1启用
                   .eq(SysMenu::getDeleted, 0) // 未删除
                   .eq(SysMenu::getVisible, 1) // visible=1可见
                   .orderByAsc(SysMenu::getSort);
            List<SysMenu> menus = list(wrapper);
            log.info("[菜单服务] 查询到的菜单数量: {}", menus.size());
            return buildMenuTree(menus, 0L);
        }

        // 普通用户：通过角色菜单关联查询
        // 获取用户角色ID列表
        List<Long> roleIds = baseMapper.selectRoleIdsByUserId(userId);
        log.info("[菜单服务] 用户角色ID列表: {}", roleIds);
        if (roleIds.isEmpty()) {
            log.warn("[菜单服务] 用户没有角色，返回空列表");
            return new ArrayList<>();
        }

        // 获取角色关联的菜单ID列表
        List<Long> menuIds = baseMapper.selectMenuIdsByRoleIds(roleIds);
        log.info("[菜单服务] 角色关联的菜单ID列表: {}", menuIds);
        if (menuIds.isEmpty()) {
            log.warn("[菜单服务] 角色没有关联菜单，返回空列表");
            return new ArrayList<>();
        }

        // 获取菜单列表并按客户端类型过滤
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, menuIds)
               .eq(SysMenu::getClientType, clientType)
               .eq(SysMenu::getStatus, 1) // status=1启用
               .eq(SysMenu::getVisible, 1) // visible=1可见
               .orderByAsc(SysMenu::getSort);
        List<SysMenu> menus = list(wrapper);
        log.info("[菜单服务] 查询到的菜单数量: {}", menus.size());

        List<SysMenu> tree = buildMenuTree(menus, 0L);
        log.info("[菜单服务] 构建的菜单树数量: {}", tree.size());
        return tree;
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

    @Override
    public List<SysMenu> getUserMegaMenus(String clientType, Long userId, Long tenantId) {
        log.info("[MegaMenu] getUserMegaMenus: clientType={}, userId={}, tenantId={}", clientType, userId, tenantId);

        boolean isSystemTenant = SYSTEM_TENANT_ID.equals(tenantId);
        boolean isSuperAdmin = false;

        // 获取用户角色
        List<String> roles = permissionCacheService.getRoles(userId);
        isSuperAdmin = roles != null && roles.contains("SUPER_ADMIN");

        Set<Long> finalMenuIds;

        if (isSystemTenant) {
            // 系统租户：跳过 sys_tenant_menu 授权检查，仅按角色权限过滤
            if (isSuperAdmin) {
                // 超管直接返回所有菜单
                return buildMenuTree(getAllMenusByClientType(clientType), 0L);
            }
            // 系统租户内普通用户：按角色菜单关联过滤
            List<Long> roleIds = baseMapper.selectRoleIdsByUserId(userId);
            if (roleIds.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> roleMenuIds = baseMapper.selectMenuIdsByRoleIds(roleIds);
            finalMenuIds = new HashSet<>(roleMenuIds);
        } else {
            // 普通租户：两级授权（sys_tenant_menu + sys_role_menu 取交集）
            // 第一级：系统管理员授权给租户的菜单
            Set<Long> tenantAuthorizedIds = tenantMenuService.getAuthorizedMenuIds(tenantId);

            // 第二级：角色授权的菜单
            List<Long> roleIds = baseMapper.selectRoleIdsByUserId(userId);
            Set<Long> roleMenuIds = (roleIds.isEmpty())
                    ? new HashSet<>()
                    : new HashSet<>(baseMapper.selectMenuIdsByRoleIds(roleIds));

            // 取交集
            finalMenuIds = new HashSet<>(tenantAuthorizedIds);
            finalMenuIds.retainAll(roleMenuIds);
        }

        if (finalMenuIds.isEmpty()) {
            log.warn("[MegaMenu] 用户没有可访问的菜单");
            return new ArrayList<>();
        }

        // 获取菜单列表并按 clientType 过滤
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getId, finalMenuIds)
               .eq(SysMenu::getClientType, clientType)
               .eq(SysMenu::getStatus, 1)
               .eq(SysMenu::getVisible, 1)
               .eq(SysMenu::getDeleted, 0);

        // 普通租户：只返回租户级菜单（menuLevel=0）
        if (!isSystemTenant) {
            wrapper.eq(SysMenu::getMenuLevel, 0);
        }
        // 系统租户：返回所有层级菜单（menuLevel 0 + 1），但已在 finalMenuIds 中过滤

        wrapper.orderByAsc(SysMenu::getSort);
        List<SysMenu> menus = list(wrapper);
        log.info("[MegaMenu] 查询到的菜单数量: {}", menus.size());

        return buildMenuTree(menus, 0L);
    }

    /**
     * 获取指定客户端类型的所有菜单（超管专用）
     */
    private List<SysMenu> getAllMenusByClientType(String clientType) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getClientType, clientType)
               .eq(SysMenu::getStatus, 1)
               .eq(SysMenu::getVisible, 1)
               .eq(SysMenu::getDeleted, 0)
               .orderByAsc(SysMenu::getSort);
        return list(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        log.info("[菜单服务] buildMenuTree 开始: allMenus.size={}, parentId={}", allMenus.size(), parentId);
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : allMenus) {
            log.info("[菜单服务] 检查菜单: id={}, menuName={}, parentId={}", menu.getId(), menu.getMenuName(), menu.getParentId());
            if (menu.getParentId().equals(parentId)) {
                log.info("[菜单服务] 找到匹配菜单: id={}, menuName={}", menu.getId(), menu.getMenuName());
                List<SysMenu> children = buildMenuTree(allMenus, menu.getId());
                log.info("[菜单服务] 子菜单数量: parentId={}, children.size={}", menu.getId(), children.size());
                menu.setChildren(children);
                tree.add(menu);
            }
        }
        log.info("[菜单服务] buildMenuTree 完成: tree.size={}, parentId={}", tree.size(), parentId);
        return tree;
    }
}