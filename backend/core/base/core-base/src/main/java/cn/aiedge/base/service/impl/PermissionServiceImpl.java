package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.Permission;
import cn.aiedge.base.entity.RolePermission;
import cn.aiedge.base.mapper.PermissionMapper;
import cn.aiedge.base.mapper.RolePermissionMapper;
import cn.aiedge.base.service.PermissionService;
import cn.aiedge.common.dto.permission.*;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<PermissionDetailVO> pageList(PermissionQueryRequest request) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.like(StringUtils.hasText(request.getPermissionCode()), Permission::getPermissionCode, request.getPermissionCode())
               .like(StringUtils.hasText(request.getPermissionName()), Permission::getPermissionName, request.getPermissionName())
               .eq(StringUtils.hasText(request.getPermissionType()), Permission::getPermissionType, request.getPermissionType())
               .eq(request.getStatus() != null, Permission::getStatus, request.getStatus())
               .eq(request.getParentId() != null, Permission::getParentId, request.getParentId())
               .orderByAsc(Permission::getSort)
               .orderByDesc(Permission::getCreateTime);

        Page<Permission> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Permission> result = page(page, wrapper);

        List<PermissionDetailVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public List<PermissionDetailVO> getTree() {
        // 查询所有权限
        List<Permission> allPermissions = lambdaQuery()
                .orderByAsc(Permission::getSort)
                .list();
        
        // 构建树形结构
        return buildTree(allPermissions, 0L);
    }

    @Override
    public List<PermissionDetailVO> getUserMenuTree(Long userId) {
        // 查询用户的菜单权限
        List<Permission> menuPermissions = permissionMapper.selectMenuPermissions(userId);
        
        // 构建树形结构
        return buildTree(menuPermissions, 0L);
    }

    @Override
    public PermissionDetailVO getDetail(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }
        
        PermissionDetailVO vo = convertToVO(permission);
        
        // 查询父权限名称
        if (permission.getParentId() != null && permission.getParentId() > 0) {
            Permission parent = getById(permission.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getPermissionName());
            }
        }
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PermissionCreateRequest request) {
        // 校验权限编码唯一性
        if (getByPermissionCode(request.getPermissionCode()) != null) {
            throw BusinessException.badRequest("权限编码已存在");
        }
        
        Permission permission = new Permission();
        BeanUtils.copyProperties(request, permission);
        
        save(permission);
        
        log.info("创建权限成功: {}", permission.getPermissionCode());
        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PermissionUpdateRequest request) {
        Permission permission = getById(request.getId());
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }
        
        BeanUtils.copyProperties(request, permission);
        updateById(permission);
        
        log.info("更新权限成功: {}", permission.getPermissionCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }
        
        // 检查是否有子权限
        long childCount = lambdaQuery()
                .eq(Permission::getParentId, id)
                .count();
        
        if (childCount > 0) {
            throw BusinessException.badRequest("存在子权限，无法删除");
        }
        
        // 检查是否有角色关联
        long roleCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, id)
        );
        
        if (roleCount > 0) {
            throw BusinessException.badRequest("该权限已分配给角色，无法删除");
        }
        
        // 删除权限
        removeById(id);
        
        log.info("删除权限成功: {}", permission.getPermissionCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        
        // 检查是否有子权限
        long childCount = lambdaQuery()
                .in(Permission::getParentId, ids)
                .count();
        
        if (childCount > 0) {
            throw BusinessException.badRequest("部分权限存在子权限，无法删除");
        }
        
        // 检查是否有角色关联
        long roleCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getPermissionId, ids)
        );
        
        if (roleCount > 0) {
            throw BusinessException.badRequest("部分权限已分配给角色，无法删除");
        }
        
        // 批量删除权限
        removeByIds(ids);
        
        log.info("批量删除权限成功: {} 个", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Permission permission = getById(id);
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }
        
        permission.setStatus(status);
        updateById(permission);
        
        log.info("更新权限状态成功: {} -> {}", permission.getPermissionCode(), status);
    }

    @Override
    public Permission getByPermissionCode(String permissionCode) {
        return permissionMapper.selectByPermissionCode(permissionCode);
    }

    @Override
    public List<Permission> getByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    @Override
    public List<Permission> getByRoleId(Long roleId) {
        return permissionMapper.selectByRoleId(roleId);
    }

    @Override
    public List<Permission> getByParentId(Long parentId) {
        return permissionMapper.selectByParentId(parentId);
    }

    /**
     * 转换为VO
     */
    private PermissionDetailVO convertToVO(Permission permission) {
        PermissionDetailVO vo = new PermissionDetailVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }

    /**
     * 构建树形结构
     */
    private List<PermissionDetailVO> buildTree(List<Permission> permissions, Long parentId) {
        List<PermissionDetailVO> tree = new ArrayList<>();
        
        for (Permission permission : permissions) {
            if ((parentId == null && permission.getParentId() == null) ||
                (parentId != null && parentId.equals(permission.getParentId()))) {
                
                PermissionDetailVO vo = convertToVO(permission);
                
                // 递归查找子节点
                List<PermissionDetailVO> children = buildTree(permissions, permission.getId());
                if (!children.isEmpty()) {
                    vo.setChildren(children);
                }
                
                tree.add(vo);
            }
        }
        
        return tree;
    }
}
