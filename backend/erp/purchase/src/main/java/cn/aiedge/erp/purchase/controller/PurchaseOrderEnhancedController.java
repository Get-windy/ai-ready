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

@Slf4j
@Tag(name = "采购订单增强管理", description = "采购订单完整生命周期API接口")
@RestController
@RequestMapping("/api/erp/purchase")
@RequiredArgsConstructor
public class PurchaseOrderEnhancedController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "获取待审批订单列表")
    @GetMapping("/pending-approval")
    @SaCheckPermission("purchase:order:list")
    public ApiResponse<List<?>> getPendingApprovalOrders() {
        List<?> orders = purchaseOrderService.getPendingApprovalOrders();
        return ApiResponse.ok(orders);
    }

    @Operation(summary = "供应商确认订单")
    @PostMapping("/orders/{id}/confirm")
    @SaCheckPermission("purchase:order:confirm")
    public ApiResponse<String> confirmBySupplier(@PathVariable Long id) {
        purchaseOrderService.confirmBySupplier(id);
        return ApiResponse.ok("供应商确认成功");
    }

    @Operation(summary = "发货通知")
    @PostMapping("/orders/{id}/ship")
    @SaCheckPermission("purchase:order:ship")
    public ApiResponse<String> shipOrder(@PathVariable Long id,
                                       @RequestParam String trackingNumber,
                                       @RequestParam LocalDateTime estimatedArrivalTime) {
        purchaseOrderService.shipOrder(id, trackingNumber, estimatedArrivalTime);
        return ApiResponse.ok("发货通知成功");
    }

    @Operation(summary = "收货确认")
    @PostMapping("/orders/{id}/receive")
    @SaCheckPermission("purchase:order:receive")
    public ApiResponse<String> receiveOrder(@PathVariable Long id,
                                          @RequestParam BigDecimal receivedQuantity,
                                          @RequestParam String qualityCheckResult) {
        purchaseOrderService.receiveOrder(id, receivedQuantity, qualityCheckResult);
        return ApiResponse.ok("收货确认成功");
    }

    @Operation(summary = "发票提交")
    @PostMapping("/orders/{id}/invoice")
    @SaCheckPermission("purchase:order:invoice")
    public ApiResponse<String> submitInvoice(@PathVariable Long id,
                                           @RequestParam String invoiceNumber,
                                           @RequestParam BigDecimal invoiceAmount,
                                           @RequestParam LocalDate invoiceDate) {
        purchaseOrderService.submitInvoice(id, invoiceNumber, invoiceAmount, invoiceDate);
        return ApiResponse.ok("发票提交成功");
    }

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

    @Operation(summary = "采购数据导出")
    @GetMapping("/export")
    @SaCheckPermission("purchase:order:export")
    public void exportPurchaseOrders(HttpServletResponse response,
                                     @RequestParam Long tenantId,
                                     @RequestParam(required = false) LocalDate startDate,
                                     @RequestParam(required = false) LocalDate endDate) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=purchase_orders.xlsx");
        
        try {
            List<Map<String, Object>> report = purchaseOrderService.generatePurchaseReport(tenantId, startDate, endDate);
            log.info("导出采购订单数据: tenantId={}, 记录数={}", tenantId, report.size());
        } catch (Exception e) {
            log.error("导出采购订单数据失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }
}