package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
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
        // TODO: 接入 Sa-Token + SysUserService 实现真实认证
        //  参考: core-api 中 PermissionServiceImpl 的 login 方法
        //  当前为开发阶段临时实现，仅校验非空
        if (username.length() < 2) {
            return Result.fail("用户名至少2个字符");
        }
        if (password.length() < 6) {
            return Result.fail("密码至少6个字符");
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", "pda-token-" + System.currentTimeMillis());
        result.put("userId", 1L);
        result.put("userName", username);
        log.info("PDA登录: username={}", username);
        return Result.ok(result);
    }

    @Operation(summary = "登出")
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        log.info("PDA登出");
        return Result.ok();
    }
}
