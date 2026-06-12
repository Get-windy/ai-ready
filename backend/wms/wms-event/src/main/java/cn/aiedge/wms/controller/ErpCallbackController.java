package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.event.service.ErpCallbackService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ERP 回调控制器 - WMS 向 ERP 推送事件时 ERP 端接收端点
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/wms")
@RequiredArgsConstructor
@Tag(name = "ERP 回调", description = "ERP 接收来自 WMS 的事件回调")
public class ErpCallbackController {

    private final ErpCallbackService erpCallbackService;

    @PostMapping("/receipt-complete")
    @Operation(summary = "ERP 接收入库完成通知")
    public Result<String> receiptComplete(@RequestBody JsonNode payload) {
        return erpCallbackService.handleReceiptComplete(payload);
    }

    @PostMapping("/ship-complete")
    @Operation(summary = "ERP 接收出库完成通知")
    public Result<String> shipComplete(@RequestBody JsonNode payload) {
        return erpCallbackService.handleShipComplete(payload);
    }

    @PostMapping("/inventory-change")
    @Operation(summary = "ERP 接收库存变动通知")
    public Result<String> inventoryChange(@RequestBody JsonNode payload) {
        return erpCallbackService.handleInventoryChange(payload);
    }

    @PostMapping("/check-diff")
    @Operation(summary = "ERP 接收盘点差异通知")
    public Result<String> checkDiff(@RequestBody JsonNode payload) {
        return erpCallbackService.handleCheckDiff(payload);
    }
}
