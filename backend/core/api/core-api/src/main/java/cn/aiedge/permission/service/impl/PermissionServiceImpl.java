package cn.aiedge.permission.service.impl;

import cn.aiedge.base.entity.Role;
import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.entity.SysRolePermission;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.mapper.RoleMapper;
import cn.aiedge.base.mapper.SysPermissionMapper;
import cn.aiedge.base.mapper.SysRolePermissionMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.permission.service.PermissionService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 * 基于 Sa-Token 和 Redis 缓存
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final RoleMapper roleMapper;
    
    private static final String PERMISSION_CACHE_KEY = "permission:user:";
    private static final String ROLE_CACHE_KEY = "role:user:";
    private static final long CACHE_EXPIRE_HOURS = 2;

    // ==================== 当前用户信息 ====================

    @Override
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long getCurrentTenantId() {
        try {
            Object tenantId = StpUtil.getSession().get("tenantId");
            return tenantId != null ? Long.valueOf(tenantId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long getCurrentUserDeptId() {
        try {
            Object deptId = StpUtil.getSession().get("deptId");
            return deptId != null ? Long.valueOf(deptId.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<Long> getCurrentUserDeptAndChildIds() {
        try {
            Object deptIds = StpUtil.getSession().get("deptAndChildIds");
            if (deptIds instanceof Set) {
                return (Set<Long>) deptIds;
            }
        } catch (Exception e) {
            log.debug("获取用户部门及子部门失败: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    @Override
    public Integer getCurrentUserDataScope() {
        try {
            Object dataScope = StpUtil.getSession().get("dataScope");
            return dataScope != null ? Integer.valueOf(dataScope.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 权限查询 ====================

    @Override
    public Set<String> getCurrentUserPermissions() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        
        List<String> permissions = getUserPermissionCodes(userId);
        return new HashSet<>(permissions);
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        
        List<String> roles = getUserRoleCodes(userId);
        return new HashSet<>(roles);
    }

    @Override
    public boolean isSuperAdmin() {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains("SUPER_ADMIN") || roles.contains("admin") || roles.contains("super_admin");
    }

    @Override
    public boolean hasPermission(String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            return true;
        }
        
        if (isSuperAdmin()) {
            return true;
        }
        
        return getCurrentUserPermissions().contains(permissionCode);
    }

    @Override
    public boolean hasRole(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return true;
        }
        
        if (isSuperAdmin()) {
            return true;
        }
        
        return getCurrentUserRoles().contains(roleCode);
    }

    // ==================== 用户权限管理 ====================

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        // 尝试从缓存获取
        String cacheKey = PERMISSION_CACHE_KEY + userId;
        List<String> cached = getCachedPermissions(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从 Sa-Token 获取权限列表
        List<String> permissions = StpUtil.getPermissionList(userId);
        
        // 缓存结果
        cachePermissions(cacheKey, permissions);
        
        return permissions != null ? permissions : Collections.emptyList();
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        // 尝试从缓存获取
        String cacheKey = ROLE_CACHE_KEY + userId;
        List<String> cached = getCachedPermissions(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从 Sa-Token 获取角色列表
        List<String> roles = StpUtil.getRoleList(userId);
        
        // 缓存结果
        cachePermissions(cacheKey, roles);
        
        return roles != null ? roles : Collections.emptyList();
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        try {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, userId));

            return userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询用户角色ID列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, Long tenantId, List<Long> roleIds) {
        if (userId == null || tenantId == null || CollUtil.isEmpty(roleIds)) {
            log.warn("分配用户角色参数不完整: userId={}, tenantId={}, roleIds={}", userId, tenantId, roleIds);
            return;
        }

        // 角色作用域校验
        try {
            Collection<Role> roles = roleMapper.selectBatchIds(roleIds);
            if (roles.size() != roleIds.size()) {
                log.warn("部分角色不存在: roleIds={}", roleIds);
                throw new RuntimeException("部分角色不存在");
            }
            Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getId, r -> r));
            boolean hasTenantContext = tenantId != null;
            for (Long roleId : roleIds) {
                Role role = roleMap.get(roleId);
                if (role == null) continue;
                if ("PLATFORM".equals(role.getScope()) && hasTenantContext) {
                    throw new RuntimeException("不能将平台级角色「" + role.getRoleName() + "」分配给租户用户");
                }
                if ("TENANT".equals(role.getScope()) && !hasTenantContext) {
                    throw new RuntimeException("不能将租户级角色「" + role.getRoleName() + "」分配给平台用户");
                }
            }
        } catch (RuntimeException e) {
            log.warn("角色作用域校验失败: {}", e.getMessage());
            throw e;
        }

        try {
            // 1. 删除该用户在该租户下的现有角色关联
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getTenantId, tenantId));

            // 2. 插入新的用户角色关联
            List<SysUserRole> userRoles = roleIds.stream()
                    .map(roleId -> new SysUserRole()
                            .setUserId(userId)
                            .setTenantId(tenantId)
                            .setRoleId(roleId))
                    .collect(Collectors.toList());

            for (SysUserRole userRole : userRoles) {
                sysUserRoleMapper.insert(userRole);
            }

            // 3. 刷新权限缓存
            refreshUserPermissionCache(userId);
            log.info("用户角色分配完成: userId={}, tenantId={}, roleIds={}", userId, tenantId, roleIds);
        } catch (Exception e) {
            log.error("分配用户角色失败: userId={}, tenantId={}, error={}", userId, tenantId, e.getMessage(), e);
            throw new RuntimeException("分配用户角色失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUserRoles(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            // 删除该用户的所有角色关联
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId));

            refreshUserPermissionCache(userId);
            log.info("用户角色已清除: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户角色失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("清除用户角色失败: " + e.getMessage(), e);
        }
    }

    // ==================== 角色权限管理 ====================

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }

        try {
            List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(
                    new LambdaQueryWrapper<SysRolePermission>()
                            .eq(SysRolePermission::getRoleId, roleId));

            return rolePermissions.stream()
                    .map(SysRolePermission::getPermissionId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询角色权限ID列表失败: roleId={}, error={}", roleId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolePermissions(Long roleId, Long tenantId, List<Long> permissionIds) {
        if (roleId == null || tenantId == null || CollUtil.isEmpty(permissionIds)) {
            log.warn("分配角色权限参数不完整: roleId={}, tenantId={}, permissionIds={}", roleId, tenantId, permissionIds);
            return;
        }

        try {
            // 1. 删除该角色在该租户下的现有权限关联
            sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, roleId)
                    .eq(SysRolePermission::getTenantId, tenantId));

            // 2. 插入新的角色权限关联
            List<SysRolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> new SysRolePermission()
                            .setRoleId(roleId)
                            .setTenantId(tenantId)
                            .setPermissionId(permissionId))
                    .collect(Collectors.toList());

            for (SysRolePermission rolePermission : rolePermissions) {
                sysRolePermissionMapper.insert(rolePermission);
            }

            // 3. 清除拥有该角色的所有用户的权限缓存
            clearAffectedUserCaches(roleId);

            log.info("角色权限分配完成: roleId={}, tenantId={}, permissionIds={}", roleId, tenantId, permissionIds);
        } catch (Exception e) {
            log.error("分配角色权限失败: roleId={}, tenantId={}, error={}", roleId, tenantId, e.getMessage(), e);
            throw new RuntimeException("分配角色权限失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRolePermissions(Long roleId) {
        if (roleId == null) {
            return;
        }

        try {
            // 删除该角色的所有权限关联
            sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, roleId));

            // 清除拥有该角色的所有用户的权限缓存
            clearAffectedUserCaches(roleId);

            log.info("角色权限已清除: roleId={}", roleId);
        } catch (Exception e) {
            log.error("清除角色权限失败: roleId={}, error={}", roleId, e.getMessage(), e);
            throw new RuntimeException("清除角色权限失败: " + e.getMessage(), e);
        }
    }

    // ==================== 权限验证 ====================

    @Override
    public boolean checkApiPermission(Long userId, String apiPath, String method) {
        if (userId == null || StrUtil.isBlank(apiPath)) {
            return false;
        }

        // 超级管理员有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        try {
            // 1. 查询用户的角色ID列表
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, userId));

            if (CollUtil.isEmpty(userRoles)) {
                log.debug("用户 {} 没有分配任何角色", userId);
                return false;
            }

            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .distinct()
                    .collect(Collectors.toList());

            // 2. 查询这些角色拥有的权限ID列表
            List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(
                    new LambdaQueryWrapper<SysRolePermission>()
                            .in(SysRolePermission::getRoleId, roleIds));

            if (CollUtil.isEmpty(rolePermissions)) {
                log.debug("用户 {} 的角色没有任何权限", userId);
                return false;
            }

            List<Long> permissionIds = rolePermissions.stream()
                    .map(SysRolePermission::getPermissionId)
                    .distinct()
                    .collect(Collectors.toList());

            // 3. 查询权限表中匹配API路径和方法的记录
            List<SysPermission> apiPermissions = sysPermissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermission>()
                            .in(SysPermission::getId, permissionIds)
                            .eq(SysPermission::getPermissionType, 3)  // 类型3为API权限
                            .eq(SysPermission::getApiPath, apiPath)
                            .eq(SysPermission::getStatus, 0));  // 状态0为正常

            // 如果指定了HTTP方法，进一步过滤
            if (StrUtil.isNotBlank(method)) {
                apiPermissions = apiPermissions.stream()
                        .filter(p -> p.getMethod() != null
                                && p.getMethod().equalsIgnoreCase(method))
                        .collect(Collectors.toList());
            }

            boolean hasPermission = !apiPermissions.isEmpty();
            log.debug("API权限检查: userId={}, apiPath={}, method={}, result={}",
                    userId, apiPath, method, hasPermission);
            return hasPermission;
        } catch (Exception e) {
            log.error("检查API权限失败: userId={}, apiPath={}, method={}, error={}",
                    userId, apiPath, method, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean checkDataPermission(Long userId, Long dataTenantId, Long dataCreateBy) {
        if (userId == null) {
            return false;
        }

        // 超级管理员有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        // 检查租户权限
        if (dataTenantId != null && !checkTenantPermission(userId, dataTenantId)) {
            return false;
        }

        // 检查数据权限范围
        Integer dataScope = getCurrentUserDataScope();
        if (dataScope == null || dataScope == 0) {
            return true; // 0: 全部数据权限
        }

        if (dataScope == 3) {
            // 3: 仅本人数据权限
            return dataCreateBy != null && userId.equals(dataCreateBy);
        }

        if (dataScope == 1 || dataScope == 2) {
            // 1: 本部门数据权限
            // 2: 本部门及以下数据权限
            Long userDeptId = getCurrentUserDeptId();
            if (userDeptId == null) {
                log.debug("用户 {} 没有部门信息，无法进行部门级别数据权限检查", userId);
                return false;
            }

            if (dataScope == 1) {
                // 仅限本部门
                return userDeptId.equals(dataCreateBy);
            }

            if (dataScope == 2) {
                // 本部门及子部门
                Set<Long> deptAndChildIds = getCurrentUserDeptAndChildIds();
                return deptAndChildIds.contains(dataCreateBy);
            }
        }

        // 4: 自定义（需要额外逻辑）
        log.debug("自定义数据权限范围，默认放行: userId={}, dataScope={}", userId, dataScope);
        return true;
    }

    @Override
    public boolean checkTenantPermission(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        
        // 超级管理员有所有权限
        if (isSuperAdmin()) {
            return true;
        }
        
        // 普通用户只能访问自己所属租户
        Long userTenantId = getCurrentTenantId();
        return tenantId.equals(userTenantId);
    }

    // ==================== 缓存管理 ====================

    @Override
    public void refreshUserPermissionCache(Long userId) {
        if (userId == null) {
            return;
        }
        
        // 清除Redis缓存
        redisTemplate.delete(PERMISSION_CACHE_KEY + userId);
        redisTemplate.delete(ROLE_CACHE_KEY + userId);
        
        // 清除Sa-Token会话缓存
        try {
            StpUtil.getSessionByLoginId(userId).delete("Permission_List");
            StpUtil.getSessionByLoginId(userId).delete("Role_List");
        } catch (Exception e) {
            log.debug("清除Sa-Token缓存失败: {}", e.getMessage());
        }
        
        log.info("用户 {} 权限缓存已刷新", userId);
    }

    @Override
    public void clearPermissionCache() {
        // 清除所有权限缓存
        Set<String> keys = redisTemplate.keys(PERMISSION_CACHE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        Set<String> roleKeys = redisTemplate.keys(ROLE_CACHE_KEY + "*");
        if (roleKeys != null && !roleKeys.isEmpty()) {
            redisTemplate.delete(roleKeys);
        }
        
        log.info("权限缓存已全部清除");
    }

    // ==================== 私有方法 ====================

    @SuppressWarnings("unchecked")
    private List<String> getCachedPermissions(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.debug("获取缓存失败: {}", e.getMessage());
        }
        return null;
    }

    private void cachePermissions(String cacheKey, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("缓存权限失败: {}", e.getMessage());
        }
    }

    /**
     * 清除所有拥有指定角色的用户的权限缓存
     * 当角色的权限变更时调用，确保相关用户的权限缓存同步更新
     */
    private void clearAffectedUserCaches(Long roleId) {
        if (roleId == null) {
            return;
        }

        try {
            // 查询拥有该角色的所有用户
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getRoleId, roleId));

            if (CollUtil.isEmpty(userRoles)) {
                return;
            }

            // 为每个受影响的用户刷新缓存
            Set<Long> affectedUserIds = userRoles.stream()
                    .map(SysUserRole::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (Long userId : affectedUserIds) {
                refreshUserPermissionCache(userId);
            }

            log.info("角色 {} 权限变更，已刷新 {} 个用户的权限缓存", roleId, affectedUserIds.size());
        } catch (Exception e) {
            log.warn("清除受影响用户缓存失败: roleId={}, error={}", roleId, e.getMessage());
        }
    }
}
