package cn.aiedge.erp.sale.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.entity.FreightRule;
import cn.aiedge.erp.sale.entity.SaleOrderLogistics;
import cn.aiedge.erp.sale.entity.ShipmentNotify;
import cn.aiedge.erp.sale.service.SaleLogisticsService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售物流域控制器：包裹/运单 · 运费规则与对账 · 电子面单取号 · 发货通知（ASN）
 *
 * <p>落地《物流发货-业界做法调研.md》P0/P1/P2；路由前缀 {@code /api/erp/sale/logistics}。
 * 物流备注（弹窗批量）仍在 {@link SaleOrderController#batchLogisticsRemark}。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "销售物流（包裹/运费/取号/发货通知）")
@RestController
@RequestMapping("/api/erp/sale/logistics")
@RequiredArgsConstructor
@SaCheckLogin
public class SaleLogisticsController {

    private final SaleLogisticsService logisticsService;

    // ═══ P0/P1 包裹（一单多包）═══

    @Operation(summary = "订单包裹列表（一条 = 一个包裹）")
    @GetMapping("/{orderId}/packages")
    public ApiResponse<List<SaleOrderLogistics>> packages(@PathVariable Long orderId) {
        return ApiResponse.ok(logisticsService.listPackages(orderId));
    }

    @Operation(summary = "新增/修改包裹（自动补包裹号、校验运单号唯一、按规则试算运费、同步主表快照）")
    @PostMapping("/{orderId}/packages")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "保存包裹")
    public ApiResponse<SaleOrderLogistics> savePackage(@PathVariable Long orderId,
                                                       @RequestBody SaleOrderLogistics pkg) {
        return ApiResponse.ok("保存成功", logisticsService.savePackage(orderId, pkg));
    }

    @Operation(summary = "删除包裹（最后一个不允许删除）")
    @DeleteMapping("/{orderId}/packages/{packageId}")
    @OperationLog(module = "销售物流", type = "DELETE", desc = "删除包裹")
    public ApiResponse<Void> deletePackage(@PathVariable Long orderId, @PathVariable Long packageId) {
        logisticsService.deletePackage(orderId, packageId);
        return ApiResponse.ok("删除成功", null);
    }

    // ═══ P2-5 电子面单取号 ═══

    @Operation(summary = "电子面单 / 发货通知回调 是否已配置（未配置则前端提示手工录入 / 仅落台账）")
    @GetMapping("/waybill/status")
    public ApiResponse<Map<String, Object>> waybillStatus() {
        return ApiResponse.ok(Map.of(
                "enabled", logisticsService.waybillEnabled(),
                "asnConfigured", logisticsService.asnConfigured()));
    }

    @Operation(summary = "获取电子面单运单号（未配置承运商接口则明确报错，不返回假号）")
    @PostMapping("/packages/{packageId}/acquire-waybill")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "电子面单取号")
    public ApiResponse<SaleOrderLogistics> acquireWaybill(@PathVariable Long packageId) {
        return ApiResponse.ok("取号成功", logisticsService.acquireWaybill(packageId));
    }

    // ═══ P2-4 运费规则 / 试算 / 对账 ═══

    @Operation(summary = "运费规则列表")
    @GetMapping("/freight/rules")
    public ApiResponse<List<FreightRule>> rules(@RequestParam(required = false) Long carrierId) {
        return ApiResponse.ok(logisticsService.listRules(carrierId));
    }

    @Operation(summary = "新增/修改运费规则")
    @PostMapping("/freight/rules")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "保存运费规则")
    public ApiResponse<FreightRule> saveRule(@RequestBody FreightRule rule) {
        return ApiResponse.ok("保存成功", logisticsService.saveRule(rule));
    }

    @Operation(summary = "删除运费规则")
    @DeleteMapping("/freight/rules/{id}")
    @OperationLog(module = "销售物流", type = "DELETE", desc = "删除运费规则")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        logisticsService.deleteRule(id);
        return ApiResponse.ok("删除成功", null);
    }

    @Operation(summary = "运费试算（承运商 × 区域 × 重量 → 首重/续重）")
    @GetMapping("/freight/calc")
    public ApiResponse<Map<String, Object>> calc(@RequestParam(required = false) Long carrierId,
                                                 @RequestParam(required = false) String area,
                                                 @RequestParam BigDecimal weight) {
        return ApiResponse.ok(logisticsService.calcFreight(carrierId, area, weight));
    }

    @Operation(summary = "运费对账（按承运商/期间：我方计费 vs 承运商账单，出差异清单）")
    @GetMapping("/freight/reconcile")
    public ApiResponse<Map<String, Object>> reconcile(
            @RequestParam(required = false) Long carrierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(logisticsService.reconcile(carrierId, startDate, endDate));
    }

    @Operation(summary = "录入承运商账单金额（自动算差异）")
    @PostMapping("/freight/bill")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "录入运费账单金额")
    public ApiResponse<SaleOrderLogistics> saveBill(@RequestParam Long packageId,
                                                    @RequestParam BigDecimal billAmount) {
        return ApiResponse.ok("已录入", logisticsService.saveBillAmount(packageId, billAmount));
    }

    @Operation(summary = "标记已对账")
    @PostMapping("/freight/reconcile-mark")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "标记运费已对账")
    public ApiResponse<Integer> markReconciled(@RequestBody List<Long> packageIds) {
        return ApiResponse.ok("已标记", logisticsService.markReconciled(packageIds));
    }

    // ═══ P2-6 发货通知（ASN）═══

    @Operation(summary = "发货通知台账分页")
    @GetMapping("/shipment-notify/page")
    public ApiResponse<Page<ShipmentNotify>> notifyPage(@RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) String orderNo,
                                                        @RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(logisticsService.notifyPage(status, orderNo, current, size));
    }

    @Operation(summary = "发送单条发货通知（可重试）")
    @PostMapping("/shipment-notify/{id}/send")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "发送发货通知")
    public ApiResponse<ShipmentNotify> sendNotify(@PathVariable Long id) {
        return ApiResponse.ok("已处理", logisticsService.sendNotify(id));
    }

    @Operation(summary = "批量发送待发送/失败的发货通知")
    @PostMapping("/shipment-notify/send-pending")
    @OperationLog(module = "销售物流", type = "UPDATE", desc = "批量发送发货通知")
    public ApiResponse<Map<String, Object>> sendPending() {
        return ApiResponse.ok(logisticsService.sendPending());
    }
}
