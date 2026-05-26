package cn.aiedge.order.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.order.dto.OrderDTO;
import cn.aiedge.order.entity.Order;
import cn.aiedge.order.service.IOrderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单管理控制器
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<OrderDTO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer orderType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long saleId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        Page<OrderDTO> result = orderService.pageOrders(page, tenantId, orderNo, customerName,
                orderType, status, saleId, startDate, endDate);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<OrderDTO> getDetail(@PathVariable Long id) {
        OrderDTO dto = orderService.getOrderDetail(id);
        return ApiResponse.ok(dto);
    }

    @Operation(summary = "创建订单")
    @PostMapping
    @SaCheckPermission("order:create")
    public ApiResponse<Long> create(@RequestBody OrderDTO dto) {
        Long id = orderService.createOrder(dto);
        return ApiResponse.ok("创建成功", id);
    }

    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    @SaCheckPermission("order:update")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody OrderDTO dto) {
        dto.setId(id);
        orderService.updateOrder(dto);
        return ApiResponse.ok("更新成功", null);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("order:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ApiResponse.ok("删除成功", null);
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("order:update")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        orderService.updateStatus(id, status);
        return ApiResponse.ok("更新成功", null);
    }

    @Operation(summary = "审核订单")
    @PutMapping("/{id}/audit")
    @SaCheckPermission("order:audit")
    public ApiResponse<Void> audit(@PathVariable Long id,
                                    @RequestParam Integer status,
                                    @RequestParam(required = false) String auditRemark) {
        orderService.auditOrder(id, status, auditRemark);
        return ApiResponse.ok("审核成功", null);
    }

    @Operation(summary = "订单收款")
    @PutMapping("/{id}/receive")
    @SaCheckPermission("order:receive")
    public ApiResponse<Void> receivePayment(@PathVariable Long id, @RequestParam BigDecimal amount) {
        orderService.addReceivedAmount(id, amount);
        return ApiResponse.ok("收款成功", null);
    }
}
