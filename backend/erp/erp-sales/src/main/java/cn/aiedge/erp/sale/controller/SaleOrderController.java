package cn.aiedge.erp.sale.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.dto.SaleOrderDetailDTO;
import cn.aiedge.erp.sale.dto.SaleOrderListDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售订单控制器
 * 列表接口返回 SaleOrderListDTO (~20字段)
 * 详情接口返回 SaleOrderDetailDTO (含子表)
 */
@Tag(name = "销售订单管理")
@RestController
@RequestMapping("/api/erp/sale/order")
@RequiredArgsConstructor
public class SaleOrderController {

    private final ISaleOrderService saleOrderService;

    // ═══════════════════════════════════════════
    // 基础 CRUD
    // ═══════════════════════════════════════════

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<SaleOrderListDTO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        Page<SaleOrderListDTO> result = saleOrderService.pageOrders(page, tenantId, orderNo, customerId, status, startDate, endDate);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "获取订单统计")
    @GetMapping("/stats")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getStats(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.ok(saleOrderService.getOrderStats(tenantId));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id:\\d+}")
    @SaCheckLogin
    public ApiResponse<SaleOrderDetailDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(saleOrderService.getOrderDetail(id));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    @SaCheckPermission("sale:order:create")
    @OperationLog(module = "销售订单管理", type = "CREATE", desc = "创建订单")
    public ApiResponse<Long> create(@RequestBody SaleOrderDTO dto) {
        return ApiResponse.ok("创建成功", saleOrderService.createOrder(dto));
    }

    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    @SaCheckPermission("sale:order:update")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "更新订单")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SaleOrderDTO dto) {
        dto.setId(id);
        saleOrderService.updateOrder(dto);
        return ApiResponse.ok("更新成功", null);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sale:order:delete")
    @OperationLog(module = "销售订单管理", type = "DELETE", desc = "删除订单")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        saleOrderService.deleteOrder(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ═══════════════════════════════════════════
    // 审批/状态流转
    // ═══════════════════════════════════════════

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @SaCheckPermission("sale:order:submit")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "提交审批")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        saleOrderService.submitForApproval(id);
        return ApiResponse.ok("提交成功", null);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("sale:order:approve")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "审批通过")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        saleOrderService.approve(id, StpUtil.getLoginIdAsLong());
        return ApiResponse.ok("审批通过", null);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @SaCheckPermission("sale:order:approve")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "审批拒绝")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        saleOrderService.reject(id, StpUtil.getLoginIdAsLong(), reason);
        return ApiResponse.ok("已拒绝", null);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("sale:order:cancel")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "取消订单")
    public ApiResponse<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        saleOrderService.cancelOrder(id, reason);
        return ApiResponse.ok("已取消", null);
    }

    @Operation(summary = "确认出库")
    @PostMapping("/{id}/ship")
    @SaCheckPermission("sale:order:ship")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "确认出库")
    public ApiResponse<Void> ship(@PathVariable Long id, @RequestParam Long warehouseId) {
        saleOrderService.confirmShipment(id, warehouseId);
        return ApiResponse.ok("出库成功", null);
    }

    @Operation(summary = "记录收款")
    @PostMapping("/{id}/payment")
    @SaCheckPermission("sale:order:payment")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "记录收款")
    public ApiResponse<Void> payment(@PathVariable Long id, @RequestParam BigDecimal amount) {
        saleOrderService.recordPayment(id, amount);
        return ApiResponse.ok("收款成功", null);
    }

    @Operation(summary = "待审批订单列表")
    @GetMapping("/pending")
    @SaCheckLogin
    public ApiResponse<List<SaleOrderListDTO>> getPending(@RequestParam Long tenantId) {
        return ApiResponse.ok(saleOrderService.getPendingOrders(tenantId));
    }

    // ═══════════════════════════════════════════
    // 批量操作
    // ═══════════════════════════════════════════

    @Operation(summary = "批量删除")
    @DeleteMapping("/batch")
    @SaCheckPermission("sale:order:delete")
    @OperationLog(module = "销售订单管理", type = "DELETE", desc = "批量删除订单")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) saleOrderService.deleteOrder(id);
        return ApiResponse.ok("批量删除成功", null);
    }

    @Operation(summary = "批量审批通过")
    @PostMapping("/batch-approve")
    @SaCheckPermission("sale:order:approve")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "批量审批")
    public ApiResponse<Void> batchApprove(@RequestBody List<Long> ids) {
        Long auditorId = StpUtil.getLoginIdAsLong();
        for (Long id : ids) saleOrderService.approve(id, auditorId);
        return ApiResponse.ok("批量审批成功", null);
    }

    // ═══════════════════════════════════════════
    // 导出
    // ═══════════════════════════════════════════

    @Operation(summary = "导出销售订单")
    @GetMapping("/export")
    @SaCheckPermission("sale:order:list")
    @OperationLog(module = "销售订单管理", type = "EXPORT", desc = "导出销售订单")
    public ApiResponse<List<SaleOrderListDTO>> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(saleOrderService.exportList(keyword, customerId, status));
    }

    @Operation(summary = "批量导入销售订单")
    @PostMapping("/import")
    @SaCheckPermission("sale:order:create")
    @OperationLog(module = "销售订单管理", type = "IMPORT", desc = "批量导入销售订单")
    public ApiResponse<String> batchImport(@RequestParam("file") MultipartFile file) {
        try {
            // 委托给 service 处理导入逻辑
            saleOrderService.batchImport(file);
            return ApiResponse.ok("导入成功");
        } catch (Exception e) {
            return ApiResponse.fail("导入失败: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // 订单处理中心 - 多tab API
    // ═══════════════════════════════════════════

    @Operation(summary = "订单处理中心统计卡片")
    @GetMapping("/center/stats")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> orderCenterStats(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> filters = buildFilters(startDate, endDate, status, null, null, null);
        return ApiResponse.ok(saleOrderService.orderCenterStats(tenantId, filters));
    }

    @Operation(summary = "订单处理中心-按单据分页查询")
    @GetMapping("/center/page-by-doc")
    @SaCheckLogin
    public ApiResponse<Page<SaleOrderListDTO>> orderCenterPageByDoc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String deliveryRoute,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long salesmanId,
            @RequestParam(required = false) String salesmanName,
            @RequestParam(required = false) String supplementType,
            @RequestParam(required = false) String generationMethod,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String settlementMethod,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String promoterName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String orderRemark,
            @RequestParam(required = false) String productBrand,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String auditorName,
            @RequestParam(required = false) String submitterName,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String logisticsCompany,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) Integer saleType,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String driverName,
            @RequestParam(required = false) String deliveryVehicle,
            @RequestParam(required = false) String buyerRemark,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String shipDateStart,
            @RequestParam(required = false) String shipDateEnd,
            @RequestParam(required = false) BigDecimal billAmountMin,
            @RequestParam(required = false) BigDecimal billAmountMax,
            @RequestParam(required = false) String extText1,
            @RequestParam(required = false) String extText2,
            @RequestParam(required = false) String extText3,
            @RequestParam(required = false) String footerExtText1,
            @RequestParam(required = false) String footerExtText2,
            @RequestParam(required = false) String source) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        Map<String, Object> filters = buildFilters(startDate, endDate, status, orderNo, customerId, salesmanId);
        if (supplementType != null) filters.put("supplementType", supplementType);
        if (generationMethod != null) filters.put("generationMethod", generationMethod);
        if (settlementMethod != null) filters.put("settlementMethod", settlementMethod);
        if (deliveryRoute != null) filters.put("deliveryRoute", deliveryRoute);
        if (customerName != null) filters.put("customerName", customerName);
        if (salesmanName != null) filters.put("salesmanName", salesmanName);
        if (warehouseName != null) filters.put("warehouseName", warehouseName);
        if (deptName != null) filters.put("deptName", deptName);
        if (promoterName != null) filters.put("promoterName", promoterName);
        if (region != null) filters.put("region", region);
        if (productName != null) filters.put("productName", productName);
        if (orderRemark != null) filters.put("orderRemark", orderRemark);
        if (productBrand != null) filters.put("productBrand", productBrand);
        if (industryCategory != null) filters.put("industryCategory", industryCategory);
        if (creatorName != null) filters.put("creatorName", creatorName);
        if (auditorName != null) filters.put("auditorName", auditorName);
        if (submitterName != null) filters.put("submitterName", submitterName);
        if (receiverName != null) filters.put("receiverName", receiverName);
        if (receiverPhone != null) filters.put("receiverPhone", receiverPhone);
        if (logisticsCompany != null) filters.put("logisticsCompany", logisticsCompany);
        if (waybillNo != null) filters.put("waybillNo", waybillNo);
        if (saleType != null) filters.put("saleType", saleType);
        if (deliveryMethod != null) filters.put("deliveryMethod", deliveryMethod);
        if (driverName != null) filters.put("driverName", driverName);
        if (deliveryVehicle != null) filters.put("deliveryVehicle", deliveryVehicle);
        if (buyerRemark != null) filters.put("buyerRemark", buyerRemark);
        if (summary != null) filters.put("summary", summary);
        if (shipDateStart != null) filters.put("shipDateStart", shipDateStart);
        if (shipDateEnd != null) filters.put("shipDateEnd", shipDateEnd);
        if (billAmountMin != null) filters.put("billAmountMin", billAmountMin);
        if (billAmountMax != null) filters.put("billAmountMax", billAmountMax);
        if (extText1 != null) filters.put("extText1", extText1);
        if (extText2 != null) filters.put("extText2", extText2);
        if (extText3 != null) filters.put("extText3", extText3);
        if (footerExtText1 != null) filters.put("footerExtText1", footerExtText1);
        if (footerExtText2 != null) filters.put("footerExtText2", footerExtText2);
        if (source != null) filters.put("sourceOrder", source);
        return ApiResponse.ok(saleOrderService.orderCenterPageByDoc(page, tenantId, filters));
    }

    @Operation(summary = "订单处理中心-按时间分组统计")
    @GetMapping("/center/group-by-date")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> orderCenterGroupByDate(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long salesmanId,
            @RequestParam(required = false) String salesmanName,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String promoterName,
            @RequestParam(required = false) String region) {
        Map<String, Object> filters = buildFilters(startDate, endDate, null, null, customerId, salesmanId);
        if (warehouseId != null) filters.put("warehouseId", warehouseId);
        if (deptId != null) filters.put("deptId", deptId);
        if (customerName != null) filters.put("customerName", customerName);
        if (salesmanName != null) filters.put("salesmanName", salesmanName);
        if (warehouseName != null) filters.put("warehouseName", warehouseName);
        if (deptName != null) filters.put("deptName", deptName);
        if (promoterName != null) filters.put("promoterName", promoterName);
        if (region != null) filters.put("region", region);
        return ApiResponse.ok(saleOrderService.orderCenterGroupByDate(tenantId, filters));
    }

    @Operation(summary = "订单处理中心-按线路分组统计")
    @GetMapping("/center/group-by-route")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> orderCenterGroupByRoute(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salesmanId) {
        Map<String, Object> filters = buildFilters(startDate, endDate, null, null, customerId, salesmanId);
        return ApiResponse.ok(saleOrderService.orderCenterGroupByRoute(tenantId, filters));
    }

    @Operation(summary = "订单处理中心-按客户分组统计")
    @GetMapping("/center/group-by-customer")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> orderCenterGroupByCustomer(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salesmanId) {
        Page<?> page = new Page<>(1, 1000);
        Map<String, Object> filters = buildFilters(startDate, endDate, null, null, customerId, salesmanId);
        return ApiResponse.ok(saleOrderService.orderCenterGroupByCustomer(page, tenantId, filters));
    }

    @Operation(summary = "订单处理中心-订单履约分页查询")
    @GetMapping("/center/fulfillment-page")
    @SaCheckLogin
    public ApiResponse<Page<SaleOrderListDTO>> orderCenterFulfillmentPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salesmanId,
            @RequestParam(required = false) Integer status) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        Map<String, Object> filters = buildFilters(startDate, endDate, status, orderNo, customerId, salesmanId);
        return ApiResponse.ok(saleOrderService.orderCenterFulfillmentPage(page, tenantId, filters));
    }

    @Operation(summary = "订单处理中心-订单履约统计概览")
    @GetMapping("/center/fulfillment-overview")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> orderCenterFulfillmentOverview(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> filters = buildFilters(startDate, endDate, null, null, null, null);
        return ApiResponse.ok(saleOrderService.orderCenterFulfillmentOverview(tenantId, filters));
    }

    @Operation(summary = "待审核列表")
    @GetMapping("/center/pending-review")
    @SaCheckLogin
    public ApiResponse<Page<SaleOrderListDTO>> pendingReviewPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String salesmanName,
            @RequestParam(required = false) Long salesmanId,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String supplementType,
            @RequestParam(required = false) String productBrand,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(required = false) String deliveryRoute,
            @RequestParam(required = false) String generationMethod,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String auditorName,
            @RequestParam(required = false) String submitterName,
            @RequestParam(required = false) String settlementMethod,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String orderRemark,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer saleType,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String driverName,
            @RequestParam(required = false) String deliveryVehicle,
            @RequestParam(required = false) String logisticsCompany,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String buyerRemark,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String shipDateStart,
            @RequestParam(required = false) String shipDateEnd,
            @RequestParam(required = false) String source) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        Map<String, Object> filters = buildFilters(startDate, endDate, null, orderNo, customerId, salesmanId);
        if (customerName != null) filters.put("customerName", customerName);
        if (salesmanName != null) filters.put("salesmanName", salesmanName);
        if (warehouseName != null) filters.put("warehouseName", warehouseName);
        if (deptName != null) filters.put("deptName", deptName);
        if (supplementType != null) filters.put("supplementType", supplementType);
        if (productBrand != null) filters.put("productBrand", productBrand);
        if (industryCategory != null) filters.put("industryCategory", industryCategory);
        if (deliveryRoute != null) filters.put("deliveryRoute", deliveryRoute);
        if (generationMethod != null) filters.put("generationMethod", generationMethod);
        if (creatorName != null) filters.put("creatorName", creatorName);
        if (auditorName != null) filters.put("auditorName", auditorName);
        if (submitterName != null) filters.put("submitterName", submitterName);
        if (settlementMethod != null) filters.put("settlementMethod", settlementMethod);
        if (receiverName != null) filters.put("receiverName", receiverName);
        if (receiverPhone != null) filters.put("receiverPhone", receiverPhone);
        if (orderRemark != null) filters.put("orderRemark", orderRemark);
        if (productName != null) filters.put("productName", productName);
        if (region != null) filters.put("region", region);
        if (saleType != null) filters.put("saleType", saleType);
        if (deliveryMethod != null) filters.put("deliveryMethod", deliveryMethod);
        if (driverName != null) filters.put("driverName", driverName);
        if (deliveryVehicle != null) filters.put("deliveryVehicle", deliveryVehicle);
        if (logisticsCompany != null) filters.put("logisticsCompany", logisticsCompany);
        if (waybillNo != null) filters.put("waybillNo", waybillNo);
        if (buyerRemark != null) filters.put("buyerRemark", buyerRemark);
        if (summary != null) filters.put("summary", summary);
        if (shipDateStart != null) filters.put("shipDateStart", shipDateStart);
        if (shipDateEnd != null) filters.put("shipDateEnd", shipDateEnd);
        if (source != null) filters.put("sourceOrder", source);
        return ApiResponse.ok(saleOrderService.pendingReviewPage(page, tenantId, filters));
    }

    @Operation(summary = "拣货/发货列表")
    @GetMapping("/center/picking-shipping")
    @SaCheckLogin
    public ApiResponse<Page<SaleOrderListDTO>> pickingShippingPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String salesmanName,
            @RequestParam(required = false) Long salesmanId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String logisticsCompany,
            @RequestParam(required = false) String driverName,
            @RequestParam(required = false) String salespersonName) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        Map<String, Object> filters = buildFilters(startDate, endDate, null, orderNo, customerId, salesmanId);
        if (customerName != null) filters.put("customerName", customerName);
        if (salesmanName != null) filters.put("salesmanName", salesmanName);
        if (warehouseId != null) filters.put("warehouseId", warehouseId);
        if (warehouseName != null) filters.put("warehouseName", warehouseName);
        if (deliveryMethod != null) filters.put("deliveryMethod", deliveryMethod);
        if (logisticsCompany != null) filters.put("logisticsCompany", logisticsCompany);
        if (driverName != null) filters.put("driverName", driverName);
        if (salespersonName != null) filters.put("salesmanName", salespersonName);
        return ApiResponse.ok(saleOrderService.pickingShippingPage(page, tenantId, filters));
    }

    // ═══ 辅助方法 ═══

    private Map<String, Object> buildFilters(String startDate, String endDate, Integer status,
                                              String orderNo, Long customerId, Long salesmanId) {
        Map<String, Object> filters = new HashMap<>();
        if (startDate != null) filters.put("startDate", startDate);
        if (endDate != null) filters.put("endDate", endDate);
        if (status != null) filters.put("status", status);
        if (orderNo != null) filters.put("orderNo", orderNo);
        if (customerId != null) filters.put("customerId", customerId);
        if (salesmanId != null) filters.put("salesmanId", salesmanId);
        return filters;
    }

    // ═══ 打印 ═══

    @Operation(summary = "批量增加打印次数")
    @PostMapping("/batch-print")
    @SaCheckPermission("sale:order:print")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "批量打印")
    public ApiResponse<Void> batchPrint(@RequestBody List<Long> ids) {
        saleOrderService.incrementPrintCount(ids);
        return ApiResponse.ok("打印成功", null);
    }

    // ═══ 商品汇总 ═══

    @Operation(summary = "商品汇总查询")
    @GetMapping("/product-summary")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> productSummary(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long salesmanId) {
        Map<String, Object> filters = buildFilters(startDate, endDate, null, null, customerId, salesmanId);
        if (customerName != null) filters.put("customerName", customerName);
        return ApiResponse.ok(saleOrderService.productSummary(tenantId, filters));
    }

    // ═══ 物流备注批量更新 ═══

    @Operation(summary = "批量更新物流备注")
    @PostMapping("/batch-logistics-remark")
    @SaCheckPermission("sale:order:update")
    @OperationLog(module = "销售订单管理", type = "UPDATE", desc = "批量更新物流备注")
    public ApiResponse<Void> batchLogisticsRemark(@RequestBody BatchLogisticsRemarkRequest request) {
        saleOrderService.batchUpdateLogisticsRemark(request.getIds(), request.getRemark());
        return ApiResponse.ok("更新成功", null);
    }

    // ═══ 信用额度 ═══

    @Operation(summary = "获取客户信用信息")
    @GetMapping("/customer-credit/{customerId}")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getCustomerCredit(@PathVariable Long customerId) {
        return ApiResponse.ok(saleOrderService.getCustomerCreditInfo(customerId));
    }

    @Operation(summary = "获取客户预收款/订金余额")
    @GetMapping("/customer-deposits/{customerId}")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> getCustomerDeposits(@PathVariable Long customerId) {
        return ApiResponse.ok(saleOrderService.getCustomerDepositBalance(customerId));
    }

    /**
     * 批量更新物流备注请求体
     */
    @Data
    public static class BatchLogisticsRemarkRequest {
        private List<Long> ids;
        private String remark;
    }
}
