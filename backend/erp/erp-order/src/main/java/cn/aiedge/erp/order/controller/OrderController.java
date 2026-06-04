package cn.aiedge.erp.order.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderStatisticsDTO;
import cn.aiedge.erp.order.entity.PurchaseOrder;
import cn.aiedge.erp.order.service.IPurchaseOrderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单管理控制器
 * API路径符合项目规范：/api/erp/purchase-orders
 */
@Tag(name = "采购订单管理")
@RestController
@RequestMapping("/api/erp/purchase-orders")
@RequiredArgsConstructor
public class OrderController {

    private final IPurchaseOrderService purchaseOrderService;

    @Operation(summary = "分页查询采购订单")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<PurchaseOrder>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) Integer purchaseType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Page<PurchaseOrder> page = new Page<>(pageNum, pageSize);
        Page<PurchaseOrder> result = purchaseOrderService.pagePurchaseOrders(page, tenantId, orderNo, supplierName,
                purchaseType, status, buyerId, startDate, endDate);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "获取采购订单详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<PurchaseOrder> getDetail(@PathVariable Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderService.getPurchaseOrderDetail(id);
        return ApiResponse.ok(purchaseOrder);
    }

    @Operation(summary = "创建采购订单")
    @PostMapping
    @SaCheckPermission("purchase_order:create")
    public ApiResponse<Long> createPurchaseOrder(@RequestBody PurchaseOrderCreateDTO dto) {
        Long id = purchaseOrderService.createPurchaseOrder(dto);
        return ApiResponse.ok("采购订单创建成功", id);
    }

    @Operation(summary = "更新采购订单")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase_order:update")
    public ApiResponse<Void> updatePurchaseOrder(@PathVariable Long id, @RequestBody PurchaseOrderCreateDTO dto) {
        dto.setId(id);
        purchaseOrderService.updatePurchaseOrder(dto);
        return ApiResponse.ok("采购订单更新成功", null);
    }

    @Operation(summary = "删除采购订单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("purchase_order:delete")
    public ApiResponse<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ApiResponse.ok("采购订单删除成功", null);
    }

    @Operation(summary = "提交采购订单审批")
    @PostMapping("/{id}/submit")
    @SaCheckPermission("purchase_order:submit")
    public ApiResponse<Void> submitForApproval(@PathVariable Long id,
                                               @RequestParam(required = false) String remark) {
        purchaseOrderService.submitForApproval(id, remark);
        return ApiResponse.ok("采购订单提交审批成功", null);
    }

    @Operation(summary = "审批采购订单")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("purchase_order:approve")
    public ApiResponse<Void> approvePurchaseOrder(@PathVariable Long id,
                                                  @RequestBody PurchaseOrderApproveDTO approveDTO) {
        purchaseOrderService.approvePurchaseOrder(id, approveDTO);
        return ApiResponse.ok("采购订单审批完成", null);
    }

    @Operation(summary = "拒绝采购订单")
    @PostMapping("/{id}/reject")
    @SaCheckPermission("purchase_order:approve")
    public ApiResponse<Void> rejectPurchaseOrder(@PathVariable Long id,
                                                 @RequestParam String rejectReason) {
        purchaseOrderService.rejectPurchaseOrder(id, rejectReason);
        return ApiResponse.ok("采购订单已拒绝", null);
    }

    @Operation(summary = "获取待审批采购订单列表")
    @GetMapping("/pending-approval")
    @SaCheckPermission("purchase_order:approve")
    public ApiResponse<List<PurchaseOrder>> getPendingApprovalOrders(@RequestParam Long tenantId,
                                                                     @RequestParam(required = false) Long approverId) {
        List<PurchaseOrder> orders = purchaseOrderService.getPendingApprovalOrders(tenantId, approverId);
        return ApiResponse.ok(orders);
    }

    @Operation(summary = "供应商确认采购订单")
    @PostMapping("/{id}/confirm")
    @SaCheckPermission("purchase_order:execute")
    public ApiResponse<Void> confirmBySupplier(@PathVariable Long id,
                                               @RequestParam String confirmationNo,
                                               @RequestParam(required = false) String remark) {
        purchaseOrderService.confirmBySupplier(id, confirmationNo, remark);
        return ApiResponse.ok("供应商确认成功", null);
    }

    @Operation(summary = "发货通知")
    @PostMapping("/{id}/ship")
    @SaCheckPermission("purchase_order:execute")
    public ApiResponse<Void> shipOrder(@PathVariable Long id,
                                       @RequestParam String trackingNo,
                                       @RequestParam String logisticsCompany,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime estimatedArrivalDate) {
        purchaseOrderService.shipOrder(id, trackingNo, logisticsCompany, estimatedArrivalDate);
        return ApiResponse.ok("发货通知成功", null);
    }

    @Operation(summary = "收货确认")
    @PostMapping("/{id}/receive")
    @SaCheckPermission("purchase_order:execute")
    public ApiResponse<Void> receiveGoods(@PathVariable Long id,
                                          @RequestParam Integer receivedQuantity,
                                          @RequestParam(required = false) String qualityCheckResult,
                                          @RequestParam(required = false) String remark) {
        purchaseOrderService.receiveGoods(id, receivedQuantity, qualityCheckResult, remark);
        return ApiResponse.ok("收货确认成功", null);
    }

    @Operation(summary = "发票提交")
    @PostMapping("/{id}/invoice")
    @SaCheckPermission("purchase_order:execute")
    public ApiResponse<Void> submitInvoice(@PathVariable Long id,
                                           @RequestParam String invoiceNo,
                                           @RequestParam BigDecimal invoiceAmount,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime invoiceDate) {
        purchaseOrderService.submitInvoice(id, invoiceNo, invoiceAmount, invoiceDate);
        return ApiResponse.ok("发票提交成功", null);
    }

    @Operation(summary = "获取采购统计信息")
    @GetMapping("/statistics")
    @SaCheckLogin
    public ApiResponse<PurchaseOrderStatisticsDTO> getPurchaseStatistics(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate) {
        PurchaseOrderStatisticsDTO statistics = purchaseOrderService.getPurchaseStatistics(tenantId, startDate, endDate);
        return ApiResponse.ok(statistics);
    }

    @Operation(summary = "获取采购报表")
    @GetMapping("/report")
    @SaCheckLogin
    public ApiResponse<byte[]> generatePurchaseReport(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        byte[] report = purchaseOrderService.generatePurchaseReport(tenantId, startDate, endDate, format);
        return ApiResponse.ok(report);
    }

    @Operation(summary = "导出采购数据")
    @GetMapping("/export")
    @SaCheckPermission("purchase_order:export")
    public ApiResponse<byte[]> exportPurchaseData(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestParam(defaultValue = "excel") String format) {
        byte[] exportData = purchaseOrderService.exportPurchaseData(tenantId, startDate, endDate, format);
        return ApiResponse.ok(exportData);
    }

    @Operation(summary = "更新采购订单状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("purchase_order:update")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        purchaseOrderService.updatePurchaseOrderStatus(id, status);
        return ApiResponse.ok("采购订单状态更新成功", null);
    }

    @Operation(summary = "批量删除采购订单")
    @DeleteMapping("/batch")
    @SaCheckPermission("purchase_order:delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            purchaseOrderService.deletePurchaseOrder(id);
        }
        return ApiResponse.ok("批量删除成功");
    }
}