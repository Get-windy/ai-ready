package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.PurchaseOrderDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderStatisticsDTO;
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
import org.springframework.format.annotation.DateTimeFormat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @SaCheckPermission("purchase:order:list")
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
     * 分页查询采购订单（「按单据」39 列视图）。
     *
     * <p><b>为什么必须放在 {@code /{id}} 之前</b>：本类同时有 {@code @GetMapping("/{id}")}，
     * 缺了本方法时 {@code GET /api/erp/purchase/order/page} 会落到路径变量上，
     * 把字符串 "page" 当 Long 解析失败 → 400「参数[id]格式不正确」。
     * 这是入库/退货表单「选源单」与订单中心「采购」tab 一直打不开的根因。</p>
     *
     * <p>入参同时兼容两套前端调用：入库/退货表单传 {@code orderNo/supplierName}，
     * 订单中心传 {@code keyword/startDate/endDate}；租户一律由会话决定，不收 tenantId。</p>
     */
    @Operation(summary = "分页查询采购订单")
    @GetMapping("/page")
    @SaCheckPermission("purchase:order:list")
    public ApiResponse<Page<PurchaseOrderListDTO>> pageOrders(
            @Parameter(description = "页码，从1起") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer size,
            @Parameter(description = "单据状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "单据编号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "供应商名称") @RequestParam(required = false) String supplierName,
            @Parameter(description = "通用关键字(编号或供应商)") @RequestParam(required = false) String keyword,
            @Parameter(description = "单据日期起 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "单据日期止 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(purchaseOrderService.pageOrders(
                current, size, status, orderNo, supplierName, keyword, startDate, endDate));
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
    @SaCheckPermission("purchase:order:list")
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

    /**
     * 采购订单统计（区间内的单数、总额，以及按状态 / 采购类型的分布）。
     *
     * <p>2026-09-20 从旧模块 {@code cn.aiedge.erp.order.OrderController} 迁移而来
     * （原路径 {@code GET /api/erp/purchase-orders/statistics}），用于把「两套采购订单实现」收敛为一套。</p>
     *
     * <p><b>参数用 {@link LocalDate} 而非 LocalDateTime</b>：前端传的是 {@code yyyy-MM-dd}（ISO.DATE），
     * 而旧接口把参数声明成 {@code LocalDateTime} + {@code ISO.DATE}，Spring 无法把 "2026-09-01"
     * 解析成 LocalDateTime，导致该接口**一直返回 400**（采购分析页的统计因此从未成功过）。
     * 这里改成 LocalDate 接收、再转成时刻传给 service。</p>
     *
     * <p>区间含 endDate 当天，故结束时刻取次日零点 —— 与 {@code DashboardController} 里
     * 同一份统计的 KPI 口径保持一致。</p>
     *
     * <p><b>校验口径与旧接口一致</b>：旧接口只要求登录（{@code @SaCheckLogin}）而无细粒度权限码，
     * 这里刻意不收紧，避免采购分析页对既有角色突然 403。后续若要补权限，
     * 建议用已存在的 {@code purchase:order:list}（新权限码必须先登记权限种子，否则非超管全 403）。</p>
     */
    @Operation(summary = "采购订单统计")
    @SaCheckPermission("purchase:order:view")
    @GetMapping("/statistics")
    @SaCheckLogin
    public ApiResponse<PurchaseOrderStatisticsDTO> statistics(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PurchaseOrderStatisticsDTO statistics = purchaseOrderService.getPurchaseStatistics(
                tenantId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        return ApiResponse.ok(statistics);
    }
}
