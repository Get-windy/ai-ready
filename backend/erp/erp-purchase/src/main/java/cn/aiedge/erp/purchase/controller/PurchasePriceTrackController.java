package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrackQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrackSaveDTO;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrendVO;
import cn.aiedge.erp.purchase.entity.PurchasePriceTrack;
import cn.aiedge.erp.purchase.service.PurchasePriceTrackService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购价格跟踪控制器
 * <p>
 * 列表（商品×往来单位最近采购价）+ 价格折扣新增 + 修改/删除 + 价格趋势。
 *
 * <p><b>鉴权（2026-09-21 补，E-02 批次 4 / 2026-09-23 换码）：</b>本类原先**六个端点零权限注解**
 * （只有全局拦截器的登录校验），任何登录用户都能读改采购价格记录 ——
 * 而"最近采购价"属成本敏感数据。</p>
 *
 * <p>2026-09-21 曾临时统一挂粗粒度遗留码 {@code purchase:price:edit}，理由是"库里没有
 * price-track 的读码"。该前提**已失效**：V11.460.0 已按 URL 种下
 * {@code purchase:price-track:{list,view,create,update,delete}} 五个码（均已授超管），
 * 于是本条从"临时口径"变成"读不走读码、且该页菜单按 price-track 前缀派生可见性，
 * 持真实码的用户反而看不到菜单"。现按资源:动作拆开，读写分离。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/purchase/price-track")
@RequiredArgsConstructor
@Tag(name = "采购价格跟踪", description = "采购价格跟踪：最近采购价列表、价格折扣维护、价格趋势")
public class PurchasePriceTrackController {

    private final PurchasePriceTrackService priceTrackService;

    @Operation(summary = "分页查询价格跟踪列表")
    @GetMapping("/page")
    @SaCheckPermission("purchase:price-track:list")
    public ApiResponse<IPage<PurchasePriceTrack>> page(PurchasePriceTrackQueryDTO query) {
        return ApiResponse.ok(priceTrackService.page(query));
    }

    @Operation(summary = "新增价格折扣")
    @PostMapping
    @SaCheckPermission("purchase:price-track:create")
    public ApiResponse<PurchasePriceTrack> save(@Valid @RequestBody PurchasePriceTrackSaveDTO dto) {
        return ApiResponse.ok(priceTrackService.saveTrack(dto));
    }

    @Operation(summary = "修改价格记录")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:price-track:update")
    public ApiResponse<PurchasePriceTrack> update(@PathVariable Long id,
                                                  @Valid @RequestBody PurchasePriceTrackSaveDTO dto) {
        return ApiResponse.ok(priceTrackService.updateTrack(id, dto));
    }

    @Operation(summary = "删除价格记录")
    @DeleteMapping("/{id}")
    @SaCheckPermission("purchase:price-track:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        priceTrackService.deleteTrack(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除价格记录")
    @PostMapping("/batch-delete")
    @SaCheckPermission("purchase:price-track:delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        priceTrackService.batchDelete(ids);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询商品价格趋势")
    @GetMapping("/trend")
    @SaCheckPermission("purchase:price-track:view")
    public ApiResponse<List<PurchasePriceTrendVO>> trend(@RequestParam Long productId) {
        return ApiResponse.ok(priceTrackService.trend(productId));
    }
}
