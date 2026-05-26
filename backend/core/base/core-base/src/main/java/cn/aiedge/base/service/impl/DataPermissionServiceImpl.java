package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.DataPermission;
import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.DataPermissionMapper;
import cn.aiedge.base.service.DataPermissionService;
import cn.aiedge.base.service.SysRoleService;
import cn.aiedge.base.service.SysUserService;
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
 * 数据权限服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl extends ServiceImpl<DataPermissionMapper, DataPermission>
        implements DataPermissionService {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final DataPermissionMapper dataPermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDataPermission(DataPermission dataPermission) {
        // 检查权限编码是否已存在
        if (dataPermission.getPermissionCode() != null) {
            LambdaQueryWrapper<DataPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataPermission::getPermissionCode, dataPermission.getPermissionCode())
                   .eq(DataPermission::getTenantId, dataPermission.getTenantId());
            long count = count(wrapper);
            if (count > 0) {
                throw new RuntimeException("数据权限编码已存在");
            }
        }

        dataPermission.setStatus(0);
        dataPermission.setCreateTime(LocalDateTime.now());
        dataPermission.setUpdateTime(LocalDateTime.now());
        dataPermission.setCreateBy(StpUtil.getLoginIdAsLong());
        save(dataPermission);
        log.info("创建数据权限成功: permissionId={}, permissionName={}", 
                 dataPermission.getId(), dataPermission.getPermissionName());
        return dataPermission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDataPermission(DataPermission dataPermission) {
        // 检查权限编码是否与其他权限冲突（排除自身）
        if (dataPermission.getPermissionCode() != null) {
            LambdaQueryWrapper<DataPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataPermission::getPermissionCode, dataPermission.getPermissionCode())
                   .eq(DataPermission::getTenantId, dataPermission.getTenantId())
                   .ne(DataPermission::getId, dataPermission.getId());
            long count = count(wrapper);
            if (count > 0) {
                throw new RuntimeException("数据权限编码已存在");
            }
        }

        dataPermission.setUpdateTime(LocalDateTime.now());
        updateById(dataPermission);
        log.info("更新数据权限成功: permissionId={}", dataPermission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataPermission(Long dataPermissionId) {
        removeById(dataPermissionId);
        log.info("删除数据权限成功: permissionId={}", dataPermissionId);
    }

    @Override
    public Page<DataPermission> pageDataPermissions(Page<DataPermission> page, Long tenantId,
                                                   String permissionName, Integer scopeType, Integer status) {
        LambdaQueryWrapper<DataPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, DataPermission::getTenantId, tenantId)
               .like(permissionName != null && !permissionName.isEmpty(), 
                     DataPermission::getPermissionName, permissionName)
               .eq(scopeType != null, DataPermission::getScopeType, scopeType)
               .eq(status != null, DataPermission::getStatus, status)
               .orderByDesc(DataPermission::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<DataPermission> getUserDataPermissions(Long userId) {
        LambdaQueryWrapper<DataPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataPermission::getUserId, userId)
               .eq(DataPermission::getStatus, 0); // 只获取启用的权限
        return list(wrapper);
    }

    @Override
    public List<DataPermission> getRoleDataPermissions(Long roleId) {
        LambdaQueryWrapper<DataPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataPermission::getRoleId, roleId)
               .eq(DataPermission::getStatus, 0); // 只获取启用的权限
        return list(wrapper);
    }

    @Override
    public boolean checkUserDataPermission(Long userId, String resourceType, String resourceId, String action) {
        if (userId == null) {
            return false;
        }

        // 获取用户的角色信息
        List<SysRole> userRoles = roleService.getUserRoles(userId);
        if (userRoles.isEmpty()) {
            return false;
        }

        // 获取用户和用户角色的数据权限
        List<DataPermission> userDataPermissions = getUserDataPermissions(userId);
        List<Long> roleIds = userRoles.stream()
                .map(SysRole::getId)
                .toList();
        
        List<DataPermission> roleDataPermissions = dataPermissionMapper.selectDataPermissionsByRoleIds(roleIds);

        // 合并所有数据权限
        userDataPermissions.addAll(roleDataPermissions);

        // 检查是否有对应的数据权限
        // 这里可以根据具体的业务逻辑来实现权限检查
        // 简化实现：只要有对应的数据权限配置，就认为有权限
        return userDataPermissions.stream()
                .anyMatch(dp -> dp.getStatus() != null && dp.getStatus() == 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssignDataPermissionToUser(Long userId, List<Long> dataPermissionIds) {
        if (userId == null || dataPermissionIds == null || dataPermissionIds.isEmpty()) {
            return;
        }

        // 检查用户是否存在
        SysUser user = userService.getUserDetail(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新权限的作用域类型为用户级
        for (Long permissionId : dataPermissionIds) {
            DataPermission permission = new DataPermission();
            permission.setId(permissionId);
            permission.setUserId(userId);
            permission.setScopeType(2); // 用户级
            updateById(permission);
        }

        log.info("批量分配数据权限给用户成功: userId={}, permissionIds={}, count={}", 
                 userId, dataPermissionIds, dataPermissionIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssignDataPermissionToRole(Long roleId, List<Long> dataPermissionIds) {
        if (roleId == null || dataPermissionIds == null || dataPermissionIds.isEmpty()) {
            return;
        }

        // 检查角色是否存在
        SysRole role = roleService.getById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 更新权限的作用域类型为角色级
        for (Long permissionId : dataPermissionIds) {
            DataPermission permission = new DataPermission();
            permission.setId(permissionId);
            permission.setRoleId(roleId);
            permission.setScopeType(1); // 角色级
            updateById(permission);
        }

        log.info("批量分配数据权限给角色成功: roleId={}, permissionIds={}, count={}", 
                 roleId, dataPermissionIds, dataPermissionIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDataPermissionStatus(Long dataPermissionId, Integer status) {
        DataPermission permission = new DataPermission();
        permission.setId(dataPermissionId);
        permission.setStatus(status);
        permission.setUpdateTime(LocalDateTime.now());
        updateById(permission);
        log.info("更新数据权限状态成功: permissionId={}, status={}", dataPermissionId, status);
    }
}