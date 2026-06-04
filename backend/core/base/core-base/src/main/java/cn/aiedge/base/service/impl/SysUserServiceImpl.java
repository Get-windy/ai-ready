package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.entity.SysUserTenant;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.mapper.SysUserTenantMapper;
import cn.aiedge.base.service.SysUserService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> 
        implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserTenantMapper userTenantMapper;

    @Override
    public String login(String username, String password, Long tenantId, String loginIp) {
        // 1. 查询用户（用户名全局唯一）
        SysUser user = baseMapper.selectByUsername(username, null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 验证用户是否属于指定租户（通过 sys_user_tenant 关联表）
        if (!isUserInTenant(user.getId(), tenantId)) {
            throw new RuntimeException("该用户不属于此租户，请检查租户名称");
        }

        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已禁用或锁定");
        }

        // 4. 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 5. 登录成功，生成Token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 6. 将租户ID存入Sa-Token Session，避免多租户拦截器递归查询
        StpUtil.getSession().set("tenantId", tenantId);

        // 7. 更新登录信息
        baseMapper.updateLoginInfo(user.getId(), loginIp);

        log.info("用户登录成功: userId={}, username={}, tenantId={}", user.getId(), username, tenantId);
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
        // 检查用户名是否存在
        if (baseMapper.selectByUsername(user.getUsername(), user.getTenantId()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 加密密码
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
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
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("更新用户成功: userId={}", user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        removeById(userId);
        log.info("删除用户成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<Long> userIds) {
        removeByIds(userIds);
        log.info("批量删除用户成功: userIds={}", userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("重置密码成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
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
        return getById(userId);
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
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("更新用户状态成功: userId={}, status={}", userId, status);
    }
}
