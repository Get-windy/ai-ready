package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysRolePermission;
import cn.aiedge.base.entity.SysRoleMenu;
import cn.aiedge.base.mapper.SysRoleMapper;
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
        removeById(roleId);
        log.info("删除角色成功: roleId={}", roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 删除原有权限
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        // TODO: 实现权限分配
        log.info("分配权限成功: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // TODO: 实现菜单分配
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