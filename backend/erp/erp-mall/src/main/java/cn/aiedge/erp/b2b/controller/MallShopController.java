package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.ShopConfigVO;
import cn.aiedge.erp.b2b.service.MallShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城 C 端「店铺」接口。
 *
 * <p><b>鉴权口径</b>：<b>刻意不加</b> {@code @SaCheckPermission}，也不要求登录 ——
 * 商城首屏（店铺名/logo/主题色/公告/价格开关）必须在**未登录**时就能渲染，
 * 否则游客进店只能看到一片空白。真正的准入在 {@code MallGuestAccess.requireShop()}：
 * 取不到店铺报 400、店铺未开放游客报 403、无配置行 fail-closed。</p>
 *
 * <p>⚠️ 因此本接口**必须**在 {@code SaTokenConfig} 的匿名白名单里登记
 * （{@code /api/v1/mall/shop/**}），否则会被 SaInterceptor 拦成 401。</p>
 *
 * <p>返回体为 {@link ShopConfigVO}（白名单），非实体 —— 凭据类字段不下发。</p>
 */
@RestController
@RequestMapping("/api/v1/mall/shop")
@Tag(name = "商城-店铺", description = "店铺配置下发（游客可达）")
@RequiredArgsConstructor
public class MallShopController {

    private final MallShopService mallShopService;

    @Operation(summary = "店铺配置", description = "下发店铺名称/装修/展示与交易开关（白名单字段，不含任何凭据）")
    @GetMapping("/config")
    public ApiResponse<ShopConfigVO> config() {
        return ApiResponse.success(mallShopService.currentShopConfig());
    }
}
