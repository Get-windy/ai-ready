package cn.aiedge.wms.controller;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.security.PasswordEncryptor;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse")
@Tag(name = "PDA-认证")
@RequiredArgsConstructor
public class PdaAuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncryptor passwordEncryptor;

    @Operation(summary = "仓库人员登录")
    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestParam @NotBlank String username,
                                             @RequestParam @NotBlank String password) {
        if (username.length() < 2) {
            return Result.fail(400, "用户名至少2个字符");
        }
        if (password.length() < 6) {
            return Result.fail(400, "密码至少6个字符");
        }

        // 2026-09-20 由桩实现改为真实鉴权：此前 resolveUserId() 用 username.hashCode() 造 userId、
        // 且完全不校验密码（只查长度），任何人凭任意凭据即可登录取到 token —— 属认证绕过。
        // 现与主站登录（SysUserServiceImpl#login）同口径。
        // tenantId 传 null：用户名全局唯一，登录阶段还拿不到租户。
        SysUser user = sysUserMapper.selectByUsername(username, null);
        if (user == null || !passwordEncryptor.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }
        // sys_user.status 语义是「1=启用，0=禁用/待审批」
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail(403, "用户已禁用或锁定");
        }
        Long tenantId = user.getTenantId();
        if (tenantId == null) {
            return Result.fail(403, "用户未绑定租户，无法登录");
        }

        StpUtil.login(user.getId());
        // 与主站登录同口径：必须写租户上下文。漏写时该会话会被租户拦截器判为
        // 「已登录但无租户」，查询/写入全部落空（fail-closed）——不是能看别人数据，而是什么都看不到。
        StpUtil.getSession().set("tenantId", tenantId);
        StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"));
        String token = StpUtil.getTokenValue();

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("userName", user.getUsername());
        result.put("tenantId", tenantId);
        log.info("PDA登录成功: username={}, userId={}, tenantId={}", username, user.getId(), tenantId);
        return Result.ok(result);
    }

    @Operation(summary = "登出")
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("PDA登出: userId={}", userId);
        }
        return Result.ok();
    }
}
