package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.check.service.CheckService;
import cn.aiedge.wms.entity.WmsCheckTask;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.pick.service.PickService;
import cn.aiedge.wms.receipt.service.ReceiptService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 集成控制器 - WMS 接收来自 ERP 的下游指令
 */
@Slf4j
@RestController
@RequestMapping("/api/wms/erp")
@RequiredArgsConstructor
@Tag(name = "ERP 集成", description = "WMS 接收来自 ERP 的业务指令")
public class ErpIntegrationController {

    private final ReceiptService receiptService;
    private final PickService pickService;
    private final CheckService checkService;

    @PostMapping("/purchase-order")
    @Operation(summary = "WMS 接收采购订单", description = "ERP 下发采购订单，WMS 创建入库收货任务")
    public Result<String> receivePurchaseOrder(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 采购订单: {}", payload);

        Long purchaseOrderId = payload.path("id").asLong();
        String purchaseOrderNo = payload.path("orderNo").asText("");

        // 从 ERP 下发采购订单 payload 构建收货任务与明细（真实数据）
        WmsReceiptTask newTask = new WmsReceiptTask();
        newTask.setTaskNo("RC" + System.currentTimeMillis());
        newTask.setSourceType(0); // 采购入库（枚举与前端对齐：0-采购入库 1-生产入库 2-退货入库 3-调拨入库 4-其他）
        newTask.setSourceOrderId(purchaseOrderId);
        newTask.setSourceOrderNo(purchaseOrderNo);
        newTask.setWarehouseId(payload.has("warehouseId") ? payload.path("warehouseId").asLong() : null);
        newTask.setWarehouseName(payload.path("warehouseName").asText(""));
        newTask.setSupplierId(payload.has("supplierId") ? payload.path("supplierId").asLong() : null);
        newTask.setSupplierName(payload.path("supplierName").asText(""));
        newTask.setStatus(0); // 待收货
        newTask.setPriority(0); // 普通
        newTask.setTotalItems(payload.path("itemCount").asInt(0));
        newTask.setTotalQuantity(BigDecimal.valueOf(payload.path("totalQuantity").asDouble(0)));
        newTask.setReceivedQuantity(BigDecimal.ZERO);
        newTask.setExpectedTime(LocalDateTime.now().plusDays(3));
        newTask.setRemark("ERP采购订单自动创建");

        receiptService.saveTask(newTask);

        // 创建明细
        JsonNode items = payload.path("items");
        if (items.isArray()) {
            int lineNo = 1;
            for (JsonNode item : items) {
                WmsReceiptDetail detail = new WmsReceiptDetail();
                detail.setTaskId(newTask.getId());
                detail.setLineNo(lineNo++);
                detail.setProductId(item.path("productId").asLong());
                detail.setProductCode(item.path("productCode").asText(""));
                detail.setProductName(item.path("productName").asText(""));
                detail.setProductSpec(item.path("spec").asText(""));
                detail.setProductUnit(item.path("unit").asText(""));
                detail.setExpectedQuantity(BigDecimal.valueOf(item.path("quantity").asDouble(0)));
                detail.setReceivedQuantity(BigDecimal.ZERO);
                detail.setStatus(0); // 待收货
                receiptService.saveDetail(detail);
            }
        }

        log.info("收货任务已创建: taskNo={}, sourceOrderNo={}", newTask.getTaskNo(), purchaseOrderNo);
        return Result.ok("收货任务已创建，任务单号: " + newTask.getTaskNo());
    }

    @PostMapping("/sale-order")
    @Operation(summary = "WMS 接收销售订单", description = "ERP 下发销售订单，WMS 创建出库拣货任务")
    public Result<String> receiveSaleOrder(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 销售订单: {}", payload);

        Long saleOrderId = payload.path("id").asLong();

        // 使用拣货服务创建波次和拣货任务
        var wave = pickService.createWave(java.util.List.of(saleOrderId));
        if (wave != null) {
            log.info("拣货波次已创建: waveId={}, saleOrderId={}", wave.getId(), saleOrderId);
            return Result.ok("拣货任务已创建，波次单号: " + wave.getWaveNo());
        }

        return Result.ok("已收到销售订单，拣货任务待创建");
    }

    @PostMapping("/product-sync")
    @Operation(summary = "WMS 接收商品同步", description = "ERP 同步商品基础信息到 WMS")
    public Result<String> syncProduct(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 商品同步: productId={}, productName={}",
                payload.path("id").asLong(), payload.path("productName").asText(""));

        // 商品信息同步到WMS商品表（WMS使用ERP商品主数据，这里记录同步日志）
        // WMS不维护独立商品表，直接使用ERP商品数据
        // 此处仅确认接收，实际业务通过ERP直接查询
        return Result.ok("商品信息已接收，WMS将直接使用ERP商品数据");
    }

    @PostMapping("/check-command")
    @Operation(summary = "WMS 接收盘点指令", description = "ERP 下发盘点指令，WMS 创建盘点任务")
    public Result<String> receiveCheckCommand(@RequestBody JsonNode payload) {
        log.info("WMS 收到 ERP 盘点指令: {}", payload);

        Long warehouseId = payload.path("warehouseId").asLong();
        String warehouseName = payload.path("warehouseName").asText("");

        WmsCheckTask task = new WmsCheckTask();
        task.setTaskNo("CHK-" + System.currentTimeMillis());
        task.setWarehouseId(warehouseId);
        task.setWarehouseName(warehouseName);
        task.setStatus(0); // 待盘点
        task.setRemark("ERP盘点指令自动创建");

        checkService.saveTask(task);
        log.info("盘点任务已创建: taskNo={}", task.getTaskNo());
        return Result.ok("盘点任务已创建，任务单号: " + task.getTaskNo());
    }
}
