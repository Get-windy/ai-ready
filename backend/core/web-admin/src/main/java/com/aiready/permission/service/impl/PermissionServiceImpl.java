package com.aiready.permission.service.impl;

import com.aiready.permission.entity.Permission;
import com.aiready.permission.entity.RolePermission;
import com.aiready.permission.mapper.PermissionMapper;
import com.aiready.permission.mapper.RolePermissionMapper;
import com.aiready.permission.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public Set<String> getPermissionCodesByUserId(Long userId) {
        List<Permission> permissions = getPermissionsByUserId(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> allPermissions = list(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSortOrder));
        
        return buildPermissionTree(allPermissions, 0L);
    }

    /**
     * 构建权限树
     */
    private List<Permission> buildPermissionTree(List<Permission> permissions, Long parentId) {
        List<Permission> tree = new ArrayList<>();
        
        for (Permission permission : permissions) {
            if (parentId.equals(permission.getParentId())) {
                List<Permission> children = buildPermissionTree(permissions, permission.getId());
                if (!children.isEmpty()) {
                    permission.setChildren(children);
                }
                tree.add(permission);
            }
        }
        
        return tree;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        Set<String> userPermissions = getPermissionCodesByUserId(userId);
        return userPermissions.contains(permissionCode);
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        Set<String> userPermissions = getPermissionCodesByUserId(userId);
        return Arrays.stream(permissionCodes)
                .anyMatch(userPermissions::contains);
    }

    @Override
    public boolean hasAllPermissions(Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        Set<String> userPermissions = getPermissionCodesByUserId(userId);
        return Arrays.stream(permissionCodes)
                .allMatch(userPermissions::contains);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) {
        // 删除原有权限
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        
        // 添加新权限
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permissionId);
                        return rp;
                    })
                    .collect(Collectors.toList());
            
            rolePermissionMapper.insertBatch(rolePermissions);
        }
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId));
        
        return rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
    }
}
