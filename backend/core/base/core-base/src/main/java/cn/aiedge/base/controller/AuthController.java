package cn.aiedge.base.controller;

import cn.aiedge.base.dto.AuthDTO;
import cn.aiedge.base.entity.SysLoginLog;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.service.SysLoginLogService;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 提供登录、登出、Token刷新等认证相关接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "认证管理", description = "登录、登出、Token刷新等接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final SysLoginLogService loginLogService;
    private final cn.aiedge.base.security.StpInterfaceImpl stpInterface;
    private final TenantMapper tenantMapper;
    private final cn.aiedge.base.security.SecurityContext securityContext;
    private final cn.aiedge.base.service.RoleBillTypeService roleBillTypeService;
    private final cn.aiedge.base.security.LoginAttemptService loginAttemptService;

    /**
     * 获取验证码
     */
    @Operation(summary = "获取验证码", description = "登录页面获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, Object>> getCaptcha() {
        Map<String, Object> result = new HashMap<>();
        String uuid = IdUtil.fastSimpleUUID();
        
        // 生成4位随机验证码
        String captchaCode = generateCaptchaCode(4);
        
        // 生成SVG验证码图片
        String svgCaptcha = generateSvgCaptcha(captchaCode);
        
        // 将验证码存入缓存（后续登录时验证）
        // 注意：实际项目中应该存入Redis，这里暂时使用内存缓存
        captchaCache.put(uuid, captchaCode);
        
        result.put("uuid", uuid);
        result.put("img", "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgCaptcha.getBytes()));
        return Result.ok(result);
    }

    /**
     * 生成随机验证码
     */
    private String generateCaptchaCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成SVG验证码图片
     */
    private String generateSvgCaptcha(String code) {
        int width = 120;
        int height = 40;
        
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width).append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">");
        
        // 背景
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#f0f0f0\"/>");
        
        // 干扰线
        for (int i = 0; i < 4; i++) {
            int x1 = (int) (Math.random() * width);
            int y1 = (int) (Math.random() * height);
            int x2 = (int) (Math.random() * width);
            int y2 = (int) (Math.random() * height);
            String color = getRandomColor();
            svg.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
               .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
               .append("\" stroke=\"").append(color).append("\" stroke-width=\"1\" opacity=\"0.5\"/>");
        }
        
        // 验证码文字
        int charWidth = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            int x = charWidth * (i + 1);
            int y = height / 2 + 5;
            String color = getRandomDarkColor();
            double rotation = (Math.random() - 0.5) * 30;
            svg.append("<text x=\"").append(x).append("\" y=\"").append(y)
               .append("\" font-family=\"Arial, sans-serif\" font-size=\"20\" font-weight=\"bold\" fill=\"").append(color)
               .append("\" text-anchor=\"middle\" dominant-baseline=\"middle\" transform=\"rotate(").append(rotation).append(" ").append(x).append(" ").append(y).append(")\">")
               .append(code.charAt(i)).append("</text>");
        }
        
        // 干扰点
        for (int i = 0; i < 20; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            String color = getRandomColor();
            svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
               .append("\" r=\"1\" fill=\"").append(color).append("\" opacity=\"0.6\"/>");
        }
        
        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 获取随机浅色
     */
    private String getRandomColor() {
        int r = 150 + (int) (Math.random() * 105);
        int g = 150 + (int) (Math.random() * 105);
        int b = 150 + (int) (Math.random() * 105);
        return "#" + String.format("%02x", r) + String.format("%02x", g) + String.format("%02x", b);
    }

    /**
     * 获取随机深色
     */
    private String getRandomDarkColor() {
        int r = (int) (Math.random() * 100);
        int g = (int) (Math.random() * 100);
        int b = (int) (Math.random() * 100);
        return "#" + String.format("%02x", r) + String.format("%02x", g) + String.format("%02x", b);
    }

    // 验证码缓存（临时方案，实际应使用Redis）
    private static final java.util.concurrent.ConcurrentHashMap<String, String> captchaCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "账号密码登录，返回Token")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @Valid @RequestBody AuthDTO.Login dto,
            HttpServletRequest request) {
        
        String loginIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String tokenId = IdUtil.fastSimpleUUID();
        Map<String, Object> result = new HashMap<>();

        try {
            // 根据租户名称获取租户ID
            Long tenantId = getTenantIdByName(dto.tenantName());
            if (tenantId == null) {
                return Result.fail(401, "租户不存在或已禁用");
            }

            // 检查账户是否被锁定（连续登录失败）
            if (loginAttemptService.isLocked(dto.username(), tenantId)) {
                long remaining = loginAttemptService.getLockRemainingSeconds(dto.username(), tenantId);
                String msg = "账户已被临时锁定，请" + (remaining / 60 + 1) + "分钟后再试";
                log.warn("登录失败（账户已锁定）: username={}, remaining={}s", dto.username(), remaining);
                return Result.fail(401, msg);
            }

            // 设置临时租户上下文，避免多租户拦截器注入 tenant_id=0
            securityContext.setTempTenantId(tenantId);

            String token;
            try {
                // 执行登录
                token = userService.login(dto.username(), dto.password(), tenantId, loginIp);
                // 登录成功，清除失败记录
                loginAttemptService.clearFailedAttempts(dto.username(), tenantId);
            } catch (Exception loginException) {
                // 登录失败，记录失败次数
                int attempts = loginAttemptService.recordFailedAttempt(dto.username(), tenantId);
                int remaining = LoginAttemptService.MAX_ATTEMPTS - attempts;
                log.warn("登录失败: username={}, attempts={}, remaining={}",
                        dto.username(), attempts, Math.max(0, remaining));
                throw loginException;
            } finally {
                // 清除临时租户上下文
                securityContext.clearTempTenantId();
            }

            // 获取用户信息
            Long userId = StpUtil.getLoginIdAsLong();

            // 将用户名存入Sa-Token Session，供操作日志等切面获取
            StpUtil.getSession().set("username", dto.username());

            // 记录登录日志（成功）- 出错不影响登录
            try {
                loginLogService.recordLogin(tenantId, userId, dto.username(),
                        1, 0, null, loginIp, userAgent, tokenId);
            } catch (Exception logException) {
                log.warn("记录登录日志失败，不影响登录: {}", logException.getMessage());
            }

            result.put("token", token);
            result.put("tokenName", "Authorization");
            result.put("userId", userId);
            result.put("tenantId", tenantId);
            result.put("tenantName", dto.tenantName());

            // 返回当前用户可访问的租户列表（用于前端租户切换）
            List<SysTenant> userTenants = userService.getUserTenants(userId);
            result.put("tenants", userTenants.stream().map(t -> Map.of(
                "id", t.getId(),
                "tenantName", t.getTenantName(),
                "tenantCode", t.getTenantCode(),
                "status", t.getStatus()
            )).toList());

            log.info("用户登录成功: username={}, tenantName={}, ip={}", dto.username(), dto.tenantName(), loginIp);
            return Result.ok("登录成功", result);

        } catch (Exception e) {
            // 清除临时租户上下文（防止异常时残留）
            securityContext.clearTempTenantId();

            // 记录登录日志（失败）- 出错不影响登录失败提示
            try {
                loginLogService.recordLogin(null, null, dto.username(),
                        1, 1, e.getMessage(), loginIp, userAgent, null);
            } catch (Exception logException) {
                log.warn("记录登录失败日志异常: {}", logException.getMessage());
            }

            log.warn("用户登录失败: username={}, reason={}", dto.username(), e.getMessage());
            return Result.fail(401, e.getMessage());
        }
    }

    /**
     * 根据租户名称获取租户ID
     */
    private Long getTenantIdByName(String tenantName) {
        // 从数据库查询租户
        try {
            SysTenant tenant = tenantMapper.selectByTenantName(tenantName);
            if (tenant != null && tenant.getStatus() == 1 && tenant.getDeleted() == 0) {
                return tenant.getId();
            }
            // 也支持通过租户编码查询
            tenant = tenantMapper.selectByTenantCode(tenantName);
            if (tenant != null && tenant.getStatus() == 1 && tenant.getDeleted() == 0) {
                return tenant.getId();
            }
        } catch (Exception e) {
            log.warn("查询租户失败: tenantName={}, error={}", tenantName, e.getMessage());
        }
        return null;
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String token = StpUtil.getTokenValue();
        
        // 将Token加入黑名单（安全加固）
        if (token != null && !token.isEmpty()) {
            long ttl = StpUtil.getTokenTimeout();
            if (ttl > 0) {
                stpInterface.addToBlacklist(token, ttl);
            }
        }
        
        // 记录登出日志
        loginLogService.recordLogout(token);
        
        // 执行登出
        userService.logout();
        
        log.info("用户登出成功: userId={}", userId);
        return Result.ok("登出成功", null);
    }

    /**
     * 刷新Token
     */
    @Operation(summary = "刷新Token", description = "当前Token有效期内刷新，返回新Token")
    @PostMapping("/refresh")
    @SaCheckLogin
    public Result<Map<String, Object>> refreshToken() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // Sa-Token 会自动处理Token刷新
        String newToken = StpUtil.getTokenValue();
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("tokenName", "Authorization");
        
        log.info("Token刷新成功: userId={}", userId);
        return Result.ok("刷新成功", result);
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userinfo")
    @SaCheckLogin
    public Result<Map<String, Object>> getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", StpUtil.getSession().getString("username"));
        result.put("tenantId", StpUtil.getSession().get("tenantId"));
        result.put("roles", StpUtil.getRoleList());
        result.put("permissions", StpUtil.getPermissionList());
        result.put("billTypes", roleBillTypeService.getUserBillTypes(userId));
        result.put("passwordExpired", StpUtil.getSession().get("passwordExpired"));

        return Result.ok(result);
    }

    /**
     * 获取登录历史
     */
    @Operation(summary = "获取登录历史")
    @GetMapping("/login-history")
    @SaCheckLogin
    public Result<List<SysLoginLog>> getLoginHistory(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysLoginLog> logs = loginLogService.getRecentLogins(userId, limit);
        return Result.ok(logs);
    }

    /**
     * 检查Token有效性
     */
    @Operation(summary = "检查Token有效性")
    @GetMapping("/check")
    public Result<Map<String, Object>> checkToken() {
        Map<String, Object> result = new HashMap<>();
        
        if (StpUtil.isLogin()) {
            result.put("valid", true);
            result.put("userId", StpUtil.getLoginIdAsLong());
            result.put("tokenTimeout", StpUtil.getTokenTimeout());
        } else {
            result.put("valid", false);
        }
        
        return Result.ok(result);
    }

    /**
     * 获取当前用户可访问的租户列表（用于前端租户切换器）
     */
    @Operation(summary = "获取当前用户可访问的租户列表")
    @GetMapping("/tenants")
    @SaCheckLogin
    public Result<List<Map<String, Object>>> getUserTenants() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysTenant> tenants = userService.getUserTenants(userId);
        List<Map<String, Object>> result = tenants.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("tenantName", t.getTenantName());
            m.put("tenantCode", t.getTenantCode());
            m.put("status", t.getStatus());
            return m;
        }).toList();
        return Result.ok(result);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}