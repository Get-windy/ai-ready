package cn.aiedge.base.service.impl;

import cn.aiedge.base.config.SuperAdminConfig;
import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.mapper.SysMenuMapper;
import cn.aiedge.base.service.MenuVisibilityService;
import cn.aiedge.base.service.SysMenuService;
import cn.aiedge.base.service.SysTenantMenuService;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.security.MenuPermissionDeriver;
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
    private final MenuPermissionDeriver menuPermissionDeriver;
    private final SysTenantMenuService tenantMenuService;
    private final MenuVisibilityService menuVisibilityService;
    private final SuperAdminConfig superAdminConfig;

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
        // 菜单为系统级全局资源（tenant_id=0），忽略传入的 tenantId
        List<SysMenu> allMenus = listAllMenus(0L);
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

        boolean isSuperAdmin = roles != null && superAdminConfig.hasSuperAdminRole(roles);
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
        // 菜单为系统级全局资源（tenant_id=0）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, parentId)
               .eq(SysMenu::getTenantId, 0L)
               .orderByAsc(SysMenu::getSort);
        return list(wrapper);
    }

    @Override
    public SysMenu getMenuDetail(Long menuId) {
        return getById(menuId);
    }

    @Override
    public List<SysMenu> listAllMenus(Long tenantId) {
        // 菜单为系统级全局资源（tenant_id=0），忽略传入的 tenantId
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getTenantId, 0L)
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
        // 菜单编码唯一性检查基于全局菜单（tenant_id=0）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuCode, menuCode)
               .eq(SysMenu::getTenantId, 0L)
               .ne(excludeId != null, SysMenu::getId, excludeId);
        return count(wrapper) > 0;
    }

    @Override
    public List<SysMenu> getUserMegaMenus(String clientType, Long userId, Long tenantId) {
        log.info("[MegaMenu] getUserMegaMenus: clientType={}, userId={}, tenantId={}", clientType, userId, tenantId);

        boolean isSystemTenant = SYSTEM_TENANT_ID.equals(tenantId);

        // 获取用户角色（走 UnifiedPermissionCacheService 的 L1 Caffeine / L2 Redis 缓存，不额外打库）
        List<String> roles = permissionCacheService.getRoles(userId);
        boolean isSuperAdmin = roles != null && roles.contains("SUPER_ADMIN");

        if (isSystemTenant && isSuperAdmin) {
            // 超管返回 tenant-admin + system-admin 全部菜单
            // ⚠️ 这条早退分支同样要过「租户级菜单显隐」（设置 → 菜单配置）：
            //    开发/默认登录的 admin 正是「系统租户 + 超管」，不过滤的话
            //    本页关掉的菜单在导航里依然可见，开关就成了空操作。
            List<SysMenu> allMenus = getAllMenusForSystemAdmin();
            return buildMenuTree(excludeTenantHidden(allMenus, tenantId), 0L);
        }

        // ── 候选菜单：一次查询取回，后续三类过滤全在内存完成（无 N+1）──
        // 过滤条件与改造前完全一致（clientType / status / visible / deleted / menuLevel）。
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getClientType, clientType)
               .eq(SysMenu::getStatus, 1)
               .eq(SysMenu::getVisible, 1)
               .eq(SysMenu::getDeleted, 0);

        // 普通租户：只返回租户级菜单（menuLevel=0），但超管不受限制
        if (!isSystemTenant && !isSuperAdmin) {
            wrapper.eq(SysMenu::getMenuLevel, 0);
        }
        // 系统租户：返回所有层级菜单（menuLevel 0 + 1），在下面的内存过滤中逐个判定

        wrapper.orderByAsc(SysMenu::getSort);
        List<SysMenu> candidates = list(wrapper);

        // ── 过滤①：用户级授权 = 由 sys_permission 派生的菜单可见性（平台-AUTHZ-01）──
        // 替代原先「角色 → sys_role_menu」这一级：该表全库仅 3 行（只属 SUPER_ADMIN），
        // 导致除超管外所有角色的菜单接口恒为空数组。
        // heldMenuCodes = null 表示「不按权限码过滤」（超管 / 通配符权限）。
        Set<String> heldMenuCodes = null;
        Set<String> knownMenuCodes = null;
        if (!isSuperAdmin) {
            // 用户有效权限码：同样走权限缓存（L1 30s / L2 5min）
            List<String> userPermissions = permissionCacheService.getPermissions(userId);
            if (userPermissions != null && userPermissions.contains("*")) {
                // 通配符 = 拥有全部权限（UnifiedPermissionCacheService 对超管角色返回 "*"），不按码过滤
                log.info("[MegaMenu] 用户持有通配符权限，跳过菜单权限码过滤: userId={}", userId);
            } else {
                heldMenuCodes = menuPermissionDeriver.coveredMenuCodePrefixes(userPermissions);
                knownMenuCodes = menuPermissionDeriver.allMenuCodePrefixes();
            }
        }

        // ── 过滤②：平台 → 租户的菜单范围闸门（sys_tenant_menu，权益层）──
        // 系统租户跳过（与改造前一致）。
        // ⚠️ 过渡口径：该租户在 sys_tenant_menu 里**一行都没有**时，按「平台尚未配置」放行，
        //    而不是按「没有授权任何菜单」处理 —— 后者会让所有租户的菜单接口继续返回空数组
        //    （实测：全表仅 4 行，且 tenant_id 全为 0，见 2026-09-22 证据）。
        //    一旦平台真的给某租户配了行，这里立刻恢复为严格过滤。
        Set<Long> tenantMenuGate = null;
        if (!isSystemTenant) {
            Set<Long> authorizedMenuIds = tenantMenuService.getAuthorizedMenuIds(tenantId);
            if (authorizedMenuIds == null || authorizedMenuIds.isEmpty()) {
                log.warn("[MegaMenu] 租户 {} 在 sys_tenant_menu 中没有任何授权行，按「平台尚未配置」放行；"
                        + "菜单范围的权益闸门现由模块 entitlement（sys_tenant_module + sys_module_permission）承担",
                        tenantId);
            } else {
                tenantMenuGate = authorizedMenuIds;
            }
        }

        // ── 过滤③：租户级菜单显隐（设置 → 系统配置 → 菜单配置）──
        // 落库位置为 sys_project_config（tenant_id + config_key），默认无配置 = 全部可见，
        // 因此未使用本页时本方法行为与改造前完全一致。
        Set<Long> hiddenMenuIds = menuVisibilityService.getHiddenMenuIds(tenantId);

        List<SysMenu> visibleMenus = new ArrayList<>(candidates.size());
        int blockedByGate = 0;
        int blockedByPermission = 0;
        int blockedByVisibility = 0;
        for (SysMenu menu : candidates) {
            if (tenantMenuGate != null && !tenantMenuGate.contains(menu.getId())) {
                blockedByGate++;
                continue;
            }
            if (!hiddenMenuIds.isEmpty() && hiddenMenuIds.contains(menu.getId())) {
                blockedByVisibility++;
                continue;
            }
            if (heldMenuCodes != null) {
                String menuCode = menu.getMenuCode();
                // 过渡口径（硬性）：menu_code 为空、或权限码库里没有任何权限码以它前缀命中的菜单
                // ⇒ 保持可见。只有「存在对应权限码、但该用户不持有」的菜单才隐藏。
                // 理由：权限码库覆盖不足时，派生不能反而让导航缩水；真正的访问控制点是后端接口鉴权。
                if (menuCode != null && !menuCode.isEmpty()
                        && knownMenuCodes.contains(menuCode)
                        && !heldMenuCodes.contains(menuCode)) {
                    blockedByPermission++;
                    continue;
                }
            }
            visibleMenus.add(menu);
        }

        if (visibleMenus.isEmpty()) {
            log.warn("[MegaMenu] 用户没有可访问的菜单: userId={}, tenantId={}, clientType={}, "
                            + "候选={}, 权限码过滤={}, 租户闸门过滤={}, 显隐过滤={}",
                    userId, tenantId, clientType, candidates.size(),
                    blockedByPermission, blockedByGate, blockedByVisibility);
            return new ArrayList<>();
        }
        log.info("[MegaMenu] 可见菜单数={}（候选={}, 权限码过滤={}, 租户闸门过滤={}, 显隐过滤={}）",
                visibleMenus.size(), candidates.size(),
                blockedByPermission, blockedByGate, blockedByVisibility);

        return buildMenuTree(visibleMenus, 0L);
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

    /**
     * 系统超管：同时返回 tenant-admin 和 system-admin 两类菜单
     */
    private List<SysMenu> getAllMenusForSystemAdmin() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getClientType, "tenant-admin", "system-admin")
               .eq(SysMenu::getStatus, 1)
               .eq(SysMenu::getVisible, 1)
               .eq(SysMenu::getDeleted, 0)
               .orderByAsc(SysMenu::getSort);
        return list(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 剔除本租户在「设置 → 系统配置 → 菜单配置」中显式隐藏的菜单。
     * 无隐藏配置（默认）时原样返回，保证与改造前的行为一致。
     */
    private List<SysMenu> excludeTenantHidden(List<SysMenu> menus, Long tenantId) {
        Set<Long> hiddenMenuIds = menuVisibilityService.getHiddenMenuIds(tenantId);
        if (hiddenMenuIds.isEmpty()) {
            return menus;
        }
        return menus.stream()
                .filter(menu -> !hiddenMenuIds.contains(menu.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : allMenus) {
            if (menu.getParentId().equals(parentId)) {
                List<SysMenu> children = buildMenuTree(allMenus, menu.getId());
                menu.setChildren(children);
                tree.add(menu);
            }
        }
        log.debug("[菜单服务] buildMenuTree: parentId={}, tree.size={}", parentId, tree.size());
        return tree;
    }
}