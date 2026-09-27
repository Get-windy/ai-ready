package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.entity.SysUserRole;
import cn.aiedge.base.entity.SysUserTenant;
import cn.aiedge.base.mapper.RoleMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.mapper.SysUserTenantMapper;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.spi.PlatformSecuritySettingsProvider;
import cn.aiedge.base.spi.PlatformSecuritySettingsProvider.PlatformSecuritySettings;
import cn.aiedge.base.util.PasswordPolicy;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /** 密码最长有效期（天）—— **yml 兜底值**；平台安全策略有配置时以策略为准 */
    @Value("${password.policy.max-age-days:90}")
    private int passwordMaxAgeDays;

    /** 平台安全策略读取口（core-api 提供实现）；裁剪部署或单测下可能缺失 → 允许为 null */
    @Autowired(required = false)
    private PlatformSecuritySettingsProvider securitySettingsProvider;

    /** SoD（职责分离）规则校验：分配角色前检查是否存在互斥组合 */
    @Autowired(required = false)
    private cn.aiedge.base.service.SysSodRuleService sysSodRuleService;

    /**
     * 解析密码最长有效期。
     *
     * <p>优先级：平台安全策略（「系统 → 平台设置 → 安全策略」的 `password_expire_days`）
     * → yml `password.policy.max-age-days` → 默认 90。
     *
     * <p>约定：返回值 {@code <=0} 表示**不过期**（页面把有效期设为 0 即关闭该检查）。
     * 读取失败一律回退 yml，**不因为读不到策略就报错或放宽校验**。
     */
    private int resolvePasswordMaxAgeDays() {
        try {
            PlatformSecuritySettingsProvider p = securitySettingsProvider;
            if (p != null) {
                PlatformSecuritySettings s = p.currentSecuritySettings();
                if (s != null && s.passwordExpireDays() != null) {
                    return s.passwordExpireDays();
                }
            }
        } catch (Exception e) {
            log.warn("读取平台安全策略的密码有效期失败，回退到 yml password.policy.max-age-days", e);
        }
        return passwordMaxAgeDays;
    }

    @Override
    public String login(String username, String password, Long tenantId, String loginIp) {
        return completeLogin(verifyCredentials(username, password), tenantId, loginIp);
    }

    @Override
    public SysUser verifyCredentials(String username, String password) {
        // 用户名全局唯一，据此跨租户定位账号（登录发生在认证之前，见 MyBatisPlusConfig 对 sys_user 的说明）
        SysUser user = baseMapper.selectByUsername(username, null);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "用户已禁用或锁定");
        }

        // 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
        }
        return user;
    }

    @Override
    public SysUser findByPhoneForLogin(String phone) {
        List<SysUser> users = baseMapper.selectListByPhoneForLogin(phone);
        if (users.isEmpty()) {
            throw BusinessException.notFound("该手机号未绑定账号");
        }
        if (users.size() > 1) {
            // phone 无唯一约束：宁可拒绝也不能随机选一个，否则会把人登进别人的账号
            log.warn("手机号命中多个账号，拒绝短信登录: phone={}, count={}", phone, users.size());
            throw BusinessException.badRequest("该手机号关联了多个账号，请改用用户名登录");
        }

        SysUser user = users.get(0);
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "用户已禁用或锁定");
        }
        return user;
    }

    @Override
    public String completeLogin(SysUser user, Long tenantId, String loginIp) {
        // 校验用户确实属于该企业。用 getUserTenants 而非 isUserInTenant：
        // 前者额外过滤了「企业已停用 / 已删除」，否则停用的企业仍能登进去。
        boolean allowed = tenantId != null && getUserTenants(user.getId()).stream()
                .anyMatch(t -> t.getId().equals(tenantId));
        if (!allowed) {
            throw BusinessException.badRequest("该用户不属于所选企业");
        }

        // 登录成功，生成Token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 将租户ID存入Sa-Token Session，避免多租户拦截器递归查询
        StpUtil.getSession().set("tenantId", tenantId);
        StpUtil.getSession().set("username", user.getUsername());

        // 写入「租户隔离整体豁免」标记（平台超管）。
        // 必须在这里算并写进 Session —— 拦截器侧只能读缓存，实时算角色会经过 SQL 造成无限递归。
        // 也必须排在 tenantId 落会话之后：角色是按租户查的。
        StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"));

        // 密码过期检查（有效期来自平台安全策略，未配置时回退 yml）
        int maxAgeDays = resolvePasswordMaxAgeDays();
        boolean passwordExpired = false;
        if (user.getPasswordUpdateTime() != null && maxAgeDays > 0) {
            passwordExpired = Duration.between(user.getPasswordUpdateTime(), LocalDateTime.now())
                .toDays() >= maxAgeDays;
        }
        StpUtil.getSession().set("passwordExpired", passwordExpired);

        // 记录「上次登录该企业」的时间 → 下次登录把它排在候选第一位
        touchTenantLoginTime(user.getId(), tenantId);

        // 更新登录信息
        String safeLoginIp = (loginIp != null && !loginIp.isEmpty()) ? loginIp : "0.0.0.0";
        baseMapper.updateLoginInfo(user.getId(), safeLoginIp);

        log.info("用户登录成功: userId={}, username={}, tenantId={}, passwordExpired={}",
            user.getId(), user.getUsername(), tenantId, passwordExpired);
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
    public void touchTenantLoginTime(Long userId, Long tenantId) {
        userTenantMapper.updateLastLoginTime(userId, tenantId);
    }

    @Override
    public Long getLastLoginTenantId(Long userId) {
        return userTenantMapper.selectLastLoginTenantId(userId);
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

    /** 平台超级管理员角色码（与本库既有口径一致：SysMenuServiceImpl 亦以此判定超管） */
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    /**
     * 分页查询用户。
     *
     * <p><b>租户条件必须在这里显式施加</b>：`sys_user` 被登记在
     * `MyBatisPlusConfig.IGNORE_TENANT_TABLES`（登录要按用户名跨租户查账号，不能移除），
     * 多租户拦截器因此**不会**为它注入 `tenant_id`，全靠 `SysUserMapper.selectUserPage` 的
     * `<if test="tenantId != null"> AND tenant_id = #{tenantId} </if>` 这一段 ——
     * 而此前传入的 `tenantId` **完全来自 HTTP 查询参数**：前端不传就没有任何租户条件，
     * 任意租户的管理员即可翻到全库所有租户的账号（用户名 / 邮箱 / 手机号）。
     *
     * <p>现按「**超管豁免、其余强制收敛到会话租户**」收敛。
     */
    @Override
    public Page<SysUser> pageUsers(Page<SysUser> page, Long tenantId,
                                   String username, Integer status, Long deptId) {
        return baseMapper.selectUserPage(page, resolveScopedTenantId(tenantId), username, status, deptId);
    }

    /**
     * 计算「实际生效的租户条件」。
     *
     * <ul>
     *   <li><b>平台超管</b> → 豁免：沿用调用方传入值（null 表示不限制，保持全局视野与「切换租户」能力）；</li>
     *   <li><b>其它角色</b> → 强制收敛到会话租户，<b>忽略</b>调用方传入值；</li>
     *   <li>取不到会话租户且非超管 → 直接拒绝（宁可报错，也不放行全表）。</li>
     * </ul>
     *
     * <p>超管判定用 Sa-Token 角色列表（`SUPER_ADMIN`），与 `SysMenuServiceImpl` 同源，
     * 不新造第二套判定。会话租户取自 `SecurityUtils.getCurrentTenantId()` —— 它读 Sa-Token Session
     * 中的 `tenantId`，故超管切换租户后立即生效，无需额外适配。
     */
    private Long resolveScopedTenantId(Long requestedTenantId) {
        if (SecurityUtils.hasRole(SUPER_ADMIN_ROLE)) {
            return requestedTenantId;
        }
        Long sessionTenantId = SecurityUtils.getCurrentTenantId();
        if (sessionTenantId == null) {
            throw BusinessException.forbidden("无法确定当前租户，已拒绝查询用户列表");
        }
        return sessionTenantId;
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
        Collection<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw BusinessException.notFound("部分角色不存在");
        }
        Map<Long, SysRole> roleMap = roles.stream().collect(Collectors.toMap(SysRole::getId, r -> r));
        boolean isTenantUser = targetUser.getTenantId() != null;
        for (Long roleId : roleIds) {
            SysRole role = roleMap.get(roleId);
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

        // SoD（职责分离）校验：新角色集合内部是否存在互斥组合。
        // ⚠️ 此前只有 core-api 的 assignUserRoles 做了这个校验，而前端「分配角色」走的是本方法
        //    （POST /user/{id}/roles），导致 SoD 规则配了也不拦人（2026-09-20 修复）。
        //    规则为空表时 findConflictingRoleIds 返回空集合，故对现有行为零影响。
        if (sysSodRuleService != null) {
            List<Long> conflictingRoleIds = sysSodRuleService.findConflictingRoleIds(roleIds);
            if (conflictingRoleIds != null && !conflictingRoleIds.isEmpty()) {
                String conflictNames = roleMapper.selectBatchIds(conflictingRoleIds).stream()
                        .map(SysRole::getRoleName)
                        .collect(Collectors.joining("、"));
                log.warn("SoD 校验失败: userId={}, 互斥角色={}", userId, conflictNames);
                throw BusinessException.forbidden(
                        "职责分离冲突：角色「" + conflictNames + "」互斥，不能同时分配给同一用户");
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
