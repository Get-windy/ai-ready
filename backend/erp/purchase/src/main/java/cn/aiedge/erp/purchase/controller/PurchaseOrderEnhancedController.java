package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购订单增强控制器 - 提供完整的采购执行API
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "采购订单增强管理", description = "采购订单完整生命周期API接口")
@RestController
@RequestMapping("/api/erp/purchase")
@RequiredArgsConstructor
public class PurchaseOrderEnhancedController {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * 获取待审批订单列表
     */
    @Operation(summary = "获取待审批订单列表")
    @GetMapping("/pending-approval")
    @SaCheckPermission("purchase:order:list")
    public ApiResponse<List<Object>> getPendingApprovalOrders() {
        List<?> orders = purchaseOrderService.getPendingApprovalOrders();
        return ApiResponse.ok(orders);
    }

    // ==================== 采购执行API ====================

    /**
     * 供应商确认
     */
    @Operation(summary = "供应商确认订单")
    @PostMapping("/orders/{id}/confirm")
    @SaCheckPermission("purchase:order:confirm")
    public ApiResponse<Void> confirmBySupplier(@PathVariable Long id) {
        purchaseOrderService.confirmBySupplier(id);
        return ApiResponse.ok("供应商确认成功");
    }

    /**
     * 发货通知
     */
    @Operation(summary = "发货通知")
    @PostMapping("/orders/{id}/ship")
    @SaCheckPermission("purchase:order:ship")
    public ApiResponse<Void> shipOrder(@PathVariable Long id,
                                       @RequestParam String trackingNumber,
                                       @RequestParam LocalDateTime estimatedArrivalTime) {
        purchaseOrderService.shipOrder(id, trackingNumber, estimatedArrivalTime);
        return ApiResponse.ok("发货通知成功");
    }

    /**
     * 收货确认
     */
    @Operation(summary = "收货确认")
    @PostMapping("/orders/{id}/receive")
    @SaCheckPermission("purchase:order:receive")
    public ApiResponse<Void> receiveOrder(@PathVariable Long id,
                                          @RequestParam BigDecimal receivedQuantity,
                                          @RequestParam String qualityCheckResult) {
        purchaseOrderService.receiveOrder(id, receivedQuantity, qualityCheckResult);
        return ApiResponse.ok("收货确认成功");
    }

    /**
     * 发票提交
     */
    @Operation(summary = "发票提交")
    @PostMapping("/orders/{id}/invoice")
    @SaCheckPermission("purchase:order:invoice")
    public ApiResponse<Void> submitInvoice(@PathVariable Long id,
                                           @RequestParam String invoiceNumber,
                                           @RequestParam BigDecimal invoiceAmount,
                                           @RequestParam LocalDate invoiceDate) {
        purchaseOrderService.submitInvoice(id, invoiceNumber, invoiceAmount, invoiceDate);
        return ApiResponse.ok("发票提交成功");
    }

    // ==================== 采购统计API ====================

    /**
     * 采购统计信息
     */
    @Operation(summary = "获取采购统计信息")
    @GetMapping("/statistics")
    @SaCheckPermission("purchase:order:statistics")
    public ApiResponse<Map<String, Object>> getPurchaseStatistics(
            @RequestParam Long tenantId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Map<String, Object> statistics = purchaseOrderService.getPurchaseStatistics(tenantId, startDate, endDate);
        return ApiResponse.ok(statistics);
    }

    /**
     * 采购报表生成
     */
    @Operation(summary = "生成采购报表")
    @GetMapping("/report")
    @SaCheckPermission("purchase:order:report")
    public ApiResponse<List<Map<String, Object>>> generatePurchaseReport(
            @RequestParam Long tenantId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<Map<String, Object>> report = purchaseOrderService.generatePurchaseReport(tenantId, startDate, endDate);
        return ApiResponse.ok(report);
    }

    /**
     * 采购数据导出
     */
    @Operation(summary = "采购数据导出")
    @GetMapping("/export")
    @SaCheckPermission("purchase:order:export")
    public void exportPurchaseOrders(HttpServletResponse response,
                                     @RequestParam Long tenantId,
                                     @RequestParam(required = false) LocalDate startDate,
                                     @RequestParam(required = false) LocalDate endDate) {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=purchase_orders.xlsx");
        
        // 生成Excel数据
        try {
            List<Map<String, Object>> report = purchaseOrderService.generatePurchaseReport(tenantId, startDate, endDate);
            // TODO: 实现Excel导出逻辑
            log.info("导出采购订单数据: tenantId={}, 记录数={}", tenantId, report.size());
        } catch (Exception e) {
            log.error("导出采购订单数据失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }
}