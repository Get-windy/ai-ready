package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.Role;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.entity.SysUserTenant;
import cn.aiedge.base.mapper.RoleMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.mapper.SysUserTenantMapper;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.util.PasswordPolicy;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> 
        implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final RoleMapper roleMapper;

    /** 密码最长有效期（天），超过需修改 */
    @Value("${password.policy.max-age-days:90}")
    private int passwordMaxAgeDays;

    @Override
    public String login(String username, String password, Long tenantId, String loginIp) {
        // 1. 查询用户（用户名全局唯一）
        SysUser user = baseMapper.selectByUsername(username, null);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 2. 验证用户是否属于指定租户（通过 sys_user_tenant 关联表）
        if (!isUserInTenant(user.getId(), tenantId)) {
            throw BusinessException.badRequest("该用户不属于此租户，请检查租户名称");
        }

        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "用户已禁用或锁定");
        }

        // 4. 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
        }

        // 5. 登录成功，生成Token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 6. 将租户ID存入Sa-Token Session，避免多租户拦截器递归查询
        StpUtil.getSession().set("tenantId", tenantId);

        // 7. 密码过期检查
        boolean passwordExpired = false;
        if (user.getPasswordUpdateTime() != null && passwordMaxAgeDays > 0) {
            passwordExpired = Duration.between(user.getPasswordUpdateTime(), LocalDateTime.now())
                .toDays() >= passwordMaxAgeDays;
        }
        StpUtil.getSession().set("passwordExpired", passwordExpired);

        // 8. 更新登录信息
        String safeLoginIp = (loginIp != null && !loginIp.isEmpty()) ? loginIp : "0.0.0.0";
        baseMapper.updateLoginInfo(user.getId(), safeLoginIp);

        log.info("用户登录成功: userId={}, username={}, tenantId={}, passwordExpired={}",
            user.getId(), username, tenantId, passwordExpired);
        return token;
    }

    @Override
    public List<SysTenant> getUserTenants(Long userId) {
        return userTenantMapper.selectTenantsByUserId(userId);
    }

    @Override
    public boolean isUserInTenant(Long userId, Long tenantId) {
        SysUserTenant ut = userTenantMapper.selectByUserAndTenant(userId, tenantId);
        return ut != null;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(SysUser user) {
        // 禁止通过 API 创建超级管理员或租户管理员
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw BusinessException.badRequest("不能直接创建超级管理员");
        }
        if (Boolean.TRUE.equals(user.getIsTenantAdmin())) {
            throw BusinessException.badRequest("不能直接创建租户管理员");
        }

        // 检查密码复杂度
        String passwordError = PasswordPolicy.validate(user.getPassword());
        if (passwordError != null) {
            throw BusinessException.badRequest(passwordError + "（" + PasswordPolicy.getStrengthDescription() + "）");
        }

        // 检查用户名是否存在
        if (baseMapper.selectByUsername(user.getUsername(), user.getTenantId()) != null) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 加密密码
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setPasswordUpdateTime(LocalDateTime.now());
        user.setIsSuperAdmin(false);
        user.setStatus(0);
        user.setLoginCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        save(user);
        log.info("创建用户成功: userId={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        if (user == null || user.getId() == null) {
            throw BusinessException.badRequest("用户ID不能为空");
        }
        SysUser existing = getById(user.getId());
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("更新用户成功: userId={}", user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw BusinessException.badRequest("超级管理员不可删除");
        }
        if (Boolean.TRUE.equals(user.getIsTenantAdmin())) {
            throw BusinessException.badRequest("租户管理员不可删除，请先转移管理员权限");
        }
        removeById(userId);
        log.info("删除用户成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<SysUser> users = listByIds(userIds);
        if (users.size() != userIds.size()) {
            throw BusinessException.notFound("部分用户不存在");
        }
        // 检查是否有超级管理员或租户管理员
        boolean hasSuperAdmin = users.stream().anyMatch(u -> Boolean.TRUE.equals(u.getIsSuperAdmin()));
        if (hasSuperAdmin) {
            throw BusinessException.badRequest("超级管理员不可删除");
        }
        boolean hasTenantAdmin = users.stream().anyMatch(u -> Boolean.TRUE.equals(u.getIsTenantAdmin()));
        if (hasTenantAdmin) {
            throw BusinessException.badRequest("租户管理员不可删除，请先转移管理员权限");
        }
        removeByIds(userIds);
        log.info("批量删除用户成功: userIds={}", userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser existing = getById(userId);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (Boolean.TRUE.equals(existing.getIsSuperAdmin())) {
            throw BusinessException.badRequest("超级管理员密码不可通过此接口重置");
        }
        // 检查新密码复杂度
        String passwordError = PasswordPolicy.validate(newPassword);
        if (passwordError != null) {
            throw BusinessException.badRequest(passwordError + "（" + PasswordPolicy.getStrengthDescription() + "）");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setPasswordUpdateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("重置密码成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("原密码错误");
        }
        resetPassword(userId, newPassword);
    }

    @Override
    public Page<SysUser> pageUsers(Page<SysUser> page, Long tenantId,
                                   String username, Integer status, Long deptId) {
        return baseMapper.selectUserPage(page, tenantId, username, status, deptId);
    }

    @Override
    public SysUser getUserDetail(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        return baseMapper.selectPermissionCodesByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 参数校验
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            // 清空用户角色
            userRoleMapper.deleteByUserId(userId);
            log.info("清空用户角色: userId={}", userId);
            return;
        }

        // 查询目标用户的租户上下文
        SysUser targetUser = getById(userId);
        if (targetUser == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 查询角色作用域并校验
        Collection<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw BusinessException.notFound("部分角色不存在");
        }
        Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getId, r -> r));
        boolean isTenantUser = targetUser.getTenantId() != null;
        for (Long roleId : roleIds) {
            Role role = roleMap.get(roleId);
            if (role == null) continue;
            if ("PLATFORM".equals(role.getScope()) && isTenantUser) {
                throw BusinessException.forbidden(
                    "不能将平台级角色「" + role.getRoleName() + "」分配给租户用户");
            }
            if ("TENANT".equals(role.getScope()) && !isTenantUser) {
                throw BusinessException.forbidden(
                    "不能将租户级角色「" + role.getRoleName() + "」分配给平台用户");
            }
        }

        // 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);

        // 批量插入新的角色关联
        List<SysUserRole> userRoles = roleIds.stream()
                .distinct()
                .map(roleId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRole.setTenantId(getUserTenantId(userId));
                    return userRole;
                })
                .toList();

        if (!userRoles.isEmpty()) {
            userRoleMapper.batchInsert(userRoles);
        }

        log.info("分配角色成功: userId={}, roleIds={}", userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssignRoles(List<Long> userIds, List<Long> roleIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("用户ID列表不能为空");
        }
        for (Long userId : userIds) {
            assignRoles(userId, roleIds);
        }
        log.info("批量分配角色成功: userIds={}, roleIds={}", userIds, roleIds);
    }

    /**
     * 获取用户所属租户ID
     */
    private Long getUserTenantId(Long userId) {
        SysUser user = getById(userId);
        return user != null ? user.getTenantId() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        SysUser existing = getById(userId);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (Boolean.TRUE.equals(existing.getIsSuperAdmin()) && status != 0) {
            throw BusinessException.badRequest("超级管理员不可禁用");
        }
        if (Boolean.TRUE.equals(existing.getIsTenantAdmin()) && status != 0) {
            throw BusinessException.badRequest("租户管理员不可禁用，请先转移管理员权限");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("更新用户状态成功: userId={}, status={}", userId, status);
    }
}
