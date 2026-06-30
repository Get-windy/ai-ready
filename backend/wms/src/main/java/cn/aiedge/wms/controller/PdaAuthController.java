package cn.aiedge.wms.controller;

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

    @Operation(summary = "仓库人员登录")
    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestParam @NotBlank String username,
                                             @RequestParam @NotBlank String password) {
        if (username.length() < 2) {
            return Result.fail("用户名至少2个字符");
        }
        if (password.length() < 6) {
            return Result.fail("密码至少6个字符");
        }

        // 使用 Sa-Token 进行登录认证
        // 通过用户名查询用户并验证密码（此处简化为直接登录，实际应查询数据库验证密码）
        // 使用用户名作为登录ID，实际生产环境应该查询 sys_user 表验证
        Long userId = resolveUserId(username, password);
        if (userId == null) {
            return Result.fail("用户名或密码错误");
        }

        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", token);
        result.put("userId", userId);
        result.put("userName", username);
        log.info("PDA登录成功: username={}, userId={}", username, userId);
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

    /**
     * 解析用户ID（简化实现）
     * 实际生产环境应注入 SysUserService 进行数据库查询和密码校验
     */
    private Long resolveUserId(String username, String password) {
        // 临时实现：使用用户名哈希作为userId，仅用于开发阶段
        // 生产环境应替换为：
        // SysUser user = sysUserService.findByUsername(username);
        // if (user != null && passwordEncoder.matches(password, user.getPassword())) {
        //     return user.getId();
        // }
        // return null;
        if (username.length() >= 2 && password.length() >= 6) {
            return (long) Math.abs(username.hashCode() % 10000) + 1;
        }
        return null;
    }
}
