package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.service.MallAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mall/auth")
@Tag(name = "商城认证", description = "用户登录、注册、登出等认证接口")
@RequiredArgsConstructor
public class MallAuthController {

    private final MallAuthService mallAuthService;

    @Operation(summary = "用户登录", description = "用户登录获取token")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = mallAuthService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    @Operation(summary = "用户注册", description = "新用户注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody LoginRequest request) {
        mallAuthService.register(request);
        return ApiResponse.success("注册成功", null);
    }

    @Operation(summary = "用户登出", description = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        mallAuthService.logout();
        return ApiResponse.success("登出成功", null);
    }

    @Operation(summary = "刷新token", description = "刷新用户token")
    @PostMapping("/refresh-token")
    public ApiResponse<String> refreshToken() {
        String token = mallAuthService.refreshToken();
        return ApiResponse.success("Token刷新成功", token);
    }
}
