package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.IdentityDTO;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.service.MallAuthService;
import cn.aiedge.erp.b2b.support.MallGuestAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商城 C 端认证接口（登录 / 注册 / 登出 / 身份切换）。
 *
 * <h3>⚠️ 「在登哪家店」由服务端解析，不接受前端传参</h3>
 * 顾客是**系统级身份**，而"他属不属于这家店"是关联关系（{@code shop_user_tenant}）——
 * 所以登录/注册都必须先知道**本店租户**。来源与商品浏览完全一致：登录会话的租户，
 * 否则请求头 {@code X-Tenant-Id}（见 {@link MallGuestAccess}，商城 C 端本来就在带它）。
 *
 * <p>**刻意不用请求体里的字段**：请求体是客户端可任意构造的，
 * 让客户端自称"我在 A 店"等于让客户端决定去校验哪一条关联。</p>
 */
@RestController
@RequestMapping("/api/v1/mall/auth")
@Tag(name = "商城认证", description = "用户登录、注册、登出等认证接口")
@RequiredArgsConstructor
public class MallAuthController {

    private final MallAuthService mallAuthService;
    private final MallGuestAccess mallGuestAccess;

    @Operation(summary = "用户登录", description = "入店登录（必须在本店注册过）：获取 token 并返回所有可用身份")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = mallAuthService.login(request, mallGuestAccess.currentShopTenantId());
        return ApiResponse.success("登录成功", response);
    }

    @Operation(summary = "用户注册",
            description = "入店注册（两分支：系统里没有则新建系统顾客；已有则必须用原密码自证身份后绑定本店）")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody LoginRequest request) {
        mallAuthService.register(request, mallGuestAccess.currentShopTenantId());
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

    @Operation(summary = "获取可用身份列表", description = "返回当前用户可切换的所有身份（个人会员+企业客户）")
    @GetMapping("/identities")
    public ApiResponse<List<IdentityDTO>> listIdentities() {
        List<IdentityDTO> identities = mallAuthService.listIdentities();
        return ApiResponse.success("获取成功", identities);
    }

    @Operation(summary = "切换下单身份", description = "切换当前激活的下单身份，后续订单将以此身份创建")
    @PostMapping("/switch-identity")
    public ApiResponse<Void> switchIdentity(@RequestParam Long partyId) {
        mallAuthService.switchIdentity(partyId);
        return ApiResponse.success("切换成功", null);
    }
}
