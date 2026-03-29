package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.mapper.SysPermissionMapper;
import cn.aiedge.base.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 权限服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPermission(SysPermission permission) {
        // 检查权限编码是否存在
        if (checkPermissionCodeExists(permission.getPermissionCode(), permission.getTenantId(), null)) {
            throw new RuntimeException("权限编码已存在");
        }

        permission.setStatus(0);
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());

        save(permission);
        log.info("创建权限成功: permissionId={}, permissionCode={}", permission.getId(), permission.getPermissionCode());
        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(SysPermission permission) {
        // 检查权限编码是否存在（排除自身）
        if (checkPermissionCodeExists(permission.getPermissionCode(), permission.getTenantId(), permission.getId())) {
            throw new RuntimeException("权限编码已存在");
        }

        permission.setUpdateTime(LocalDateTime.now());
        updateById(permission);
        log.info("更新权限成功: permissionId={}", permission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long permissionId) {
        // 检查是否有子权限
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId, permissionId);
        long count = count(wrapper);
        if (count > 0) {
            throw new RuntimeException("存在子权限，无法删除");
        }

        removeById(permissionId);
        log.info("删除权限成功: permissionId={}", permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePermissions(List<Long> permissionIds) {
        // 检查是否有子权限
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysPermission::getParentId, permissionIds);
        long count = count(wrapper);
        if (count > 0) {
            throw new RuntimeException("部分权限存在子权限，无法删除");
        }

        removeByIds(permissionIds);
        log.info("批量删除权限成功: permissionIds={}", permissionIds);
    }

    @Override
    public Page<SysPermission> pagePermissions(Page<SysPermission> page, Long tenantId,
                                                String permissionName, Integer permissionType, Integer status) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getTenantId, tenantId)
               .eq(permissionType != null, SysPermission::getPermissionType, permissionType)
               .eq(status != null, SysPermission::getStatus, status)
               .like(permissionName != null && !permissionName.isEmpty(), SysPermission::getPermissionName, permissionName)
               .orderByAsc(SysPermission::getSort);

        return page(page, wrapper);
    }

    @Override
    public SysPermission getPermissionDetail(Long permissionId) {
        return getById(permissionId);
    }

    @Override
    public List<SysPermission> getPermissionTree(Long tenantId) {
        List<SysPermission> allPermissions = listAllPermissions(tenantId);
        return buildPermissionTree(allPermissions, 0L);
    }

    @Override
    public List<SysPermission> getUserPermissions(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public List<SysPermission> getRolePermissions(Long roleId) {
        return baseMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public List<SysPermission> getChildrenPermissions(Long parentId, Long tenantId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId, parentId)
               .eq(SysPermission::getTenantId, tenantId)
               .orderByAsc(SysPermission::getSort);
        return list(wrapper);
    }

    @Override
    public boolean checkPermissionCodeExists(String permissionCode, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionCode, permissionCode)
               .eq(SysPermission::getTenantId, tenantId)
               .ne(excludeId != null, SysPermission::getId, excludeId);
        return count(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermissionStatus(Long permissionId, Integer status) {
        SysPermission permission = new SysPermission();
        permission.setId(permissionId);
        permission.setStatus(status);
        permission.setUpdateTime(LocalDateTime.now());
        updateById(permission);
        log.info("更新权限状态成功: permissionId={}, status={}", permissionId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermissionSort(Long permissionId, Integer sort) {
        SysPermission permission = new SysPermission();
        permission.setId(permissionId);
        permission.setSort(sort);
        permission.setUpdateTime(LocalDateTime.now());
        updateById(permission);
        log.info("更新权限排序成功: permissionId={}, sort={}", permissionId, sort);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取所有权限列表
     */
    private List<SysPermission> listAllPermissions(Long tenantId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getTenantId, tenantId)
               .orderByAsc(SysPermission::getSort);
        return list(wrapper);
    }

    /**
     * 构建权限树
     */
    private List<SysPermission> buildPermissionTree(List<SysPermission> allPermissions, Long parentId) {
        List<SysPermission> tree = new ArrayList<>();
        for (SysPermission permission : allPermissions) {
            if (permission.getParentId().equals(parentId)) {
                // 递归构建子树（这里简化处理，实际可使用children字段）
                tree.add(permission);
            }
        }
        return tree;
    }
}