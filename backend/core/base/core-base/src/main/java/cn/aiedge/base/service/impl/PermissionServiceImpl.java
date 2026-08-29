package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.entity.SysRolePermission;
import cn.aiedge.base.mapper.PermissionMapper;
import cn.aiedge.base.mapper.SysRolePermissionMapper;
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
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, SysPermission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<PermissionDetailVO> pageList(PermissionQueryRequest request) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();

        Integer permissionType = convertPermissionType(request.getPermissionType());

        wrapper.like(StringUtils.hasText(request.getPermissionCode()), SysPermission::getPermissionCode, request.getPermissionCode())
               .like(StringUtils.hasText(request.getPermissionName()), SysPermission::getPermissionName, request.getPermissionName())
               .eq(permissionType != null, SysPermission::getPermissionType, permissionType)
               .eq(request.getStatus() != null, SysPermission::getStatus, request.getStatus())
               .eq(request.getParentId() != null, SysPermission::getParentId, request.getParentId())
               .orderByAsc(SysPermission::getSort)
               .orderByDesc(SysPermission::getCreateTime);

        Page<SysPermission> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<SysPermission> result = page(page, wrapper);

        List<PermissionDetailVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public List<PermissionDetailVO> getTree() {
        // 查询所有权限
        List<SysPermission> allPermissions = lambdaQuery()
                .orderByAsc(SysPermission::getSort)
                .list();

        // 构建树形结构
        return buildTree(allPermissions, 0L);
    }

    @Override
    public List<PermissionDetailVO> getUserMenuTree(Long userId) {
        // 查询用户的菜单权限
        List<SysPermission> menuPermissions = permissionMapper.selectMenuPermissions(userId);

        // 构建树形结构
        return buildTree(menuPermissions, 0L);
    }

    @Override
    public PermissionDetailVO getDetail(Long id) {
        SysPermission permission = getById(id);
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }

        PermissionDetailVO vo = convertToVO(permission);

        // 查询父权限名称
        if (permission.getParentId() != null && permission.getParentId() > 0) {
            SysPermission parent = getById(permission.getParentId());
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

        SysPermission permission = new SysPermission();
        BeanUtils.copyProperties(request, permission);
        permission.setPermissionType(convertPermissionType(request.getPermissionType()));
        if (request.getVisible() != null) {
            permission.setVisible(request.getVisible() ? 1 : 0);
        }

        save(permission);

        log.info("创建权限成功: {}", permission.getPermissionCode());
        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PermissionUpdateRequest request) {
        SysPermission permission = getById(request.getId());
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }

        BeanUtils.copyProperties(request, permission);
        if (request.getVisible() != null) {
            permission.setVisible(request.getVisible() ? 1 : 0);
        }
        updateById(permission);

        log.info("更新权限成功: {}", permission.getPermissionCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysPermission permission = getById(id);
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }

        // 检查是否有子权限
        long childCount = lambdaQuery()
                .eq(SysPermission::getParentId, id)
                .count();

        if (childCount > 0) {
            throw BusinessException.badRequest("存在子权限，无法删除");
        }

        // 检查是否有角色关联
        long roleCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, id)
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
                .in(SysPermission::getParentId, ids)
                .count();

        if (childCount > 0) {
            throw BusinessException.badRequest("部分权限存在子权限，无法删除");
        }

        // 检查是否有角色关联
        long roleCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getPermissionId, ids)
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
        SysPermission permission = getById(id);
        if (permission == null) {
            throw BusinessException.notFound("权限不存在");
        }

        permission.setStatus(status);
        updateById(permission);

        log.info("更新权限状态成功: {} -> {}", permission.getPermissionCode(), status);
    }

    @Override
    public SysPermission getByPermissionCode(String permissionCode) {
        return permissionMapper.selectByPermissionCode(permissionCode);
    }

    @Override
    public List<SysPermission> getByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    @Override
    public List<SysPermission> getByRoleId(Long roleId) {
        return permissionMapper.selectByRoleId(roleId);
    }

    @Override
    public List<SysPermission> getByParentId(Long parentId) {
        return permissionMapper.selectByParentId(parentId);
    }

    /**
     * 转换为VO
     */
    private PermissionDetailVO convertToVO(SysPermission permission) {
        PermissionDetailVO vo = new PermissionDetailVO();
        BeanUtils.copyProperties(permission, vo);
        vo.setPermissionType(convertPermissionType(permission.getPermissionType()));
        if (permission.getVisible() != null) {
            vo.setVisible(permission.getVisible() == 1);
        }
        return vo;
    }

    /**
     * 构建树形结构
     */
    private List<PermissionDetailVO> buildTree(List<SysPermission> permissions, Long parentId) {
        List<PermissionDetailVO> tree = new ArrayList<>();

        for (SysPermission permission : permissions) {
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

    /**
     * 转换权限类型字符串为数值
     * menu-菜单(1)、button-按钮(2)、api-API接口(3)
     */
    private Integer convertPermissionType(String permissionType) {
        if (permissionType == null) {
            return null;
        }
        switch (permissionType) {
            case "menu":
                return 1;
            case "button":
                return 2;
            case "api":
                return 3;
            default:
                return null;
        }
    }

    /**
     * 转换权限类型数值为字符串
     * 1-菜单、2-按钮、3-API
     */
    private String convertPermissionType(Integer permissionType) {
        if (permissionType == null) {
            return null;
        }
        switch (permissionType) {
            case 1:
                return "menu";
            case 2:
                return "button";
            case 3:
                return "api";
            default:
                return null;
        }
    }
}
