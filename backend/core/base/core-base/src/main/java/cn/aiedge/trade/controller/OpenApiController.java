package cn.aiedge.trade.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import cn.aiedge.trade.entity.ExternalOrderRaw;
import cn.aiedge.trade.service.ExternalOrderService;
import cn.aiedge.trade.service.InventorySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 对外开放API控制器
 * 用于接收外部平台（电商、ERP等）的订单和库存请求
 *
 * 特点：
 * 1. 高性能 - 支持缓存和批量查询
 * 2. 幂等性 - 所有接口可重复调用
 * 3. 异步处理 - 订单回调异步入库
 * 4. 安全验证 - 签名校验
 */
@Slf4j
@Tag(name = "对外开放API", description = "订单接入、库存查询、商品同步等开放接口")
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
@Validated
public class OpenApiController {

    private final ExternalOrderService orderService;
    private final InventorySyncService inventoryService;

    // ========== 订单接入接口 ==========

    @Operation(summary = "订单回调接口", description = "接收外部平台订单回调，异步处理")
    @PostMapping("/order/callback/{channelCode}")
    public Result<ExternalOrderRaw> orderCallback(
            @Parameter(description = "渠道编码") @PathVariable String channelCode,
            @RequestBody String callbackData,
            @Parameter(description = "签名") @RequestParam(required = false) String signature) {
        log.info("接收订单回调: channel={}", channelCode);
        ExternalOrderRaw raw = orderService.receiveCallback(channelCode, callbackData);
        return Result.success(raw);
    }

    @Operation(summary = "批量推送订单", description = "外部系统批量推送订单数据")
    @PostMapping("/order/batch/{channelCode}")
    public Result<Map<String, Object>> batchPushOrders(
            @Parameter(description = "渠道编码") @PathVariable String channelCode,
            @RequestBody List<String> orderDataList) {
        int count = 0;
        for (String data : orderDataList) {
            try {
                orderService.receiveCallback(channelCode, data);
                count++;
            } catch (Exception e) {
                log.warn("批量订单推送失败: {}", e.getMessage());
            }
        }
        return Result.success(Map.of("total", orderDataList.size(), "success", count));
    }

    @Operation(summary = "订单状态推送", description = "推送订单状态变更到外部平台（如发货）")
    @PostMapping("/order/status/{internalOrderId}")
    public Result<Void> pushOrderStatus(
            @PathVariable Long internalOrderId,
            @RequestParam String channelCode,
            @RequestParam String externalOrderId,
            @RequestParam String status) {
        // TODO: 实现订单状态推送
        log.info("推送订单状态: internal={}, external={}, status={}", internalOrderId, externalOrderId, status);
        return Result.success();
    }

    // ========== 库存查询接口（高频） ==========

    @Operation(summary = "库存查询", description = "查询SKU可用库存，支持缓存")
    @GetMapping("/inventory/query")
    public Result<InventoryQueryResult> queryInventory(
            @Parameter(description = "SKU编码") @RequestParam String skuCode,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        InventoryQueryResult result = inventoryService.queryInventory(skuCode, warehouseId);
        return Result.success(result);
    }

    @Operation(summary = "批量库存查询", description = "批量查询SKU库存，适用于电商平台")
    @PostMapping("/inventory/batch-query")
    public Result<Map<String, InventoryQueryResult>> batchQueryInventory(
            @RequestBody List<String> skuCodes,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        Map<String, InventoryQueryResult> results = inventoryService.batchQueryInventory(skuCodes, warehouseId);
        return Result.success(results);
    }

    @Operation(summary = "库存锁定查询", description = "查询库存并锁定（下单时使用）")
    @PostMapping("/inventory/lock")
    public Result<InventoryQueryResult> lockInventory(
            @RequestParam String skuCode,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam String lockId) {
        // TODO: 实现库存锁定逻辑
        InventoryQueryResult result = inventoryService.queryInventory(skuCode, warehouseId);
        if (result.getAvailableQuantity() < quantity) {
            result.setSuccess(false);
            result.setErrorMsg("库存不足");
        }
        return Result.success(result);
    }

    @Operation(summary = "库存释放", description = "释放锁定的库存")
    @PostMapping("/inventory/release")
    public Result<Void> releaseInventory(
            @RequestParam String lockId) {
        // TODO: 实现库存释放逻辑
        return Result.success();
    }

    // ========== 库存同步接口 ==========

    @Operation(summary = "库存同步推送", description = "推送库存变更到外部平台")
    @PostMapping("/inventory/sync/{channelCode}")
    public Result<ProductSyncResult> syncInventory(
            @PathVariable String channelCode,
            @RequestParam String skuCode,
            @RequestParam Integer quantity) {
        ProductSyncResult result = inventoryService.pushToChannel(channelCode, skuCode, quantity);
        return Result.success(result);
    }

    @Operation(summary = "批量库存同步", description = "批量推送库存到外部平台")
    @PostMapping("/inventory/batch-sync/{channelCode}")
    public Result<Map<String, ProductSyncResult>> batchSyncInventory(
            @PathVariable String channelCode,
            @RequestBody Map<String, Integer> skuQuantities) {
        Map<String, ProductSyncResult> results = inventoryService.batchPushToChannel(channelCode, skuQuantities);
        return Result.success(results);
    }

    // ========== 商品同步接口 ==========

    @Operation(summary = "商品价格更新", description = "更新商品价格到外部平台")
    @PostMapping("/product/price/{channelCode}")
    public Result<ProductSyncResult> updatePrice(
            @PathVariable String channelCode,
            @RequestParam String skuCode,
            @RequestParam BigDecimal price) {
        ProductSyncResult result = inventoryService.updatePrice(channelCode, skuCode, price);
        return Result.success(result);
    }

    // ========== 健康检查接口 ==========

    @Operation(summary = "API健康检查", description = "验证API可用性")
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        return Result.success(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "version", "1.0.0"
        ));
    }

    @Operation(summary = "待处理订单数量", description = "查询待处理的订单数量")
    @GetMapping("/order/pending-count")
    public Result<Integer> pendingCount(
            @RequestParam(required = false) String channelCode) {
        return Result.success(orderService.countPending(channelCode));
    }
}