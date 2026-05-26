package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.User;
import cn.aiedge.base.service.AuthService;
import cn.aiedge.base.service.UserService;
import cn.aiedge.common.dto.auth.LoginRequest;
import cn.aiedge.common.dto.auth.LoginVO;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类 - 性能优化与安全加固版本
 * 
 * 优化点：
 * 1. 防暴力破解机制（登录失败次数限制、账户锁定）
 * 2. 登录频率限制
 * 3. 密码强度校验增强
 * 4. 登录日志异步记录
 * 5. 缓存优化减少数据库查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceOptimizedImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    // 登录失败计数缓存
    private final Cache<String, Integer> loginFailCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    // 登录频率限制缓存
    private final Cache<String, LocalDateTime> loginRateCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    // Redis Key前缀
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final String ACCOUNT_LOCK_KEY_PREFIX = "account:lock:";
    private static final String LOGIN_RATE_KEY_PREFIX = "login:rate:";
    private static final String LOGIN_LOG_KEY_PREFIX = "login:log:";
    
    // 安全配置常量
    private static final int MAX_LOGIN_FAIL_ATTEMPTS = 5;        // 最大登录失败次数
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30; // 账户锁定时间
    private static final int LOGIN_RATE_LIMIT_SECONDS = 3;       // 登录频率限制（秒）
    private static final int PASSWORD_MIN_LENGTH = 8;            // 密码最小长度

    @Override
    public LoginVO login(LoginRequest request, String clientIp) {
        String username = request.getUsername();
        String password = request.getPassword();
        
        // 1. 检查账户是否被锁定
        checkAccountLock(username);
        
        // 2. 检查登录频率限制
        checkLoginRateLimit(username, clientIp);
        
        // 3. 查询用户 - 使用缓存优化
        User user = userService.getByUsername(username);
        if (user == null) {
            recordLoginFail(username, clientIp);
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 4. 检查用户状态
        if (user.getStatus() != 1) {
            throw BusinessException.badRequest("用户已被禁用");
        }

        // 5. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLoginFail(username, clientIp);
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 6. 清除登录失败记录
        clearLoginFailRecord(username);
        
        // 7. Sa-Token 登录
        StpUtil.login(user.getId());

        // 8. 获取Token信息
        String tokenValue = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        // 9. 异步更新登录信息
        updateLoginInfoAsync(user.getId(), clientIp);

        // 10. 构建响应
        LoginVO vo = buildLoginResponse(user, tokenValue, tokenTimeout);

        // 11. 记录登录成功日志
        recordLoginSuccess(user.getId(), username, clientIp);

        log.info("用户登录成功: {}", user.getUsername());
        return vo;
    }

    /**
     * 检查账户是否被锁定
     */
    private void checkAccountLock(String username) {
        String lockKey = ACCOUNT_LOCK_KEY_PREFIX + username;
        Boolean isLocked = (Boolean) redisTemplate.opsForValue().get(lockKey);
        
        if (Boolean.TRUE.equals(isLocked)) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
            throw BusinessException.badRequest(
                String.format("账户已被锁定，请%d分钟后重试", ttl != null ? ttl : ACCOUNT_LOCK_DURATION_MINUTES)
            );
        }
    }

    /**
     * 检查登录频率限制
     */
    private void checkLoginRateLimit(String username, String clientIp) {
        // 检查用户名级别的频率限制
        String rateKey = LOGIN_RATE_KEY_PREFIX + username;
        LocalDateTime lastLogin = (LocalDateTime) redisTemplate.opsForValue().get(rateKey);
        
        if (lastLogin != null) {
            long secondsSinceLastLogin = ChronoUnit.SECONDS.between(lastLogin, LocalDateTime.now());
            if (secondsSinceLastLogin < LOGIN_RATE_LIMIT_SECONDS) {
                throw BusinessException.badRequest(
                    String.format("登录过于频繁，请%d秒后再试", LOGIN_RATE_LIMIT_SECONDS - secondsSinceLastLogin)
                );
            }
        }
        
        // 检查IP级别的频率限制（防止暴力破解）
        String ipRateKey = LOGIN_RATE_KEY_PREFIX + "ip:" + clientIp;
        Integer ipAttempts = (Integer) redisTemplate.opsForValue().get(ipRateKey);
        if (ipAttempts != null && ipAttempts > 10) {
            throw BusinessException.badRequest("该IP登录尝试过于频繁，请稍后再试");
        }
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFail(String username, String clientIp) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + username;
        String ipRateKey = LOGIN_RATE_KEY_PREFIX + "ip:" + clientIp;
        String rateKey = LOGIN_RATE_KEY_PREFIX + username;
        
        // 增加失败计数
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        failCount = (failCount == null) ? 1 : failCount + 1;
        redisTemplate.opsForValue().set(failKey, failCount, 30, TimeUnit.MINUTES);
        
        // 增加IP尝试计数
        Integer ipAttempts = (Integer) redisTemplate.opsForValue().get(ipRateKey);
        ipAttempts = (ipAttempts == null) ? 1 : ipAttempts + 1;
        redisTemplate.opsForValue().set(ipRateKey, ipAttempts, 5, TimeUnit.MINUTES);
        
        // 记录登录时间
        redisTemplate.opsForValue().set(rateKey, LocalDateTime.now(), 1, TimeUnit.MINUTES);
        
        // 检查是否需要锁定账户
        if (failCount >= MAX_LOGIN_FAIL_ATTEMPTS) {
            String lockKey = ACCOUNT_LOCK_KEY_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, true, ACCOUNT_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            
            // 记录安全事件
            log.warn("账户锁定: username={}, ip={}, failCount={}", username, clientIp, failCount);
            
            throw BusinessException.badRequest(
                String.format("登录失败次数过多，账户已锁定%d分钟", ACCOUNT_LOCK_DURATION_MINUTES)
            );
        }
        
        log.warn("登录失败: username={}, ip={}, failCount={}", username, clientIp, failCount);
    }

    /**
     * 清除登录失败记录
     */
    private void clearLoginFailRecord(String username) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + username;
        String lockKey = ACCOUNT_LOCK_KEY_PREFIX + username;
        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);
    }

    /**
     * 异步更新登录信息
     */
    private void updateLoginInfoAsync(Long userId, String loginIp) {
        // 使用Redis队列记录登录日志，由后台任务异步处理
        String logKey = LOGIN_LOG_KEY_PREFIX + LocalDateTime.now().toLocalDate();
        String logEntry = String.format("%d|%s|%d", userId, loginIp, System.currentTimeMillis());
        redisTemplate.opsForList().rightPush(logKey, logEntry);
        
        // 更新用户登录信息
        userService.updateLoginInfo(userId, loginIp);
    }

    /**
     * 记录登录成功
     */
    private void recordLoginSuccess(Long userId, String username, String clientIp) {
        log.info("登录成功: userId={}, username={}, ip={}", userId, username, clientIp);
    }

    /**
     * 构建登录响应
     */
    private LoginVO buildLoginResponse(User user, String tokenValue, long tokenTimeout) {
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

        // 角色和权限 - 使用缓存优化
        List<String> roles = userService.getRoleCodes(user.getId());
        List<String> permissions = userService.getPermissionCodes(user.getId());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        vo.setUserInfo(userInfo);
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
        // TODO: 实现刷新令牌逻辑
        throw BusinessException.badRequest("暂不支持刷新令牌");
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
     * 密码强度校验
     */
    public void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            throw BusinessException.badRequest("密码不能为空");
        }
        
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw BusinessException.badRequest("密码长度至少为" + PASSWORD_MIN_LENGTH + "位");
        }
        
        // 检查是否包含数字
        if (!password.matches(".*\\d.*")) {
            throw BusinessException.badRequest("密码必须包含数字");
        }
        
        // 检查是否包含字母
        if (!password.matches(".*[a-zA-Z].*")) {
            throw BusinessException.badRequest("密码必须包含字母");
        }
        
        // 检查是否包含特殊字符
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw BusinessException.badRequest("密码必须包含特殊字符");
        }
        
        // 检查常见弱密码
        String[] weakPasswords = {"password", "12345678", "qwerty", "admin123"};
        for (String weak : weakPasswords) {
            if (password.toLowerCase().contains(weak)) {
                throw BusinessException.badRequest("密码过于简单，请使用更复杂的密码");
            }
        }
    }
}
