package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.*;
import cn.aiedge.erp.b2b.service.MallOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mall/orders")
@Tag(name = "商城订单", description = "订单创建、查询、取消等接口")
@RequiredArgsConstructor
public class MallOrderController {

    private final MallOrderService mallOrderService;

    @Operation(summary = "创建订单", description = "从购物车创建新订单")
    @PostMapping
    public ApiResponse<OrderDetailDTO> createOrder(@RequestBody OrderCreateRequest request) {
        OrderDetailDTO order = mallOrderService.createOrder(request);
        return ApiResponse.success("订单创建成功", order);
    }

    @Operation(summary = "获取订单列表", description = "分页获取当前用户的订单列表")
    @GetMapping
    public ApiResponse<PageResult<OrderListDTO>> listOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "订单状态") @RequestParam(required = false) String orderStatus) {
        PageResult<OrderListDTO> result = mallOrderService.listOrders(page, size, orderStatus);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取订单详情", description = "根据ID获取订单详细信息")
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        OrderDetailDTO detail = mallOrderService.getOrderDetail(id);
        return ApiResponse.success(detail);
    }

    @Operation(summary = "取消订单", description = "取消指定的订单")
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        mallOrderService.cancelOrder(id);
        return ApiResponse.success("订单已取消", null);
    }

    @Operation(summary = "确认收货", description = "确认收货完成订单")
    @PutMapping("/{id}/confirm")
    public ApiResponse<Void> confirmOrder(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        mallOrderService.confirmOrder(id);
        return ApiResponse.success("已确认收货", null);
    }

    // ⚠️ 2026-09-23 移除三个「买家端」端点：POST /{id}/pay、PUT /{id}/approve、PUT /{id}/reject。
    //   · 它们只校验「已登录」，且服务实现不校验订单归属 ⇒ 任意登录用户可按 id 把
    //     别人的（或自己的）订单置为已付款 / 审核通过（详见 TRADE_MODULE_AUDIT_20260923.md P0-5、P2-5）；
    //   · payOrder 只改状态、不产生支付记录，等于"白拿单"通道；
    //   · approve/reject 是审核动作，管理端已有带 @SaCheckPermission 的等价端点
    //     （/erp/mall/admin/order/{id}/approve|reject），不应在买家端重复暴露；
    //   · 三者在前端两个应用里均无调用方（pc-admin 用管理端端点，mobile-mall 未接）。
    //   将来做 C 端支付时，应走 core-payment 的 createPayment + 渠道回调驱动订单状态。

    @Operation(summary = "支付方式列表", description = "获取可用的支付方式列表")
    @GetMapping("/payment-methods")
    public ApiResponse<List<Map<String, Object>>> getPaymentMethods() {
        List<Map<String, Object>> methods = mallOrderService.getPaymentMethods();
        return ApiResponse.success(methods);
    }
}
