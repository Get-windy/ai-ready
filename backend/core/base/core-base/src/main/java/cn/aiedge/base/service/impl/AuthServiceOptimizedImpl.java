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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Primary
public class AuthServiceOptimizedImpl implements AuthService {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TenantMapper tenantMapper;

    private final Cache<String, Integer> loginFailCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final Cache<String, LocalDateTime> loginRateCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final String ACCOUNT_LOCK_KEY_PREFIX = "account:lock:";
    private static final String LOGIN_RATE_KEY_PREFIX = "login:rate:";
    private static final String LOGIN_LOG_KEY_PREFIX = "login:log:";
    
    private static final int MAX_LOGIN_FAIL_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    private static final int LOGIN_RATE_LIMIT_SECONDS = 3;
    private static final int PASSWORD_MIN_LENGTH = 8;

    @Override
    public LoginVO login(LoginRequest request, String clientIp) {
        String username = request.getUsername();
        String password = request.getPassword();
        
        checkAccountLock(username);
        checkLoginRateLimit(username, clientIp);
        
        User user = userService.getByUsername(username);
        if (user == null) {
            recordLoginFail(username, clientIp);
            throw BusinessException.badRequest("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw BusinessException.badRequest("用户已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLoginFail(username, clientIp);
            throw BusinessException.badRequest("用户名或密码错误");
        }

        clearLoginFailRecord(username);

        // 租户过期/停用检查
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

        StpUtil.login(user.getId());

        String tokenValue = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        updateLoginInfoAsync(user.getId(), clientIp);

        LoginVO vo = buildLoginResponse(user, tokenValue, tokenTimeout);

        recordLoginSuccess(user.getId(), username, clientIp);

        log.info("用户登录成功: {}", user.getUsername());
        return vo;
    }

    private void checkAccountLock(String username) {
        if (redisTemplate != null) {
            String lockKey = ACCOUNT_LOCK_KEY_PREFIX + username;
            Boolean isLocked = (Boolean) redisTemplate.opsForValue().get(lockKey);
            
            if (Boolean.TRUE.equals(isLocked)) {
                Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
                throw BusinessException.badRequest(
                    String.format("账户已被锁定，请%d分钟后重试", ttl != null ? ttl : ACCOUNT_LOCK_DURATION_MINUTES)
                );
            }
        } else {
            Integer failCount = loginFailCache.getIfPresent(username);
            if (failCount != null && failCount >= MAX_LOGIN_FAIL_ATTEMPTS) {
                throw BusinessException.badRequest("账户已被锁定，请稍后重试");
            }
        }
    }

    private void checkLoginRateLimit(String username, String clientIp) {
        LocalDateTime lastLogin = loginRateCache.getIfPresent(username);
        
        if (lastLogin != null) {
            long secondsSinceLastLogin = ChronoUnit.SECONDS.between(lastLogin, LocalDateTime.now());
            if (secondsSinceLastLogin < LOGIN_RATE_LIMIT_SECONDS) {
                throw BusinessException.badRequest(
                    String.format("登录过于频繁，请%d秒后再试", LOGIN_RATE_LIMIT_SECONDS - secondsSinceLastLogin)
                );
            }
        }
        
        loginRateCache.put(username, LocalDateTime.now());
    }

    private void recordLoginFail(String username, String clientIp) {
        Integer failCount = loginFailCache.getIfPresent(username);
        failCount = (failCount == null) ? 1 : failCount + 1;
        loginFailCache.put(username, failCount);
        
        if (redisTemplate != null) {
            String failKey = LOGIN_FAIL_KEY_PREFIX + username;
            redisTemplate.opsForValue().set(failKey, failCount, 30, TimeUnit.MINUTES);
            
            if (failCount >= MAX_LOGIN_FAIL_ATTEMPTS) {
                String lockKey = ACCOUNT_LOCK_KEY_PREFIX + username;
                redisTemplate.opsForValue().set(lockKey, true, ACCOUNT_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            }
        }
        
        log.warn("登录失败: username={}, ip={}, failCount={}", username, clientIp, failCount);
        
        if (failCount >= MAX_LOGIN_FAIL_ATTEMPTS) {
            throw BusinessException.badRequest(
                String.format("登录失败次数过多，账户已锁定%d分钟", ACCOUNT_LOCK_DURATION_MINUTES)
            );
        }
    }

    private void clearLoginFailRecord(String username) {
        loginFailCache.invalidate(username);
        
        if (redisTemplate != null) {
            String failKey = LOGIN_FAIL_KEY_PREFIX + username;
            String lockKey = ACCOUNT_LOCK_KEY_PREFIX + username;
            redisTemplate.delete(failKey);
            redisTemplate.delete(lockKey);
        }
    }

    private void updateLoginInfoAsync(Long userId, String loginIp) {
        if (redisTemplate != null) {
            String logKey = LOGIN_LOG_KEY_PREFIX + LocalDateTime.now().toLocalDate();
            String logEntry = String.format("%d|%s|%d", userId, loginIp, System.currentTimeMillis());
            redisTemplate.opsForList().rightPush(logKey, logEntry);
        }
        
        userService.updateLoginInfo(userId, loginIp);
    }

    private void recordLoginSuccess(Long userId, String username, String clientIp) {
        log.info("登录成功: userId={}, username={}, ip={}", userId, username, clientIp);
    }

    private LoginVO buildLoginResponse(User user, String tokenValue, long tokenTimeout) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(tokenValue);
        vo.setExpiresIn(tokenTimeout);

        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());

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

    public void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            throw BusinessException.badRequest("密码不能为空");
        }
        
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw BusinessException.badRequest("密码长度至少为" + PASSWORD_MIN_LENGTH + "位");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw BusinessException.badRequest("密码必须包含数字");
        }
        
        if (!password.matches(".*[a-zA-Z].*")) {
            throw BusinessException.badRequest("密码必须包含字母");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw BusinessException.badRequest("密码必须包含特殊字符");
        }
        
        String[] weakPasswords = {"password", "12345678", "qwerty", "admin123"};
        for (String weak : weakPasswords) {
            if (password.toLowerCase().contains(weak)) {
                throw BusinessException.badRequest("密码过于简单，请使用更复杂的密码");
            }
        }
    }
}