package cn.aiedge.base.controller;

import cn.aiedge.base.dto.AuthDTO;
import cn.aiedge.base.entity.SysLoginLog;
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
            // 执行登录
            String token = userService.login(dto.username(), dto.password(), dto.tenantId(), loginIp);
            
            // 获取用户信息
            Long userId = StpUtil.getLoginIdAsLong();
            
            // 记录登录日志（成功）
            loginLogService.recordLogin(dto.tenantId(), userId, dto.username(), 
                    1, 0, null, loginIp, userAgent, tokenId);

            result.put("token", token);
            result.put("tokenName", "Authorization");
            result.put("userId", userId);
            
            log.info("用户登录成功: username={}, ip={}", dto.username(), loginIp);
            return Result.ok("登录成功", result);

        } catch (Exception e) {
            // 记录登录日志（失败）
            loginLogService.recordLogin(dto.tenantId(), null, dto.username(), 
                    1, 1, e.getMessage(), loginIp, userAgent, null);
            
            log.warn("用户登录失败: username={}, reason={}", dto.username(), e.getMessage());
            return Result.fail(401, e.getMessage());
        }
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
        result.put("username", StpUtil.getLoginId());
        result.put("roles", StpUtil.getRoleList());
        result.put("permissions", StpUtil.getPermissionList());
        
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