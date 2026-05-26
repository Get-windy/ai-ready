package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysRoleMenu;
import cn.aiedge.base.entity.SysRolePermission;
import cn.aiedge.base.mapper.SysRoleMapper;
import cn.aiedge.base.mapper.SysRoleMenuMapper;
import cn.aiedge.base.mapper.SysRolePermissionMapper;
import cn.aiedge.base.service.SysRoleService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> 
        implements SysRoleService {

    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(SysRole role) {
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setCreateBy(StpUtil.getLoginIdAsLong());
        save(role);
        log.info("创建角色成功: roleId={}, roleName={}", role.getId(), role.getRoleName());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole role) {
        role.setUpdateTime(LocalDateTime.now());
        updateById(role);
        log.info("更新角色成功: roleId={}", role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        // 删除角色前先删除关联数据
        rolePermissionMapper.deleteByRoleId(roleId);
        roleMenuMapper.deleteByRoleId(roleId);
        removeById(roleId);
        log.info("删除角色成功: roleId={}", roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 参数校验
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        if (permissionIds == null || permissionIds.isEmpty()) {
            // 清空角色权限
            rolePermissionMapper.deleteByRoleId(roleId);
            log.info("清空角色权限: roleId={}", roleId);
            return;
        }

        // 获取角色信息用于租户ID
        SysRole role = getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 批量插入新的权限关联
        List<SysRolePermission> rolePermissions = permissionIds.stream()
                .distinct()
                .map(permissionId -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permissionId);
                    rp.setTenantId(role.getTenantId());
                    return rp;
                })
                .toList();

        if (!rolePermissions.isEmpty()) {
            rolePermissionMapper.batchInsert(rolePermissions);
        }

        log.info("分配权限成功: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 参数校验
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        if (menuIds == null || menuIds.isEmpty()) {
            // 清空角色菜单
            roleMenuMapper.deleteByRoleId(roleId);
            log.info("清空角色菜单: roleId={}", roleId);
            return;
        }

        // 获取角色信息用于租户ID
        SysRole role = getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 删除原有菜单关联
        roleMenuMapper.deleteByRoleId(roleId);

        // 批量插入新的菜单关联
        List<SysRoleMenu> roleMenus = menuIds.stream()
                .distinct()
                .map(menuId -> {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(roleId);
                    rm.setMenuId(menuId);
                    rm.setTenantId(role.getTenantId());
                    return rm;
                })
                .toList();

        if (!roleMenus.isEmpty()) {
            roleMenuMapper.batchInsert(roleMenus);
        }

        log.info("分配菜单成功: roleId={}, menuIds={}", roleId, menuIds);
    }

    @Override
    public Page<SysRole> pageRoles(Page<SysRole> page, Long tenantId, String roleName, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getTenantId, tenantId)
                .like(roleName != null, SysRole::getRoleName, roleName)
                .eq(status != null, SysRole::getStatus, status)
                .orderByDesc(SysRole::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return baseMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return baseMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleStatus(Long roleId, Integer status) {
        SysRole role = new SysRole();
        role.setId(roleId);
        role.setStatus(status);
        role.setUpdateTime(LocalDateTime.now());
        updateById(role);
        log.info("更新角色状态成功: roleId={}, status={}", roleId, status);
    }
}
