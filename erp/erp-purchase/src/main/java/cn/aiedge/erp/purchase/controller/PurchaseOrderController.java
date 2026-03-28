package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购订单控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购订单管理", description = "采购订单CRUD接口")
@RestController
@RequestMapping("/api/erp/purchase/order")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * 创建采购订单
     */
    @Operation(summary = "创建采购订单")
    @PostMapping
    @SaCheckPermission("purchase:order:create")
    public ApiResponse<Long> createOrder(@RequestBody @Valid PurchaseOrder order) {
        Long orderId = purchaseOrderService.createOrder(order);
        return ApiResponse.ok("创建成功", orderId);
    }

    /**
     * 更新采购订单
     */
    @Operation(summary = "更新采购订单")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:order:update")
    public ApiResponse<Void> updateOrder(@PathVariable Long id, @RequestBody PurchaseOrder order) {
        order.setId(id);
        purchaseOrderService.updateOrder(order);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 删除采购订单
     */
    @Operation(summary = "删除采购订单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("purchase:order:delete")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id) {
        purchaseOrderService.deleteOrder(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 提交审批
     */
    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @SaCheckPermission("purchase:order:submit")
    public ApiResponse<Void> submitForApproval(@PathVariable Long id) {
        purchaseOrderService.submitForApproval(id);
        return ApiResponse.ok("提交成功", null);
    }

    /**
     * 审批通过
     */
    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("purchase:order:approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        purchaseOrderService.approve(id);
        return ApiResponse.ok("审批通过", null);
    }

    /**
     * 审批拒绝
     */
    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @SaCheckPermission("purchase:order:approve")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        purchaseOrderService.reject(id, reason);
        return ApiResponse.ok("已拒绝", null);
    }

    /**
     * 取消订单
     */
    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("purchase:order:cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, @RequestParam String reason) {
        purchaseOrderService.cancel(id, reason);
        return ApiResponse.ok("已取消", null);
    }

    /**
     * 分页查询
     */
    @Operation(summary = "分页查询采购订单")
    @GetMapping("/page")
    @SaCheckPermission("purchase:order:list")
    public ApiResponse<PageResult<PurchaseOrder>> pageOrders(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Integer status) {
        Page<PurchaseOrder> page = new Page<>(current, size);
        Page<PurchaseOrder> result = purchaseOrderService.pageOrders(page, tenantId, orderNo, supplierId, status);
        return ApiResponse.ok(PageResult.of(result));
    }

    /**
     * 获取订单详情
     */
    @Operation(summary = "获取采购订单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("purchase:order:detail")
    public ApiResponse<PurchaseOrder> getOrderDetail(@PathVariable Long id) {
        PurchaseOrder order = purchaseOrderService.getOrderDetail(id);
        return ApiResponse.ok(order);
    }
}