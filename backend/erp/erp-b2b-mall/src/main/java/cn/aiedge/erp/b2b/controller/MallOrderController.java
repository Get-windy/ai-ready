package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.*;
import cn.aiedge.erp.b2b.service.MallOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "支付订单", description = "发起订单支付")
    @PostMapping("/{id}/pay")
    public ApiResponse<Void> payOrder(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        mallOrderService.payOrder(id);
        return ApiResponse.success("支付成功", null);
    }

    @Operation(summary = "支付方式列表", description = "获取可用的支付方式列表")
    @GetMapping("/payment-methods")
    public ApiResponse<List> getPaymentMethods() {
        List methods = mallOrderService.getPaymentMethods();
        return ApiResponse.success(methods);
    }

    @Operation(summary = "订单物流跟踪", description = "获取订单物流跟踪信息")
    @GetMapping("/{id}/track")
    public ApiResponse<Void> trackOrder(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        mallOrderService.trackOrder(id);
        return ApiResponse.success("查询成功", null);
    }
}
