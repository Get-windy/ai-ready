package cn.aiedge.erp.stock.controller;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.service.StockService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 库存管理Controller
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "库存管理", description = "库存查询、盘点、调拨等接口")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StockController {

    private final StockService stockService;

    @Operation(summary = "查询库存详情")
    @GetMapping("/{productId}/{warehouseId}")
    public Stock getStockDetail(
            @Parameter(description = "产品ID") @PathVariable Long productId,
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId) {
        return stockService.getStockDetail(productId, warehouseId);
    }

    @Operation(summary = "库存增加")
    @PostMapping("/increase")
    public boolean increaseStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "增加数量") @RequestParam BigDecimal quantity) {
        return stockService.increaseStock(productId, warehouseId, quantity);
    }

    @Operation(summary = "库存减少")
    @PostMapping("/decrease")
    public boolean decreaseStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "减少数量") @RequestParam BigDecimal quantity) {
        return stockService.decreaseStock(productId, warehouseId, quantity);
    }

    @Operation(summary = "库存盘点")
    @PostMapping("/check")
    public boolean checkStock(
            @Parameter(description = "产品ID") @RequestParam Long productId,
            @Parameter(description = "仓库ID") @RequestParam Long warehouseId,
            @Parameter(description = "实际数量") @RequestParam BigDecimal actualQuantity) {
        return stockService.dispanStock(productId, warehouseId, actualQuantity);
    }

    @Operation(summary = "查询库存列表")
    @GetMapping("/list")
    public Page<Stock> getStockList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size) {
        return stockService.page(new Page<>(current, size));
    }

    @Operation(summary = "库存预警检查")
    @GetMapping("/alert")
    public java.util.List<Stock> checkStockAlert() {
        return stockService.checkStockAlert();
    }
}
