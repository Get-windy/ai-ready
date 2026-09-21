package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.*;
import cn.aiedge.base.mapper.*;
import cn.aiedge.base.security.StpInterfaceImpl;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.service.RoleService;
import cn.aiedge.base.service.SysOperLogService;
import cn.aiedge.common.dto.role.*;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, SysRole> implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final StpInterfaceImpl stpInterface;
    private final SysOperLogService operLogService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<RoleDetailVO> pageList(RoleQueryRequest request) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        // 根据当前用户上下文自动过滤 scope
        String currentScope = resolveCurrentScope();
        if (currentScope != null) {
            wrapper.eq(SysRole::getScope, currentScope);
        } else if (StringUtils.hasText(request.getScope())) {
            wrapper.eq(SysRole::getScope, request.getScope());
        }

        // 租户隔离：租户用户只能看到自己的角色
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId != null) {
            wrapper.eq(SysRole::getTenantId, tenantId);
        }

        wrapper.like(StringUtils.hasText(request.getRoleCode()), SysRole::getRoleCode, request.getRoleCode())
               .like(StringUtils.hasText(request.getRoleName()), SysRole::getRoleName, request.getRoleName())
               .eq(StringUtils.hasText(request.getRoleType()), SysRole::getRoleType, convertRoleType(request.getRoleType()))
               .eq(request.getStatus() != null, SysRole::getStatus, request.getStatus())
               .orderByAsc(SysRole::getSort)
               .orderByDesc(SysRole::getCreateTime);

        Page<SysRole> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<SysRole> result = page(page, wrapper);

        List<RoleDetailVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override
    public List<RoleDetailVO> listAll(String scope) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort);

        // 优先使用显式传入的 scope，否则根据用户上下文自动推断
        String effectiveScope = scope;
        if (effectiveScope == null) {
            effectiveScope = resolveCurrentScope();
        }
        if (effectiveScope != null) {
            wrapper.eq(SysRole::getScope, effectiveScope);
        }

        // 租户隔离：TENANT 作用域的角色按租户过滤
        if ("TENANT".equals(effectiveScope)) {
            Long tenantId = SecurityUtils.getCurrentTenantId();
            if (tenantId != null) {
                wrapper.eq(SysRole::getTenantId, tenantId);
            }
        }

        List<SysRole> roles = list(wrapper);

        return roles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDetailVO getDetail(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        
        RoleDetailVO vo = convertToVO(role);
        
        // 查询角色权限
        List<SysPermission> permissions = permissionMapper.selectByRoleId(id);
        vo.setPermissions(permissions.stream().map(this::convertToPermissionVO).collect(Collectors.toList()));
        vo.setPermissionIds(permissions.stream().map(SysPermission::getId).collect(Collectors.toList()));
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest request) {
        // 校验角色编码唯一性
        if (getByRoleCode(request.getRoleCode()) != null) {
            throw BusinessException.badRequest("角色编码已存在");
        }

        // scope 隔离校验：租户用户不能创建平台级角色
        String requestedScope = request.getScope();
        String currentScope = resolveCurrentScope();
        if ("PLATFORM".equals(requestedScope) && "TENANT".equals(currentScope)) {
            throw BusinessException.forbidden("租户用户无权创建平台级角色");
        }

        SysRole role = new SysRole();
        // roleType 由 String(admin/user) 转为 Integer(0/1)，需单独转换，故排除自动拷贝
        BeanUtils.copyProperties(request, role, "roleType");
        role.setRoleType(convertRoleType(request.getRoleType()));

        // 自动设置 scope：如果未指定，根据当前用户上下文推断
        if (role.getScope() == null) {
            role.setScope(currentScope != null ? currentScope : "TENANT");
        }

        // 自动设置 tenantId（如果用户有租户上下文）
        if (role.getTenantId() == null) {
            Long tenantId = SecurityUtils.getCurrentTenantId();
            role.setTenantId(tenantId);
        }

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
        SysRole role = getById(request.getId());
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }

        // scope 隔离校验：租户用户不能将角色 scope 改为 PLATFORM
        String currentScope = resolveCurrentScope();
        if ("TENANT".equals(currentScope) && "PLATFORM".equals(request.getScope())) {
            throw BusinessException.forbidden("租户用户无权将角色作用域设置为平台级");
        }

        // roleType 由 String(admin/user) 转为 Integer(0/1)，需单独转换，故排除自动拷贝
        BeanUtils.copyProperties(request, role, "roleType");
        if (request.getRoleType() != null) {
            role.setRoleType(convertRoleType(request.getRoleType()));
        }
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
        SysRole role = getById(id);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }

        // scope 隔离校验：租户用户不能删除平台级角色
        String currentScope = resolveCurrentScope();
        if ("TENANT".equals(currentScope) && "PLATFORM".equals(role.getScope())) {
            throw BusinessException.forbidden("租户用户无权删除平台级角色");
        }

        // 检查是否有用户关联
        long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id)
        );
        
        if (userCount > 0) {
            throw BusinessException.badRequest("该角色已分配给用户，无法删除");
        }
        
        // 删除角色权限关联（sys_role_permission 无 deleted 列 → 物理删除；租户条件由拦截器注入）
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        
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
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, ids)
        );
        
        if (userCount > 0) {
            throw BusinessException.badRequest("部分角色已分配给用户，无法删除");
        }
        
        // 删除角色权限关联（批量，一条 SQL 覆盖全部被删角色）
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .in(SysRolePermission::getRoleId, ids));
        
        // 批量删除角色
        removeByIds(ids);
        
        log.info("批量删除角色成功: {} 个", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysRole role = getById(id);
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
        SysRole role = getById(roleId);
        String roleName = (role != null) ? role.getRoleName() : String.valueOf(roleId);

        // 获取当前已有权限 ID（用于后续 diff 计算）
        List<Long> oldPermissionIds = listRolePermissionIds(roleId);

        // 删除原有权限（覆盖式保存）
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));

        // 添加新权限
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<SysRolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> {
                        SysRolePermission rp = new SysRolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permissionId);
                        rp.setCreateTime(LocalDateTime.now());
                        return rp;
                    })
                    .collect(Collectors.toList());
            // 逐条 insert：BaseMapper 不提供批量插入，且本 Mapper 原有的 batchInsert **从来没有 SQL 绑定**
            // （调用即 Invalid bound statement → 保存恒 500），故此处不再依赖它
            rolePermissions.forEach(rolePermissionMapper::insert);
        }

        // 清除该角色下所有用户的权限缓存
        try {
            List<SysUserRole> userRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
            );
            for (SysUserRole userRole : userRoles) {
                stpInterface.clearUserPermissionCache(userRole.getUserId());
            }
            if (!userRoles.isEmpty()) {
                log.info("清除 {} 个用户的权限缓存", userRoles.size());
            }
        } catch (Exception e) {
            log.warn("清除用户权限缓存异常: roleId={}", roleId, e);
        }

        // 记录权限变更审计日志
        try {
            recordPermissionAuditLog(role, roleName, oldPermissionIds, permissionIds);
        } catch (Exception e) {
            log.warn("记录权限变更审计日志异常: roleId={}", roleId, e);
        }

        log.info("分配权限成功: roleId={}, roleName={}, permissionCount={}",
                roleId, roleName, permissionIds != null ? permissionIds.size() : 0);
    }

    /**
     * 记录权限变更审计日志
     */
    private void recordPermissionAuditLog(SysRole role, String roleName,
                                           List<Long> oldPermissionIds, List<Long> newPermissionIds) {
        Set<Long> oldSet = new HashSet<>(oldPermissionIds != null ? oldPermissionIds : Collections.emptyList());
        Set<Long> newSet = new HashSet<>(newPermissionIds != null ? newPermissionIds : Collections.emptyList());

        List<Long> added = new ArrayList<>(newSet);
        added.removeAll(oldSet);

        List<Long> removed = new ArrayList<>(oldSet);
        removed.removeAll(newSet);

        if (added.isEmpty() && removed.isEmpty()) {
            return; // 无变更，不记录
        }

        Map<String, Object> diffData = new LinkedHashMap<>();
        diffData.put("roleName", roleName);
        diffData.put("roleCode", role != null ? role.getRoleCode() : null);
        diffData.put("addedCount", added.size());
        diffData.put("removedCount", removed.size());
        if (!added.isEmpty()) diffData.put("addedPermissionIds", added);
        if (!removed.isEmpty()) diffData.put("removedPermissionIds", removed);

        try {
            SysOperLog logEntry = new SysOperLog();
            logEntry.setTenantId(SecurityUtils.getCurrentTenantId());
            if (StpUtil.isLogin()) {
                logEntry.setUserId(StpUtil.getLoginIdAsLong());
                logEntry.setUsername(StpUtil.getSession().getString("username"));
            }
            logEntry.setModule("权限管理");
            logEntry.setAction("分配权限");
            logEntry.setMethod("RoleServiceImpl.assignPermissions");
            logEntry.setDiffData(objectMapper.writeValueAsString(diffData));
            logEntry.setStatus(0);
            logEntry.setOperTime(LocalDateTime.now());
            operLogService.recordLogAsync(logEntry);
        } catch (JsonProcessingException e) {
            log.warn("序列化审计日志 diffData 失败", e);
        }
    }

    @Override
    public SysRole getByRoleCode(String roleCode) {
        return roleMapper.selectByRoleCode(roleCode);
    }

    @Override
    public List<SysRole> getByUserId(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        return listRolePermissionIds(roleId);
    }

    /**
     * 读取某角色的权限ID列表。
     * <p>用 {@code LambdaQueryWrapper} 而非 Mapper 自定义方法 —— 后者在本 Mapper 上没有 SQL 绑定
     * （见 {@code SysRolePermissionMapper} 类注释）。</p>
     */
    private List<Long> listRolePermissionIds(Long roleId) {
        List<SysRolePermission> rows = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        List<Long> ids = new ArrayList<>();
        if (rows != null) {
            for (SysRolePermission row : rows) {
                if (row.getPermissionId() != null) {
                    ids.add(row.getPermissionId());
                }
            }
        }
        return ids;
    }

    /**
     * 解析当前用户上下文对应的角色作用域
     * - 平台用户（无租户上下文）→ PLATFORM
     * - 租户用户（有租户上下文）→ TENANT
     * - 无法确定 → null（不限）
     */
    private String resolveCurrentScope() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId != null) {
            return "TENANT";
        }
        // 已登录但无租户ID → 平台管理员
        if (SecurityUtils.isLoggedIn()) {
            return "PLATFORM";
        }
        return null;
    }

    /**
     * 转换为VO
     */
    private RoleDetailVO convertToVO(SysRole role) {
        RoleDetailVO vo = new RoleDetailVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }

    /**
     * 角色类型转换：String("admin"/"user"/"0"/"1") → Integer(0-系统角色 1-自定义角色)
     */
    private Integer convertRoleType(String roleType) {
        if (roleType == null || roleType.isBlank()) {
            return null;
        }
        return switch (roleType.trim()) {
            case "admin" -> 0;
            case "user" -> 1;
            default -> {
                try {
                    yield Integer.valueOf(roleType.trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    /**
     * 转换为权限VO
     */
    private PermissionVO convertToPermissionVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        BeanUtils.copyProperties(permission, vo);
        vo.setPermissionType(convertPermissionType(permission.getPermissionType()));
        return vo;
    }

    /**
     * 转换权限类型数值为字符串
     * 1-菜单、2-按钮、3-API
     */
    private String convertPermissionType(Integer permissionType) {
        if (permissionType == null) {
            return null;
        }
        return switch (permissionType) {
            case 1 -> "menu";
            case 2 -> "button";
            case 3 -> "api";
            default -> null;
        };
    }
}
