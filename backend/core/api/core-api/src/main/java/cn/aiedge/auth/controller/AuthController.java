package cn.aiedge.auth.controller;

import cn.aiedge.base.service.AuthService;
import cn.aiedge.common.dto.auth.LoginRequest;
import cn.aiedge.common.dto.auth.LoginVO;
import cn.aiedge.common.result.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "登录、登出等认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return ApiResponse.ok(authService.login(request, clientIp));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok("登出成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public ApiResponse<LoginVO.UserInfo> getCurrentUserInfo() {
        return ApiResponse.ok(authService.getCurrentUserInfo());
    }

    @Operation(summary = "检查用户名是否可用")
    @GetMapping("/check-username")
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        return ApiResponse.ok(authService.checkUsernameAvailable(username));
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
