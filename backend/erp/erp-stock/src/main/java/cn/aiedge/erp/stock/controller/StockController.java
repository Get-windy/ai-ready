package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.erp.stock.service.WarehouseService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存管理Controller
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "库存管理", description = "库存查询、盘点、调拨等接口")
@RestController
@RequestMapping("/api/erp/stock")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StockController {

    private final StockService stockService;
    private final WarehouseService warehouseService;

    @Operation(summary = "查询仓库列表")
    @GetMapping("/warehouses")
    public Result<List<Warehouse>> getWarehouses() {
        return Result.ok(warehouseService.getWarehouseList());
    }

    @Operation(summary = "查询库存详情")
    @GetMapping("/{productId}/{warehouseId}")
    public Result<Stock> getStockDetail(
            @Parameter(description = "产品ID") @PathVariable Long productId,
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId) {
        Stock stock = stockService.getStockDetail(productId, warehouseId);
        if (stock == null) {
            return Result.fail("库存记录不存在");
        }
        return Result.ok(stock);
    }

    @Operation(summary = "冻结库存")
    @PostMapping("/freeze")
    public Result<Boolean> freezeStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "冻结数量") @RequestParam BigDecimal quantity) {
        boolean success = stockService.freezeStock(productId, warehouseId, quantity);
        if (success) {
            log.info("库存冻结成功: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity);
        }
        return Result.ok(success);
    }

    @Operation(summary = "解冻库存")
    @PostMapping("/unfreeze")
    public Result<Boolean> unfreezeStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "解冻数量") @RequestParam BigDecimal quantity) {
        boolean success = stockService.unfreezeStock(productId, warehouseId, quantity);
        if (success) {
            log.info("库存解冻成功: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity);
        }
        return Result.ok(success);
    }

    @Operation(summary = "库存增加")
    @PostMapping("/increase")
    public Result<Boolean> increaseStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "增加数量") @RequestParam BigDecimal quantity) {
        boolean success = stockService.increaseStock(productId, warehouseId, quantity);
        if (success) {
            log.info("库存增加成功: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity);
        }
        return Result.ok(success);
    }

    @Operation(summary = "库存减少")
    @PostMapping("/decrease")
    public Result<Boolean> decreaseStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "减少数量") @RequestParam BigDecimal quantity) {
        boolean success = stockService.decreaseStock(productId, warehouseId, quantity);
        if (success) {
            log.info("库存减少成功: productId={}, warehouseId={}, quantity={}", productId, warehouseId, quantity);
        }
        return Result.ok(success);
    }

    @Operation(summary = "分页查询库存")
    @GetMapping("/page")
    public Result<IPage<Stock>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        QueryWrapper<Stock> wrapper = new QueryWrapper<Stock>().eq("deleted", 0);
        if (productId != null) wrapper.eq("product_id", productId);
        if (warehouseId != null) wrapper.eq("warehouse_id", warehouseId);
        wrapper.orderByDesc("create_time");
        return Result.ok(stockService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询库存详情(by id)")
    @GetMapping("/{id}")
    public Result<Stock> getById(@Parameter(description = "库存ID") @PathVariable Long id) {
        Stock stock = stockService.getById(id);
        if (stock == null) {
            return Result.fail("库存记录不存在");
        }
        return Result.ok(stock);
    }

    @Operation(summary = "库存预警检查")
    @GetMapping("/alert")
    public Result<List<Stock>> checkStockAlert() {
        return Result.ok(stockService.checkStockAlert());
    }

    @Operation(summary = "根据产品ID查询库存汇总")
    @GetMapping("/by-product/{productId}")
    public Result<Stock> getByProductId(
            @Parameter(description = "产品ID") @PathVariable Long productId) {
        Stock stock = stockService.getStockByProductId(productId);
        if (stock == null) {
            return Result.fail("该产品暂无库存记录");
        }
        return Result.ok(stock);
    }

    @Operation(summary = "导出库存列表")
    @GetMapping("/export")
    public Result<List<Stock>> export() {
        return Result.ok(stockService.list());
    }
}
