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
 * <p><b>鉴权（2026-09-21 补，E-02 批次 4）：</b>本类原先**六个端点零权限注解**
 * （只有全局拦截器的登录校验），任何登录用户都能读改采购价格记录 ——
 * 而"最近采购价"属成本敏感数据。现统一挂 {@code purchase:price:edit}。</p>
 *
 * <p>⚠️ 读端点（{@code /page}、{@code /trend}）也用了这个写码：库里没有
 * {@code purchase:price-track:list} 之类的读码，而**宁可让读也走这道门，也不能把进价敞开**
 * —— 若只保护写，未授权的用户照样能翻到全量最近采购价。这是权衡后的临时口径，
 * 待该页单独设计码族（读/写分离）时替换，已登记 MASTER_TODO。</p>
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
    @SaCheckPermission("purchase:price:edit")
    public ApiResponse<IPage<PurchasePriceTrack>> page(PurchasePriceTrackQueryDTO query) {
        return ApiResponse.ok(priceTrackService.page(query));
    }

    @Operation(summary = "新增价格折扣")
    @PostMapping
    @SaCheckPermission("purchase:price:edit")
    public ApiResponse<PurchasePriceTrack> save(@Valid @RequestBody PurchasePriceTrackSaveDTO dto) {
        return ApiResponse.ok(priceTrackService.saveTrack(dto));
    }

    @Operation(summary = "修改价格记录")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:price:edit")
    public ApiResponse<PurchasePriceTrack> update(@PathVariable Long id,
                                                  @Valid @RequestBody PurchasePriceTrackSaveDTO dto) {
        return ApiResponse.ok(priceTrackService.updateTrack(id, dto));
    }

    @Operation(summary = "删除价格记录")
    @DeleteMapping("/{id}")
    @SaCheckPermission("purchase:price:edit")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        priceTrackService.deleteTrack(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除价格记录")
    @PostMapping("/batch-delete")
    @SaCheckPermission("purchase:price:edit")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        priceTrackService.batchDelete(ids);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询商品价格趋势")
    @GetMapping("/trend")
    @SaCheckPermission("purchase:price:edit")
    public ApiResponse<List<PurchasePriceTrendVO>> trend(@RequestParam Long productId) {
        return ApiResponse.ok(priceTrackService.trend(productId));
    }
}
