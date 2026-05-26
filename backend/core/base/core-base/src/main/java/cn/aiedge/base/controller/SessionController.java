package cn.aiedge.base.controller;

import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 * 提供会话查询、强制下线等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "会话管理")
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    /**
     * 获取当前会话信息
     */
    @Operation(summary = "获取当前会话信息")
    @GetMapping("/current")
    @SaCheckLogin
    public Result<Map<String, Object>> getCurrentSession() {
        Long userId = StpUtil.getLoginIdAsLong();
        SaSession session = StpUtil.getSession();
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("tokenValue", StpUtil.getTokenValue());
        result.put("tokenTimeout", StpUtil.getTokenTimeout());
        result.put("sessionTimeout", session.getTimeout());
        result.put("createTime", session.getCreateTime());
        
        return Result.ok(result);
    }

    /**
     * 获取所有在线用户
     */
    @Operation(summary = "获取所有在线用户")
    @GetMapping("/online")
    @SaCheckPermission("session:view")
    public Result<List<String>> getOnlineUsers() {
        List<String> onlineUsers = StpUtil.searchTokenValue("", 0, -1, false);
        return Result.ok(onlineUsers);
    }

    /**
     * 强制用户下线
     */
    @Operation(summary = "强制用户下线")
    @PostMapping("/kickout/{userId}")
    @SaCheckPermission("session:kickout")
    public Result<Void> kickoutUser(@PathVariable Long userId) {
        StpUtil.kickout(userId);
        log.info("强制用户下线: userId={}", userId);
        return Result.ok("操作成功", null);
    }

    /**
     * 强制所有设备下线
     */
    @Operation(summary = "强制用户所有设备下线")
    @PostMapping("/kickout-all/{userId}")
    @SaCheckPermission("session:kickout")
    public Result<Void> kickoutAllDevices(@PathVariable Long userId) {
        StpUtil.kickout(userId);
        log.info("强制用户所有设备下线: userId={}", userId);
        return Result.ok("操作成功", null);
    }

    /**
     * 禁用账号
     */
    @Operation(summary = "禁用账号")
    @PostMapping("/disable/{userId}")
    @SaCheckPermission("session:disable")
    public Result<Void> disableUser(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "86400") long time) {
        StpUtil.disable(userId, time);
        log.info("禁用账号: userId={}, time={}s", userId, time);
        return Result.ok("操作成功", null);
    }

    /**
     * 解除账号禁用
     */
    @Operation(summary = "解除账号禁用")
    @PostMapping("/enable/{userId}")
    @SaCheckPermission("session:disable")
    public Result<Void> enableUser(@PathVariable Long userId) {
        StpUtil.untieDisable(userId);
        log.info("解除账号禁用: userId={}", userId);
        return Result.ok("操作成功", null);
    }

    /**
     * 检查账号是否被禁用
     */
    @Operation(summary = "检查账号是否被禁用")
    @GetMapping("/disabled/{userId}")
    @SaCheckPermission("session:view")
    public Result<Map<String, Object>> checkDisabled(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("disabled", StpUtil.isDisable(userId));
        result.put("disableTime", StpUtil.getDisableTime(userId));
        return Result.ok(result);
    }

    /**
     * 获取用户Token列表
     */
    @Operation(summary = "获取用户Token列表")
    @GetMapping("/tokens/{userId}")
    @SaCheckPermission("session:view")
    public Result<List<String>> getUserTokens(@PathVariable Long userId) {
        List<String> tokens = StpUtil.getTokenValueListByLoginId(userId);
        return Result.ok(tokens);
    }

    /**
     * 强制Token下线
     */
    @Operation(summary = "强制指定Token下线")
    @PostMapping("/kickout-token")
    @SaCheckPermission("session:kickout")
    public Result<Void> kickoutToken(@RequestParam String token) {
        StpUtil.kickoutByTokenValue(token);
        log.info("强制Token下线: token={}", maskToken(token));
        return Result.ok("操作成功", null);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 16) {
            return "****";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }
}
