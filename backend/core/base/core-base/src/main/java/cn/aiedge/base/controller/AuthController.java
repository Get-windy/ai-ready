package cn.aiedge.base.controller;

import cn.aiedge.base.dto.AuthDTO;
import cn.aiedge.base.entity.SysLoginLog;
import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.SysLoginLogService;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.service.message.SmsSender;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.ratelimit.RateLimit;
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
import java.util.concurrent.TimeUnit;

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

    /** 登录用短信验证码：Redis 键前缀、有效期（分钟）、同一手机号重发间隔（秒） */
    private static final String SMS_CODE_PREFIX = "login:sms:code:";
    private static final String SMS_CODE_LIMIT_PREFIX = "login:sms:limit:";
    private static final long SMS_CODE_TTL_MINUTES = 5;
    private static final long SMS_CODE_RESEND_SECONDS = 60;

    private final SysUserService userService;
    private final SysLoginLogService loginLogService;
    private final cn.aiedge.base.security.StpInterfaceImpl stpInterface;
    private final cn.aiedge.base.service.LoginFlowService loginFlowService;
    private final cn.aiedge.base.service.RoleBillTypeService roleBillTypeService;
    private final cn.aiedge.base.security.LoginAttemptService loginAttemptService;
    /** 存放「待选企业」票据：多企业登录时先验身份，待用户选定企业后再签发 Token */
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    /** 短信通道（手机号验证码登录用） */
    private final SmsSender smsSender;

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
    @Operation(summary = "用户登录", description = "账号密码登录；仅一个企业直接返回Token，多个企业返回候选企业列表待选择")
    @RateLimit(key = "auth:login", type = RateLimit.LimitType.IP, qps = 1, capacity = 10, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @Valid @RequestBody AuthDTO.Login dto,
            HttpServletRequest request) {

        String loginIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 验证码校验
        if (dto.captcha() == null || dto.captcha().isBlank()) {
            return Result.fail(401, "请输入验证码");
        }
        if (dto.captchaKey() == null || dto.captchaKey().isBlank()) {
            return Result.fail(401, "验证码Key无效");
        }
        String cachedCode = captchaCache.remove(dto.captchaKey());
        if (cachedCode == null) {
            return Result.fail(401, "验证码已过期，请刷新");
        }
        if (!cachedCode.equalsIgnoreCase(dto.captcha().trim())) {
            return Result.fail(401, "验证码错误");
        }

        // 账户锁定检查：企业不再由用户输入，故按用户名全局计数（tenantId 传 null）
        if (loginAttemptService.isLocked(dto.username(), null)) {
            long remaining = loginAttemptService.getLockRemainingSeconds(dto.username(), null);
            String msg = "账户已被临时锁定，请" + (remaining / 60 + 1) + "分钟后再试";
            log.warn("登录失败（账户已锁定）: username={}, remaining={}s", dto.username(), remaining);
            return Result.fail(401, msg);
        }

        try {
            // 先确认身份：用户名全局唯一，不涉及企业
            SysUser user = userService.verifyCredentials(dto.username(), dto.password());
            loginAttemptService.clearFailedAttempts(dto.username(), null);
            // 身份已确认 → 按可访问企业数量决定「直接登入」还是「返回候选待选」
            return proceedAfterIdentify(user, dto.username(), loginIp, userAgent);

        } catch (Exception e) {
            // 记录失败次数与日志（出错不影响失败提示）
            int attempts = loginAttemptService.recordFailedAttempt(dto.username(), null);
            log.warn("登录失败: username={}, attempts={}, reason={}", dto.username(), attempts, e.getMessage());
            try {
                loginLogService.recordLogin(null, null, dto.username(),
                        1, 1, e.getMessage(), loginIp, userAgent, null);
            } catch (Exception logException) {
                log.warn("记录登录失败日志异常: {}", logException.getMessage());
            }
            return Result.fail(401, e.getMessage());
        }
    }

    /**
     * 发送登录短信验证码
     * <p>
     * 通道不可用时**拒绝发码**（而不是写一个永远收不到的码）：{@code SmsSenderImpl}
     * 未配置 {@code sms.endpoint} 时会给出明确原因，此处原样透出，便于运维定位。
     * </p>
     */
    @Operation(summary = "发送登录验证码", description = "向手机号发送登录用短信验证码")
    @RateLimit(key = "auth:sms-code", type = RateLimit.LimitType.IP, qps = 1, capacity = 5, message = "发送过于频繁，请稍后再试")
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody AuthDTO.SendSmsCode dto) {
        String phone = dto.phone();

        // 重发间隔：防刷，也防运营商把短时间大量同内容短信判为骚扰
        String limitKey = SMS_CODE_LIMIT_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long ttl = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            long wait = (ttl != null && ttl > 0) ? ttl : SMS_CODE_RESEND_SECONDS;
            return Result.fail(429, "请求过于频繁，请 " + wait + " 秒后再试");
        }

        if (!smsSender.configured()) {
            log.warn("[短信验证码] 通道未配置，拒绝发码: phone={}", maskPhone(phone));
            return Result.fail(503, "短信通道未配置，请联系管理员在「系统 → 平台设置 → 短信配置」中维护");
        }

        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code,
                SMS_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", SMS_CODE_RESEND_SECONDS, TimeUnit.SECONDS);

        SysMessage message = new SysMessage();
        message.setReceiverContact(phone);
        message.setTitle("登录验证码");
        message.setContent("【登录验证码】您的验证码是 " + code + "，"
                + SMS_CODE_TTL_MINUTES + " 分钟内有效，请勿泄露。");

        if (!smsSender.send(message)) {
            // 发送失败：撤掉刚写的码与重发锁，让用户可以立刻重试
            redisTemplate.delete(SMS_CODE_PREFIX + phone);
            redisTemplate.delete(limitKey);
            String reason = smsSender.failureReason();
            log.warn("[短信验证码] 发送失败: phone={}, reason={}", maskPhone(phone), reason);
            return Result.fail(503, "验证码发送失败：" + (reason == null ? "短信通道异常" : reason));
        }

        log.info("[短信验证码] 已发送: phone={}", maskPhone(phone));
        return Result.ok("验证码已发送", null);
    }

    /**
     * 手机号验证码登录
     * <p>与账号密码登录同口径：身份确认后同样走「单企业直接登入 / 多企业待选」分流。</p>
     */
    @Operation(summary = "手机号验证码登录", description = "手机号 + 短信验证码登录，支持单/多企业分流")
    @RateLimit(key = "auth:login-sms", type = RateLimit.LimitType.IP, qps = 1, capacity = 10, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login-by-sms")
    public Result<Map<String, Object>> loginBySms(@Valid @RequestBody AuthDTO.LoginBySms dto,
                                                  HttpServletRequest request) {
        String loginIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 锁定检查：手机号同样全局计数
        if (loginAttemptService.isLocked(dto.phone(), null)) {
            long remaining = loginAttemptService.getLockRemainingSeconds(dto.phone(), null);
            String msg = "账户已被临时锁定，请" + (remaining / 60 + 1) + "分钟后再试";
            log.warn("短信登录失败（账户已锁定）: phone={}, remaining={}s", maskPhone(dto.phone()), remaining);
            return Result.fail(401, msg);
        }

        try {
            // 验证码一次性：校验通过即作废，防重放
            if (!consumeSmsCode(dto.phone(), dto.smsCode())) {
                throw BusinessException.badRequest("验证码错误或已过期");
            }

            SysUser user = userService.findByPhoneForLogin(dto.phone());
            loginAttemptService.clearFailedAttempts(dto.phone(), null);
            return proceedAfterIdentify(user, maskPhone(dto.phone()), loginIp, userAgent);

        } catch (Exception e) {
            int attempts = loginAttemptService.recordFailedAttempt(dto.phone(), null);
            log.warn("短信登录失败: phone={}, attempts={}, reason={}",
                    maskPhone(dto.phone()), attempts, e.getMessage());
            try {
                loginLogService.recordLogin(null, null, dto.phone(),
                        1, 1, e.getMessage(), loginIp, userAgent, null);
            } catch (Exception logException) {
                log.warn("记录登录失败日志异常: {}", logException.getMessage());
            }
            return Result.fail(401, e.getMessage());
        }
    }

    /**
     * 身份已确认后的分流（委托 {@link cn.aiedge.base.service.LoginFlowService}）。
     * <p>账号密码、短信验证码、三方扫码三条登录路径共用同一实现，
     * 保证「单企业直接登入 / 多企业候选待选」的行为完全一致。</p>
     */
    private Result<Map<String, Object>> proceedAfterIdentify(SysUser user, String accountLabel,
                                                             String loginIp, String userAgent) {
        cn.aiedge.base.service.LoginFlowService.Outcome outcome = loginFlowService.proceed(user, loginIp);

        if (!outcome.needSelectTenant()) {
            loginFlowService.recordLoginSuccess(user, outcome.tenant().getId(), loginIp, userAgent);
            return Result.ok("登录成功", loginFlowService.buildLoginResult(outcome));
        }

        log.info("登录待选企业: account={}, 候选企业数={}", accountLabel, outcome.tenants().size());
        return Result.ok("请选择要登录的企业", loginFlowService.buildSelectTenantResult(outcome));
    }

    /**
     * 校验并消费短信验证码（一次性）
     */
    private boolean consumeSmsCode(String phone, String inputCode) {
        if (inputCode == null || inputCode.isBlank()) {
            return false;
        }
        String key = SMS_CODE_PREFIX + phone;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null || !cached.equals(inputCode.trim())) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }

    /**
     * 手机号脱敏（日志用，避免日志里成片明文手机号）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 选择企业完成登录（多企业用户的登录第二步）
     * <p>凭第一步返回的票据换取 Token；票据一次性消费、5 分钟有效。</p>
     */
    @Operation(summary = "选择企业完成登录", description = "多企业用户登录第二步：凭登录票据 + 企业ID 换取 Token")
    @PostMapping("/select-tenant")
    public Result<Map<String, Object>> selectTenant(
            @Valid @RequestBody AuthDTO.SelectTenant dto,
            HttpServletRequest request) {

        String loginIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 票据一次性消费：先取后删，避免同一票据被重放
        String key = cn.aiedge.base.service.LoginFlowService.SELECT_TOKEN_PREFIX + dto.selectToken();
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            return Result.fail(401, "登录已超时，请重新登录");
        }
        redisTemplate.delete(key);

        Long userId = Long.parseLong(userIdStr);
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() != 1) {
            return Result.fail(401, "账号状态异常，请重新登录");
        }

        List<SysTenant> tenants = userService.getUserTenants(userId);
        SysTenant tenant = tenants.stream()
                .filter(t -> t.getId().equals(dto.tenantId()))
                .findFirst()
                .orElse(null);
        if (tenant == null) {
            log.warn("选择企业被拒（企业不在该账号可访问范围内）: userId={}, tenantId={}", userId, dto.tenantId());
            return Result.fail(403, "无权登录所选企业");
        }

        String token = loginFlowService.loginInTenant(user, tenant, loginIp);
        loginFlowService.recordLoginSuccess(user, tenant.getId(), loginIp, userAgent);
        cn.aiedge.base.service.LoginFlowService.Outcome outcome =
                new cn.aiedge.base.service.LoginFlowService.Outcome(
                        false, token, userId, tenant, tenants, null, null);
        return Result.ok("登录成功", loginFlowService.buildLoginResult(outcome));
    }

    /**
     * 切换企业（已登录用户在其可访问的企业之间切换）
     * <p>
     * 必须改<b>会话租户</b>，不能只改前端 localStorage：{@code TenantHeaderInterceptor}
     * 会在请求头与会话租户不一致时直接 403。
     * </p>
     */
    @Operation(summary = "切换企业", description = "在当前账号可访问的企业之间切换会话身份")
    @PostMapping("/switch-tenant")
    @SaCheckLogin
    public Result<Map<String, Object>> switchTenant(@Valid @RequestBody AuthDTO.SwitchTenant dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        List<SysTenant> tenants = userService.getUserTenants(userId);
        SysTenant tenant = tenants.stream()
                .filter(t -> t.getId().equals(dto.tenantId()))
                .findFirst()
                .orElse(null);
        if (tenant == null) {
            log.warn("切换企业被拒（企业不在该账号可访问范围内）: userId={}, tenantId={}", userId, dto.tenantId());
            return Result.fail(403, "无权切换到所选企业");
        }

        Long from = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        if (tenant.getId().equals(from)) {
            return Result.ok("当前已是「" + tenant.getTenantName() + "」", buildSwitchResult(tenant, tenants));
        }

        // 切换会话租户：租户插件与 TenantHeaderInterceptor 都以它为准
        StpUtil.getSession().set("tenantId", tenant.getId());
        // 角色与权限都是按租户查的：换企业后必须清缓存并重算超管豁免，否则沿用旧企业的身份
        stpInterface.clearUserPermissionCache(userId);
        StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"));
        // 记录「上次登录该企业」→ 下次登录它排在第一位
        userService.touchTenantLoginTime(userId, tenant.getId());

        log.info("切换企业: userId={}, from={}, to={}", userId, from, tenant.getId());
        return Result.ok("已切换到「" + tenant.getTenantName() + "」", buildSwitchResult(tenant, tenants));
    }

    // ========== 登录链路内部辅助方法 ==========

    /**
     * 切换企业的返回体（不含 Token：会话不变，仅租户上下文变更）
     */
    private Map<String, Object> buildSwitchResult(SysTenant tenant, List<SysTenant> tenants) {
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenant.getId());
        result.put("tenantName", tenant.getTenantName());
        result.put("tenants", loginFlowService.toTenantOptions(tenants));
        return result;
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

        try {
            // 注意：此接口在 SaTokenConfig 中被 excludePathPatterns 排除，
            // 拦截器不会自动解析 token，因此需要手动调用 checkLogin() 触发 token 解析
            StpUtil.checkLogin();

            result.put("valid", true);
            result.put("userId", StpUtil.getLoginIdAsLong());
            result.put("tokenTimeout", StpUtil.getTokenTimeout());
        } catch (Exception e) {
            // Token 无效或已过期
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