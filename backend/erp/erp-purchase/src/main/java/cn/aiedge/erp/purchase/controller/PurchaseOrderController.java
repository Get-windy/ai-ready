package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 采购订单控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购订单管理", description = "采购订单CRUD+审批+查询接口")
@RestController
@RequestMapping("/api/erp/purchase/order")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;

    /**
     * WMS→ERP：收货确认后回写采购订单明细已收数量（累加增量）。
     * 裁决：WMS 不直接依赖 erp-purchase，只 HTTP 调用本接口。
     */
    @Operation(summary = "收货回写：累加采购订单明细已收数量")
    @PostMapping("/received")
    public ApiResponse<Boolean> receiveBackfill(@RequestBody Map<String, Object> body) {
        Object poIdObj = body.get("purchaseOrderId");
        Object itemsObj = body.get("items");
        if (!(poIdObj instanceof Number) || !(itemsObj instanceof List)) {
            return ApiResponse.ok("purchaseOrderId 与 items 不能为空", false);
        }
        Long purchaseOrderId = ((Number) poIdObj).longValue();
        List<?> items = (List<?>) itemsObj;
        for (Object itemObj : items) {
            if (!(itemObj instanceof Map)) continue;
            Map<?, ?> it = (Map<?, ?>) itemObj;
            Object pid = it.get("productId");
            Object qv = it.get("receivedQuantity");
            if (!(pid instanceof Number) || qv == null) continue;
            Long productId = ((Number) pid).longValue();
            BigDecimal qty = new BigDecimal(String.valueOf(qv));
            LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PurchaseOrderItem::getOrderId, purchaseOrderId)
                    .eq(PurchaseOrderItem::getProductId, productId);
            List<PurchaseOrderItem> matched = purchaseOrderItemMapper.selectList(wrapper);
            for (PurchaseOrderItem poItem : matched) {
                purchaseOrderItemMapper.addReceivedQuantity(poItem.getId(), qty);
            }
        }
        return ApiResponse.ok("收货回写成功", true);
    }

    /**
     * 创建采购订单（含子表）
     */
    @Operation(summary = "创建采购订单")
    @PostMapping
    @SaCheckPermission("purchase:order:create")
    public ApiResponse<Long> createOrder(@RequestBody PurchaseOrderDTO dto) {
        Long orderId = purchaseOrderService.createOrder(dto);
        return ApiResponse.ok("创建成功", orderId);
    }

    /**
     * 更新采购订单（含子表）
     */
    @Operation(summary = "更新采购订单")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:order:update")
    public ApiResponse<Void> updateOrder(@PathVariable Long id, @RequestBody PurchaseOrderDTO dto) {
        purchaseOrderService.updateOrder(id, dto);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 获取采购订单详情（含所有子表）
     */
    @Operation(summary = "获取采购订单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("purchase:order:detail")
    public ApiResponse<PurchaseOrderDTO> getOrderDetail(@PathVariable Long id) {
        PurchaseOrderDTO dto = purchaseOrderService.getOrderDetail(id);
        return ApiResponse.ok(dto);
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
     * 批量审批通过
     */
    @Operation(summary = "批量审批通过")
    @PostMapping("/batch-approve")
    @SaCheckPermission("purchase:order:approve")
    public ApiResponse<Void> batchApprove(@RequestBody List<Long> ids) {
        purchaseOrderService.batchApprove(ids);
        return ApiResponse.ok("批量审批通过", null);
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
     * 生成下一单据号
     */
    @Operation(summary = "生成下一单据号")
    @GetMapping("/next-no")
    @SaCheckLogin
    public ApiResponse<String> getNextOrderNo(
            @Parameter(description = "日期(yyyy-MM-dd 或 yyyyMMdd)") @RequestParam(required = false) String date) {
        // 容错：前端可能传 yyyyMMdd（generateCodeAsync 的 getTodayStr 返回 8 位），也可能传标准 ISO 日期
        LocalDate localDate;
        if (date == null || date.isBlank()) {
            localDate = LocalDate.now();
        } else if (date.matches("\\d{8}")) {
            localDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        } else {
            localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        String orderNo = purchaseOrderService.generateNextOrderNo(localDate);
        return ApiResponse.ok(orderNo);
    }

    /**
     * 获取订单明细列表
     */
    @Operation(summary = "获取采购订单明细")
    @GetMapping("/{id}/items")
    @SaCheckPermission("purchase:order:detail")
    public ApiResponse<List<PurchaseOrderItem>> getOrderItems(@PathVariable Long id) {
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectByOrderId(id);
        return ApiResponse.ok(items);
    }

    /**
     * 导出采购订单
     */
    @Operation(summary = "导出采购订单")
    @GetMapping("/export")
    @SaCheckPermission("purchase:order:list")
    public ApiResponse<List<PurchaseOrder>> exportOrders(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        List<PurchaseOrder> list = purchaseOrderService.exportOrders(tenantId, orderNo, supplierId, status);
        return ApiResponse.ok(list);
    }

    /**
     * 批量导入采购订单
     */
    @Operation(summary = "批量导入采购订单")
    @PostMapping("/import")
    @SaCheckPermission("purchase:order:create")
    public ApiResponse<Map<String, Integer>> importOrders(@RequestParam("file") MultipartFile file) {
        int count = purchaseOrderService.importOrders(file);
        return ApiResponse.ok(Map.of("count", count));
    }

    /**
     * 批量打印
     */
    @Operation(summary = "批量打印采购订单")
    @PostMapping("/batch-print")
    @SaCheckPermission("purchase:order:list")
    public ApiResponse<Void> batchPrint(@RequestBody Map<String, Object> params) {
        // 雪花 ID 超出 JS 安全整数范围，前端一律以字符串透传；Number / String 两种入参都需兼容
        List<?> rawIds = (List<?>) params.get("ids");
        List<Long> ids = rawIds == null ? List.of()
                : rawIds.stream().map(v -> Long.parseLong(String.valueOf(v))).toList();
        String template = params.get("template") != null ? params.get("template").toString() : "default";
        purchaseOrderService.batchPrint(ids, template);
        return ApiResponse.ok("打印完成", null);
    }
}
