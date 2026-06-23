package cn.aiedge.erp.sale.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import cn.aiedge.erp.sale.service.ISaleOrderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 销售订单控制器
 */
@Tag(name = "销售订单管理")
@RestController
@RequestMapping("/api/erp/sale/order")
@RequiredArgsConstructor
public class SaleOrderController {

    private final ISaleOrderService saleOrderService;

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<SaleOrderDTO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<SaleOrder> page = new Page<>(pageNum, pageSize);
        Page<SaleOrderDTO> result = saleOrderService.pageOrders(page, tenantId, orderNo, customerId, status, startDate, endDate);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "获取订单统计")
    @GetMapping("/stats")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getStats(@RequestParam(required = false) Long tenantId) {
        Map<String, Object> stats = saleOrderService.getOrderStats(tenantId);
        return ApiResponse.ok(stats);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id:\\d+}")
    @SaCheckLogin
    public ApiResponse<SaleOrderDTO> getDetail(@PathVariable Long id) {
        SaleOrderDTO dto = saleOrderService.getOrderDetail(id);
        return ApiResponse.ok(dto);
    }

    @Operation(summary = "创建订单")
    @PostMapping
    @SaCheckPermission("sale:order:create")
    @OperationLog(module = "销售订单管理", type = "CREATE", desc = "创建订单")
    public ApiResponse<Long> create(@RequestBody SaleOrderDTO dto) {
        Long id = saleOrderService.createOrder(dto);
        return ApiResponse.ok("创建成功", id);
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
    public ApiResponse<List<SaleOrderDTO>> getPending(@RequestParam Long tenantId) {
        List<SaleOrderDTO> list = saleOrderService.getPendingOrders(tenantId);
        return ApiResponse.ok(list);
    }

    @Operation(summary = "批量删除销售订单")
    @DeleteMapping("/batch")
    @SaCheckPermission("sale:order:delete")
    @OperationLog(module = "销售订单管理", type = "DELETE", desc = "批量删除订单")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            saleOrderService.deleteOrder(id);
        }
        return ApiResponse.ok("批量删除成功", null);
    }

    @Operation(summary = "导出销售订单")
    @GetMapping("/export")
    @SaCheckPermission("sale:order:list")
    @OperationLog(module = "销售订单管理", type = "EXPORT", desc = "导出销售订单")
    public ApiResponse<List<SaleOrder>> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Integer status) {
        List<SaleOrder> list = saleOrderService.exportList(keyword, customerId, status);
        return ApiResponse.ok(list);
    }
}