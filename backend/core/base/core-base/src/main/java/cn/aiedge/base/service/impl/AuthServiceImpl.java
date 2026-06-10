package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.User;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.service.AuthService;
import cn.aiedge.base.service.UserService;
import cn.aiedge.common.dto.auth.LoginRequest;
import cn.aiedge.common.dto.auth.LoginVO;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * 包含登录失败锁定机制: 连续5次失败后锁定账户30分钟
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final TenantMapper tenantMapper;

    /** 最大失败次数 */
    private static final int MAX_FAILURE_COUNT = 5;
    /** 锁定时长（分钟） */
    private static final int LOCK_DURATION_MINUTES = 30;
    /** Redis 失败计数键前缀 */
    private static final String FAILURE_KEY_PREFIX = "login:failure:";

    public AuthServiceImpl(UserService userService, PasswordEncoder passwordEncoder,
                           StringRedisTemplate stringRedisTemplate,
                           TenantMapper tenantMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public LoginVO login(LoginRequest request, String clientIp) {
        String failureKey = FAILURE_KEY_PREFIX + request.getUsername();

        // 检查是否被锁定
        String failureCount = stringRedisTemplate.opsForValue().get(failureKey);
        if (failureCount != null && Integer.parseInt(failureCount) >= MAX_FAILURE_COUNT) {
            Long ttl = stringRedisTemplate.getExpire(failureKey, TimeUnit.MINUTES);
            long remainingMinutes = (ttl != null && ttl > 0) ? ttl : LOCK_DURATION_MINUTES;
            throw BusinessException.badRequest(
                "账户已被锁定，请" + remainingMinutes + "分钟后重试");
        }

        // 查询用户
        User user = userService.getByUsername(request.getUsername());
        if (user == null) {
            recordFailure(request.getUsername());
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 检查用户状态 (0正常 1停用)
        if (user.getStatus() == 1) {
            throw BusinessException.badRequest("用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailure(request.getUsername());
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 登录成功，清除失败计数
        stringRedisTemplate.delete(failureKey);

        // 检查租户有效性（过期或停用的租户禁止登录）
        if (user.getTenantId() != null) {
            SysTenant tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant == null || tenant.getDeleted() == 1) {
                throw BusinessException.badRequest("租户不存在或已被删除");
            }
            if (tenant.getStatus() != null && tenant.getStatus() != 0) {
                throw BusinessException.badRequest("租户已被停用，请联系平台管理员");
            }
            if (tenant.getExpireTime() != null && LocalDateTime.now().isAfter(tenant.getExpireTime())) {
                throw BusinessException.badRequest("租户已过期，请联系平台管理员续费");
            }
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 存储租户ID和部门ID到会话，供后续权限检查使用
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("deptId", user.getDeptId());
        log.debug("用户会话已保存租户信息: userId={}, tenantId={}, deptId={}",
                user.getId(), user.getTenantId(), user.getDeptId());

        // 获取Token信息
        String tokenValue = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        // 更新登录信息
        userService.updateLoginInfo(user.getId(), clientIp);

        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setAccessToken(tokenValue);
        vo.setExpiresIn(tokenTimeout);

        // 用户信息
        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());

        // 角色和权限
        List<String> roles = userService.getRoleCodes(user.getId());
        List<String> permissions = userService.getPermissionCodes(user.getId());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        vo.setUserInfo(userInfo);

        log.info("用户登录成功: {}", user.getUsername());
        return vo;
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("用户登出成功: userId={}", userId);
        }
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!StpUtil.isLogin()) {
            throw BusinessException.unauthorized("用户未登录，请重新登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String newToken = StpUtil.getTokenValue();

        User user = userService.getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        LoginVO vo = new LoginVO();
        vo.setAccessToken(newToken);
        vo.setTokenType("Bearer");
        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(userId);
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());
        vo.setUserInfo(userInfo);
        return vo;
    }

    @Override
    public LoginVO.UserInfo getCurrentUserInfo() {
        if (!StpUtil.isLogin()) {
            throw BusinessException.unauthorized("用户未登录");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());

        List<String> roles = userService.getRoleCodes(userId);
        List<String> permissions = userService.getPermissionCodes(userId);
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    @Override
    public boolean checkUsernameAvailable(String username) {
        return userService.getByUsername(username) == null;
    }

    /**
     * 记录登录失败次数，超过阈值锁定账户
     */
    private void recordFailure(String username) {
        String failureKey = FAILURE_KEY_PREFIX + username;
        Long count = stringRedisTemplate.opsForValue().increment(failureKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(failureKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= MAX_FAILURE_COUNT) {
            log.warn("账户已被锁定: username={}, failureCount={}", username, count);
        }
    }
}
