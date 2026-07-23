package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.PurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderListDTO;
import cn.aiedge.erp.purchase.service.PurchaseDocQueryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 按单据Tab查询控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购订单-按单据查询", description = "列表页按单据Tab分页查询(39列)")
@RestController
@RequestMapping("/api/erp/purchase/order/doc-query")
@RequiredArgsConstructor
public class PurchaseDocQueryController {

    private final PurchaseDocQueryService purchaseDocQueryService;

    @Operation(summary = "按单据Tab分页查询")
    @GetMapping("/page")
    public ApiResponse<Page<PurchaseOrderListDTO>> pageByDoc(PurchaseDocQueryDTO query) {
        Page<PurchaseOrderListDTO> page = purchaseDocQueryService.pageByDoc(query);
        return ApiResponse.ok(page);
    }
}
