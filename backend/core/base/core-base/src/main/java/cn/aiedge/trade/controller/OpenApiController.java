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
 * 4. 安全验证 - **签名校验（2026-09-27 补齐，此前是空头支票）**
 *
 * <p><b>⚠️ 鉴权口径（2026-09-27 审计修复）</b>：</p>
 * <ul>
 *   <li>本控制器**自身不加**鉴权注解：外部平台无 Sa-Token 会话，须由
 *       {@link cn.aiedge.trade.open.OpenApiAuthInterceptor} 统一做 HMAC-SHA256 验签
 *       （请求头 {@code X-Api-Key / X-Timestamp / X-Sign}，密钥取
 *       {@code external_channel_config.app_secret}）。</li>
 *   <li>该拦截器生效的前提是 `/api/open/**` 已进 {@code SaTokenConfig} 的两份白名单。</li>
 *   <li>修复前的问题：既未放行白名单（**外部平台必然 401、功能不可用**），又无任何鉴权注解
 *       （**任何已登录用户可无权限码调用**，含写订单、查任意 SKU 库存）；
 *       且类注释宣称"签名校验"而 {@code signature} 参数**收下后从未使用**。</li>
 *   <li>{@code signature} 查询参数已由请求头 {@code X-Sign} 取代，保留仅为兼容，**不再参与校验**。</li>
 * </ul>
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
        // ⚠️ 2026-09-27 审计修复：原为 TODO 桩 —— 只打日志后 `return Result.success()`，
        //    即**假装成功**（接入方收到 200 却什么都没发生）。现改为如实失败。
        //    真实推送需要「已接入并启用的外部平台 + 其回调地址」，当前 external_channel_config
        //    无启用记录 ⇒ 无对象可推。配置渠道后应改为委托渠道适配器按 channelCode 推送
        //    （复用 /api/open/inventory/sync 那套 pushToChannel 能力）。
        log.warn("订单状态推送被调用但未实现: internal={}, channel={}", internalOrderId, channelCode);
        return Result.fail(501, "订单状态推送尚未实现：当前无已接入并启用的外部平台");
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
        // ⚠️ 2026-09-27 审计修复：原为 TODO 桩 —— 只做可用量比较就返回，**并未真正锁定库存**，
        //    调用方会误以为已锁成功。库存锁定是写操作，且涉及「ERP 轨 / WMS 轨」双写口径
        //    （见 STORAGE_MODULE_AUDIT_20260923 §7.2），必须走 InventoryService 唯一写入口，
        //    不能在本控制器里自行实现 ⇒ 如实失败，待与仓储模块对齐后接入。
        log.warn("库存锁定被调用但未实现: sku={}, qty={}, lockId={}", skuCode, quantity, lockId);
        return Result.fail(501, "库存锁定尚未实现：需接入库存服务（唯一写入口），当前不提供");
    }

    @Operation(summary = "库存释放", description = "释放锁定的库存")
    @PostMapping("/inventory/release")
    public Result<Void> releaseInventory(
            @RequestParam String lockId) {
        // ⚠️ 2026-09-27 审计修复：原为 TODO 桩 —— 直接 `return Result.success()`，即假装释放成功。
        log.warn("库存释放被调用但未实现: lockId={}", lockId);
        return Result.fail(501, "库存释放尚未实现：需接入库存服务（唯一写入口），当前不提供");
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