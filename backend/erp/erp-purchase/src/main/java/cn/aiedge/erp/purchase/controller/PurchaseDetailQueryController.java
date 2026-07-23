package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.PurchaseDetailListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseDetailQueryDTO;
import cn.aiedge.erp.purchase.service.PurchaseDetailQueryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 按明细Tab查询控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购订单-按明细查询", description = "列表页按明细Tab分页查询(59列)")
@RestController
@RequestMapping("/api/erp/purchase/order/detail-query")
@RequiredArgsConstructor
public class PurchaseDetailQueryController {

    private final PurchaseDetailQueryService purchaseDetailQueryService;

    @Operation(summary = "按明细Tab分页查询")
    @GetMapping("/page")
    public ApiResponse<Page<PurchaseDetailListDTO>> pageByDetail(PurchaseDetailQueryDTO query) {
        Page<PurchaseDetailListDTO> page = purchaseDetailQueryService.pageByDetail(query);
        return ApiResponse.ok(page);
    }
}
