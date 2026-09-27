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
import org.springframework.context.annotation.Primary;
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
@Primary
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
        // 与 listAllPermissions 同口径：平台级(0) + 本租户 —— 否则懒加载子节点同样恒空
        wrapper.eq(SysPermission::getParentId, parentId)
               .in(SysPermission::getTenantId,
                       tenantId == null || tenantId == 0L ? List.of(0L) : List.of(0L, tenantId))
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
     * 获取所有权限列表（**平台级 tenant_id=0 + 本租户自建**）。
     *
     * <p>⚠️ 2026-09-26 修复：原先只取 {@code tenant_id = tenantId} 精确匹配，而平台定义的全部
     * 权限码（含 {@code dms:*} / {@code sale:*} / {@code purchase:*} 等 1464 条）的
     * {@code tenant_id} 恒为 0 ⇒ 租户管理员打开「角色 → 分配权限」时清单**恒为空**，
     * 根本无法给本租户角色配任何平台权限码。这正是「平台开模块 → 租户自助配权限」
     * 这条两层设计链路断在**最后一环**的原因（配送模块审计 §4.1 实证：
     * {@code GET /permission/tree?tenantId=2} 返回 0 条，而库中 tenant_id=0 的有 1464 条）。</p>
     *
     * <p>租户插件对 {@code sys_permission} 已忽略（见 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}：
     * 「权限定义系统级」），故此处是唯一过滤条件。<b>本改动只增不减</b>：原能看到的本租户
     * 权限码依旧可见，新增平台级；{@code tenantId=0}（平台视角）时行为完全不变。</p>
     */
    private List<SysPermission> listAllPermissions(Long tenantId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysPermission::getTenantId,
                        tenantId == null || tenantId == 0L ? List.of(0L) : List.of(0L, tenantId))
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