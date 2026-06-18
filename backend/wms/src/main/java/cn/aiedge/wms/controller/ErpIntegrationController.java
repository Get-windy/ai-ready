package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
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
 * ERP 集成控制器 - WMS 接收来自 ERP 的下游指令
 */
@Slf4j
@RestController
@RequestMapping("/api/wms/erp")
@RequiredArgsConstructor
@Tag(name = "ERP 集成", description = "WMS 接收来自 ERP 的业务指令")
public class ErpIntegrationController {

    @PostMapping("/purchase-order")
    @Operation(summary = "WMS 接收采购订单", description = "ERP 下发采购订单，WMS 创建入库收货任务")
    public Result<String> receivePurchaseOrder(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 采购订单: {}", payload);
        // TODO: 创建收货任务
        return Result.ok("已收到采购订单，收货任务待创建");
    }

    @PostMapping("/sale-order")
    @Operation(summary = "WMS 接收销售订单", description = "ERP 下发销售订单，WMS 创建出库拣货任务")
    public Result<String> receiveSaleOrder(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 销售订单: {}", payload);
        // TODO: 创建拣货任务
        return Result.ok("已收到销售订单，拣货任务待创建");
    }

    @PostMapping("/product-sync")
    @Operation(summary = "WMS 接收商品同步", description = "ERP 同步商品基础信息到 WMS")
    public Result<String> syncProduct(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 商品同步: {}", payload);
        // TODO: 同步商品信息
        return Result.ok("商品信息已接收");
    }

    @PostMapping("/check-command")
    @Operation(summary = "WMS 接收盘点指令", description = "ERP 下发盘点指令，WMS 创建盘点任务")
    public Result<String> receiveCheckCommand(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 盘点指令: {}", payload);
        // TODO: 创建盘点任务
        return Result.ok("已收到盘点指令，盘点任务待创建");
    }
}
