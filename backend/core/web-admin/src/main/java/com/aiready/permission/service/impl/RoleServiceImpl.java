package com.aiready.permission.service.impl;

import com.aiready.permission.entity.Role;
import com.aiready.permission.entity.UserRole;
import com.aiready.permission.mapper.RoleMapper;
import com.aiready.permission.mapper.UserRoleMapper;
import com.aiready.permission.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public Set<String> getRoleCodesByUserId(Long userId) {
        List<Role> roles = getRolesByUserId(userId);
        return roles.stream()
                .map(Role::getRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        Set<String> userRoles = getRoleCodesByUserId(userId);
        return userRoles.contains(roleCode);
    }

    @Override
    public boolean hasAnyRole(Long userId, String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) {
            return true;
        }
        Set<String> userRoles = getRoleCodesByUserId(userId);
        return Arrays.stream(roleCodes)
                .anyMatch(userRoles::contains);
    }

    @Override
    public boolean hasAllRoles(Long userId, String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) {
            return true;
        }
        Set<String> userRoles = getRoleCodesByUserId(userId);
        return Arrays.stream(roleCodes)
                .allMatch(userRoles::contains);
    }

    @Override
    public Integer getDataScopeByUserId(Long userId) {
        List<Role> roles = getRolesByUserId(userId);
        
        if (CollectionUtils.isEmpty(roles)) {
            return 4; // 默认仅本人数据
        }
        
        // 取最大权限范围（数值越小权限越大）
        return roles.stream()
                .mapToInt(Role::getDataScope)
                .min()
                .orElse(4);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        
        // 添加新角色
        if (!CollectionUtils.isEmpty(roleIds)) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRole ur = new UserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        return ur;
                    })
                    .collect(Collectors.toList());
            
            userRoleMapper.insertBatch(userRoles);
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId));
        
        return userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public Role getByRoleCode(String roleCode) {
        return getOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode));
    }
}
