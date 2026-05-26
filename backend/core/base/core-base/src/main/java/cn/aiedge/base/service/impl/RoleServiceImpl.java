package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.Permission;
import cn.aiedge.base.entity.Role;
import cn.aiedge.base.entity.RolePermission;
import cn.aiedge.base.entity.UserRole;
import cn.aiedge.base.mapper.PermissionMapper;
import cn.aiedge.base.mapper.RoleMapper;
import cn.aiedge.base.mapper.RolePermissionMapper;
import cn.aiedge.base.mapper.UserRoleMapper;
import cn.aiedge.base.service.RoleService;
import cn.aiedge.common.dto.role.*;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public PageResult<RoleDetailVO> pageList(RoleQueryRequest request) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.like(StringUtils.hasText(request.getRoleCode()), Role::getRoleCode, request.getRoleCode())
               .like(StringUtils.hasText(request.getRoleName()), Role::getRoleName, request.getRoleName())
               .eq(StringUtils.hasText(request.getRoleType()), Role::getRoleType, request.getRoleType())
               .eq(request.getStatus() != null, Role::getStatus, request.getStatus())
               .orderByAsc(Role::getSort)
               .orderByDesc(Role::getCreateTime);

        Page<Role> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Role> result = page(page, wrapper);

        List<RoleDetailVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public List<RoleDetailVO> listAll() {
        List<Role> roles = lambdaQuery()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getSort)
                .list();
        
        return roles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDetailVO getDetail(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        
        RoleDetailVO vo = convertToVO(role);
        
        // 查询角色权限
        List<Permission> permissions = permissionMapper.selectByRoleId(id);
        vo.setPermissions(permissions.stream().map(this::convertToPermissionVO).collect(Collectors.toList()));
        vo.setPermissionIds(permissions.stream().map(Permission::getId).collect(Collectors.toList()));
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest request) {
        // 校验角色编码唯一性
        if (getByRoleCode(request.getRoleCode()) != null) {
            throw BusinessException.badRequest("角色编码已存在");
        }
        
        Role role = new Role();
        BeanUtils.copyProperties(request, role);
        
        save(role);
        
        // 分配权限
        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }
        
        log.info("创建角色成功: {}", role.getRoleCode());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleUpdateRequest request) {
        Role role = getById(request.getId());
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        
        BeanUtils.copyProperties(request, role);
        updateById(role);
        
        // 更新权限
        if (request.getPermissionIds() != null) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }
        
        log.info("更新角色成功: {}", role.getRoleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        
        // 检查是否有用户关联
        long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id)
        );
        
        if (userCount > 0) {
            throw BusinessException.badRequest("该角色已分配给用户，无法删除");
        }
        
        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(id);
        
        // 删除角色
        removeById(id);
        
        log.info("删除角色成功: {}", role.getRoleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        
        // 检查是否有用户关联
        long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, ids)
        );
        
        if (userCount > 0) {
            throw BusinessException.badRequest("部分角色已分配给用户，无法删除");
        }
        
        // 删除角色权限关联
        ids.forEach(rolePermissionMapper::deleteByRoleId);
        
        // 批量删除角色
        removeByIds(ids);
        
        log.info("批量删除角色成功: {} 个", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Role role = getById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        
        role.setStatus(status);
        updateById(role);
        
        log.info("更新角色状态成功: {} -> {}", role.getRoleCode(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 删除原有权限
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 添加新权限
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permissionId);
                        rp.setCreateTime(LocalDateTime.now());
                        return rp;
                    })
                    .collect(Collectors.toList());
            rolePermissionMapper.batchInsert(rolePermissions);
        }
        
        log.info("分配权限成功: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    @Override
    public Role getByRoleCode(String roleCode) {
        return roleMapper.selectByRoleCode(roleCode);
    }

    @Override
    public List<Role> getByUserId(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 转换为VO
     */
    private RoleDetailVO convertToVO(Role role) {
        RoleDetailVO vo = new RoleDetailVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }

    /**
     * 转换为权限VO
     */
    private PermissionVO convertToPermissionVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }
}
