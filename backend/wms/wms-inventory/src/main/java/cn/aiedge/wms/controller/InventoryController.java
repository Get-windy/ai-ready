package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.entity.WmsInventoryLog;
import cn.aiedge.wms.inventory.service.InventoryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@Validated
@Tag(name = "库存管理")
@RestController
@RequestMapping("/api/wms/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "查询库存（唯一键）")
    @GetMapping("/query")
    public Result<WmsInventory> query(@RequestParam @NotNull Long productId,
                                      @RequestParam @NotNull Long warehouseId,
                                      @RequestParam(required = false) Long locationId,
                                      @RequestParam(required = false) String batchNo) {
        return Result.ok(inventoryService.getByUniqueKey(productId, warehouseId, locationId, batchNo));
    }

    @Operation(summary = "分页查询库存列表")
    @GetMapping("/page")
    public Result<Page<WmsInventory>> page(@Valid Page<WmsInventory> page, WmsInventory query) {
        return Result.ok(inventoryService.pageInventory(page, query));
    }

    @Operation(summary = "分页查询库存流水")
    @GetMapping("/log/page")
    public Result<Page<WmsInventoryLog>> logPage(@Valid Page<WmsInventoryLog> page, WmsInventoryLog query) {
        return Result.ok(inventoryService.pageLog(page, query));
    }

    @Operation(summary = "增加库存")
    @PostMapping("/increase")
    public Result<String> increase(@Valid @RequestBody StockChangeRequest request) {
        inventoryService.increase(
                request.productId(), request.warehouseId(), request.locationId(),
                request.batchNo(), request.quantity(), request.traceId(),
                request.sourceType(), request.sourceId(), request.sourceNo(),
                request.operatorId(), request.operatorName());
        log.info("库存增加: productId={}, warehouseId={}, qty={}, traceId={}",
                request.productId(), request.warehouseId(), request.quantity(), request.traceId());
        return Result.ok("增加库存成功");
    }

    @Operation(summary = "减少库存")
    @PostMapping("/decrease")
    public Result<String> decrease(@Valid @RequestBody StockChangeRequest request) {
        inventoryService.decrease(
                request.productId(), request.warehouseId(), request.locationId(),
                request.batchNo(), request.quantity(), request.traceId(),
                request.sourceType(), request.sourceId(), request.sourceNo(),
                request.operatorId(), request.operatorName());
        log.info("库存减少: productId={}, warehouseId={}, qty={}, traceId={}",
                request.productId(), request.warehouseId(), request.quantity(), request.traceId());
        return Result.ok("减少库存成功");
    }

    @Operation(summary = "冻结库存")
    @PostMapping("/freeze")
    public Result<String> freeze(@Valid @RequestBody FreezeRequest request) {
        inventoryService.freeze(
                request.productId(), request.warehouseId(), request.locationId(),
                request.batchNo(), request.quantity(), request.traceId(),
                request.sourceType(), request.sourceId(), request.operatorId(),
                request.operatorName());
        log.info("库存冻结: productId={}, warehouseId={}, qty={}, traceId={}",
                request.productId(), request.warehouseId(), request.quantity(), request.traceId());
        return Result.ok("冻结库存成功");
    }

    @Operation(summary = "解冻库存")
    @PostMapping("/unfreeze")
    public Result<String> unfreeze(@Valid @RequestBody FreezeRequest request) {
        inventoryService.unfreeze(
                request.productId(), request.warehouseId(), request.locationId(),
                request.batchNo(), request.quantity(), request.traceId(),
                request.sourceType(), request.sourceId(), request.operatorId(),
                request.operatorName());
        log.info("库存解冻: productId={}, warehouseId={}, qty={}, traceId={}",
                request.productId(), request.warehouseId(), request.quantity(), request.traceId());
        return Result.ok("解冻库存成功");
    }

    @Operation(summary = "移库（库存转移）")
    @PostMapping("/move")
    public Result<String> move(@Valid @RequestBody MoveRequest request) {
        inventoryService.move(
                request.productId(), request.warehouseId(),
                request.fromLocationId(), request.toLocationId(),
                request.batchNo(), request.quantity(), request.traceId(),
                request.sourceType(), request.sourceId(), request.operatorId(),
                request.operatorName());
        log.info("库存移库: productId={}, from={}, to={}, qty={}, traceId={}",
                request.productId(), request.fromLocationId(), request.toLocationId(),
                request.quantity(), request.traceId());
        return Result.ok("移库成功");
    }

    // ==================== DTO ====================

    @Schema(description = "库存变动请求")
    public record StockChangeRequest(
            @Schema(description = "商品ID") @NotNull Long productId,
            @Schema(description = "仓库ID") @NotNull Long warehouseId,
            @Schema(description = "货位ID") Long locationId,
            @Schema(description = "批次号") String batchNo,
            @Schema(description = "数量") @NotNull @Positive BigDecimal quantity,
            @Schema(description = "追踪ID") String traceId,
            @Schema(description = "来源类型") String sourceType,
            @Schema(description = "来源单据ID") Long sourceId,
            @Schema(description = "来源单据号") String sourceNo,
            @Schema(description = "操作人ID") Long operatorId,
            @Schema(description = "操作人姓名") String operatorName) {}

    @Schema(description = "冻结/解冻请求")
    public record FreezeRequest(
            @Schema(description = "商品ID") @NotNull Long productId,
            @Schema(description = "仓库ID") @NotNull Long warehouseId,
            @Schema(description = "货位ID") Long locationId,
            @Schema(description = "批次号") String batchNo,
            @Schema(description = "数量") @NotNull @Positive BigDecimal quantity,
            @Schema(description = "追踪ID") String traceId,
            @Schema(description = "来源类型") String sourceType,
            @Schema(description = "来源单据ID") Long sourceId,
            @Schema(description = "操作人ID") Long operatorId,
            @Schema(description = "操作人姓名") String operatorName) {}

    @Schema(description = "移库请求")
    public record MoveRequest(
            @Schema(description = "商品ID") @NotNull Long productId,
            @Schema(description = "仓库ID") @NotNull Long warehouseId,
            @Schema(description = "来源货位ID") @NotNull Long fromLocationId,
            @Schema(description = "目标货位ID") @NotNull Long toLocationId,
            @Schema(description = "批次号") String batchNo,
            @Schema(description = "数量") @NotNull @Positive BigDecimal quantity,
            @Schema(description = "追踪ID") String traceId,
            @Schema(description = "来源类型") String sourceType,
            @Schema(description = "来源单据ID") Long sourceId,
            @Schema(description = "操作人ID") Long operatorId,
            @Schema(description = "操作人姓名") String operatorName) {}
}
