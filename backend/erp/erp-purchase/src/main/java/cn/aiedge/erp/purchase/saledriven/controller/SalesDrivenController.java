package cn.aiedge.erp.purchase.saledriven.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenPurchaseResult;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenQueryDTO;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenRowDTO;
import cn.aiedge.erp.purchase.saledriven.service.SalesDrivenService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 以销定购控制器
 * <p>
 * 以销定购页面：按销售订单查看待采购队列，支持「采购成品」与「采购原料」，
 * 二者均自动按商品默认供应商生成采购订单并提交审批。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "以销定购", description = "销售订单视角的采购准备：查询+按销售单生成采购单")
@RestController
@RequestMapping("/api/erp/purchase/sales-driven")
@RequiredArgsConstructor
@SaCheckLogin
public class SalesDrivenController {

    private final SalesDrivenService salesDrivenService;

    /**
     * 以销定购-销售订单列表分页（42列 + 订金 + 是否已采购）
     */
    @Operation(summary = "以销定购列表")
    @GetMapping("/page")
    public ApiResponse<Page<SalesDrivenRowDTO>> page(SalesDrivenQueryDTO query) {
        return ApiResponse.ok(salesDrivenService.page(query));
    }

    /**
     * 采购成品：按销售订单商品生成采购订单并提交
     */
    @Operation(summary = "采购成品")
    @PostMapping("/{id}/purchase-finished")
    public ApiResponse<SalesDrivenPurchaseResult> purchaseFinished(@PathVariable Long id) {
        return ApiResponse.ok(salesDrivenService.purchase(id, SalesDrivenService.MODE_FINISHED));
    }

    /**
     * 采购原料：按销售订单商品 BOM 拆解原料生成采购订单并提交
     */
    @Operation(summary = "采购原料")
    @PostMapping("/{id}/purchase-material")
    public ApiResponse<SalesDrivenPurchaseResult> purchaseMaterial(@PathVariable Long id) {
        return ApiResponse.ok(salesDrivenService.purchase(id, SalesDrivenService.MODE_MATERIAL));
    }
}
