package cn.aiedge.erp.sale.controller;

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

/**
 * 销售订单控制器
 */
@Tag(name = "销售订单管理")
@RestController
@RequestMapping("/api/sale/order")
@RequiredArgsConstructor
public class SaleOrderController {

    private final ISaleOrderService saleOrderService;

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    @SaCheckLogin
    public Result<Page<SaleOrderDTO>> page(
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
        return Result.ok(result);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public Result<SaleOrderDTO> getDetail(@PathVariable Long id) {
        SaleOrderDTO dto = saleOrderService.getOrderDetail(id);
        return Result.ok(dto);
    }

    @Operation(summary = "创建订单")
    @PostMapping
    @SaCheckPermission("sale:order:create")
    public Result<Long> create(@RequestBody SaleOrderDTO dto) {
        Long id = saleOrderService.createOrder(dto);
        return Result.ok("创建成功", id);
    }

    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    @SaCheckPermission("sale:order:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody SaleOrderDTO dto) {
        dto.setId(id);
        saleOrderService.updateOrder(dto);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sale:order:delete")
    public Result<Void> delete(@PathVariable Long id) {
        saleOrderService.deleteOrder(id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @SaCheckPermission("sale:order:submit")
    public Result<Void> submit(@PathVariable Long id) {
        saleOrderService.submitForApproval(id);
        return Result.ok("提交成功", null);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("sale:order:approve")
    public Result<Void> approve(@PathVariable Long id) {
        saleOrderService.approve(id, StpUtil.getLoginIdAsLong());
        return Result.ok("审批通过", null);
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @SaCheckPermission("sale:order:approve")
    public Result<Void> reject(@PathVariable Long id, @RequestParam String reason) {
        saleOrderService.reject(id, StpUtil.getLoginIdAsLong(), reason);
        return Result.ok("已拒绝", null);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("sale:order:cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        saleOrderService.cancelOrder(id, reason);
        return Result.ok("已取消", null);
    }

    @Operation(summary = "确认出库")
    @PostMapping("/{id}/ship")
    @SaCheckPermission("sale:order:ship")
    public Result<Void> ship(@PathVariable Long id, @RequestParam Long warehouseId) {
        saleOrderService.confirmShipment(id, warehouseId);
        return Result.ok("出库成功", null);
    }

    @Operation(summary = "记录收款")
    @PostMapping("/{id}/payment")
    @SaCheckPermission("sale:order:payment")
    public Result<Void> payment(@PathVariable Long id, @RequestParam BigDecimal amount) {
        saleOrderService.recordPayment(id, amount);
        return Result.ok("收款成功", null);
    }

    @Operation(summary = "待审批订单列表")
    @GetMapping("/pending")
    @SaCheckLogin
    public Result<List<SaleOrderDTO>> getPending(@RequestParam Long tenantId) {
        List<SaleOrderDTO> list = saleOrderService.getPendingOrders(tenantId);
        return Result.ok(list);
    }

    /**
     * 统一响应结果
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;

        public static <T> Result<T> ok(T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = "success";
            result.data = data;
            return result;
        }

        public static <T> Result<T> ok(String message, T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = message;
            result.data = data;
            return result;
        }
    }
}