package cn.aiedge.erp.sale.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.dto.SalePriceTrackQueryDTO;
import cn.aiedge.erp.sale.dto.SalePriceTrackSaveDTO;
import cn.aiedge.erp.sale.dto.SalePriceTrendVO;
import cn.aiedge.erp.sale.dto.SalesDetailQueryDTO;
import cn.aiedge.erp.sale.entity.SalePriceTrack;
import cn.aiedge.erp.sale.service.SalePriceTrackService;
import cn.aiedge.erp.sale.service.SalesDetailQueryService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 销售价格跟踪控制器
 * <p>
 * 列表（商品×往来单位最近销售价）+ 价格折扣新增 + 修改/删除 + 价格趋势。
 * 另保留销售明细实时聚合接口（未维护价格时的旁证口径）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/sales/price-track")
@RequiredArgsConstructor
@Tag(name = "销售价格跟踪", description = "销售价格跟踪：最近销售价列表、价格折扣维护、价格趋势")
public class SalesPriceTrackController {

    private final SalePriceTrackService priceTrackService;
    private final SalesDetailQueryService salesDetailQueryService;

    @Operation(summary = "分页查询价格跟踪列表")
    @GetMapping("/page")
    public ApiResponse<IPage<SalePriceTrack>> page(SalePriceTrackQueryDTO query) {
        return ApiResponse.ok(priceTrackService.page(query));
    }

    @Operation(summary = "新增价格折扣")
    @PostMapping
    public ApiResponse<SalePriceTrack> save(@Valid @RequestBody SalePriceTrackSaveDTO dto) {
        return ApiResponse.ok(priceTrackService.saveTrack(dto));
    }

    @Operation(summary = "修改价格记录")
    @PutMapping("/{id}")
    public ApiResponse<SalePriceTrack> update(@PathVariable Long id,
                                              @Valid @RequestBody SalePriceTrackSaveDTO dto) {
        return ApiResponse.ok(priceTrackService.updateTrack(id, dto));
    }

    @Operation(summary = "删除价格记录")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        priceTrackService.deleteTrack(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除价格记录")
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        priceTrackService.batchDelete(ids);
        return ApiResponse.ok();
    }

    @Operation(summary = "查询商品销售价格趋势")
    @GetMapping("/trend")
    public ApiResponse<List<SalePriceTrendVO>> trend(@RequestParam Long productId) {
        return ApiResponse.ok(priceTrackService.trend(productId));
    }

    @Operation(summary = "最近成交价实时聚合（商品×往来单位，直读销售出库明细）")
    @GetMapping("/recent-price/page")
    public Page<Map<String, Object>> recentPricePage(SalesDetailQueryDTO queryDTO) {
        if (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) {
            queryDTO.setCurrent(1L);
        }
        if (queryDTO.getSize() == null || queryDTO.getSize() <= 0) {
            queryDTO.setSize(20L);
        }
        return salesDetailQueryService.pageRecentPriceAgg(queryDTO);
    }
}
