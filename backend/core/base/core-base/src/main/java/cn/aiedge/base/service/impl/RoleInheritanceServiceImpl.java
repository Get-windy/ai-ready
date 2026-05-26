package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.RoleInheritance;
import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysRolePermission;
import cn.aiedge.base.mapper.RoleInheritanceMapper;
import cn.aiedge.base.mapper.SysRoleMapper;
import cn.aiedge.base.mapper.SysRolePermissionMapper;
import cn.aiedge.base.service.RoleInheritanceService;
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
import java.util.stream.Collectors;

/**
 * 角色继承关系服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleInheritanceServiceImpl extends ServiceImpl<RoleInheritanceMapper, RoleInheritance>
        implements RoleInheritanceService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRoleInheritance(Long parentRoleId, Long childRoleId, Integer inheritanceType) {
        // 参数校验
        if (parentRoleId == null || childRoleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }

        if (parentRoleId.equals(childRoleId)) {
            throw new IllegalArgumentException("不能设置角色继承自身");
        }

        // 检查角色是否存在
        SysRole parentRole = sysRoleMapper.selectById(parentRoleId);
        SysRole childRole = sysRoleMapper.selectById(childRoleId);
        if (parentRole == null || childRole == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查是否已经存在继承关系
        LambdaQueryWrapper<RoleInheritance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleInheritance::getParentRoleId, parentRoleId)
               .eq(RoleInheritance::getChildRoleId, childRoleId);
        RoleInheritance existing = getOne(wrapper);
        
        if (existing != null) {
            // 更新现有关系
            existing.setInheritanceType(inheritanceType);
            updateById(existing);
        } else {
            // 创建新的继承关系
            RoleInheritance inheritance = new RoleInheritance();
            inheritance.setParentRoleId(parentRoleId);
            inheritance.setChildRoleId(childRoleId);
            inheritance.setTenantId(parentRole.getTenantId()); // 使用父角色的租户ID
            inheritance.setInheritanceType(inheritanceType != null ? inheritanceType : 1); // 默认完全继承
            inheritance.setCreateBy(StpUtil.getLoginIdAsLong());
            inheritance.setCreateTime(LocalDateTime.now());
            save(inheritance);
        }

        log.info("设置角色继承关系成功: parentRoleId={}, childRoleId={}, inheritanceType={}", 
                 parentRoleId, childRoleId, inheritanceType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetRoleInheritance(Long parentRoleId, List<Long> childRoleIds, Integer inheritanceType) {
        if (childRoleIds == null || childRoleIds.isEmpty()) {
            return;
        }

        for (Long childRoleId : childRoleIds) {
            setRoleInheritance(parentRoleId, childRoleId, inheritanceType);
        }

        log.info("批量设置角色继承关系成功: parentRoleId={}, childRoleIds={}, count={}", 
                 parentRoleId, childRoleIds, childRoleIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleInheritance(Long parentRoleId, Long childRoleId) {
        LambdaQueryWrapper<RoleInheritance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleInheritance::getParentRoleId, parentRoleId)
               .eq(RoleInheritance::getChildRoleId, childRoleId);
        
        remove(wrapper);
        log.info("删除角色继承关系成功: parentRoleId={}, childRoleId={}", parentRoleId, childRoleId);
    }

    @Override
    public List<Long> getParentRoleIds(Long childRoleId) {
        LambdaQueryWrapper<RoleInheritance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleInheritance::getChildRoleId, childRoleId);
        List<RoleInheritance> inheritances = list(wrapper);
        
        return inheritances.stream()
                .map(RoleInheritance::getParentRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getChildRoleIds(Long parentRoleId) {
        LambdaQueryWrapper<RoleInheritance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleInheritance::getParentRoleId, parentRoleId);
        List<RoleInheritance> inheritances = list(wrapper);
        
        return inheritances.stream()
                .map(RoleInheritance::getChildRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getAllRolePermissionsWithInheritance(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }

        // 获取角色自身的权限
        List<Long> rolePermissions = sysRoleMapper.selectPermissionIdsByRoleId(roleId);

        // 获取父角色的权限（递归获取）
        List<Long> parentRoleIds = getParentRoleIds(roleId);
        for (Long parentRoleId : parentRoleIds) {
            List<Long> parentPermissions = getAllRolePermissionsWithInheritance(parentRoleId);
            rolePermissions.addAll(parentPermissions);
        }

        // 去重并返回
        return rolePermissions.stream()
                .distinct()
                .collect(Collectors.toList());
    }
}