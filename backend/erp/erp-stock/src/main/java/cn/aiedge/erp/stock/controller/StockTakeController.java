package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockTakeItemVO;
import cn.aiedge.erp.stock.dto.StockTakeQuery;
import cn.aiedge.erp.stock.dto.StockTakeUncheckedVO;
import cn.aiedge.erp.stock.entity.StockTake;
import cn.aiedge.erp.stock.entity.StockTakeItem;
import cn.aiedge.erp.stock.service.StockTakeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/take")
@RequiredArgsConstructor
@Tag(name = "盘点单管理", description = "盘点单创建、保存、盘点处理(生成报损/报溢单)、未盘商品查询等操作")
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询盘点单(按单据)")
    public Result<Page<StockTake>> page(@Parameter(description = "查询条件") @ModelAttribute StockTakeQuery query) {
        return Result.ok(stockTakeService.pageList(query));
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询盘点明细(按明细)")
    public Result<Page<StockTakeItemVO>> pageDetail(@Parameter(description = "查询条件") @ModelAttribute StockTakeQuery query) {
        return Result.ok(stockTakeService.pageDetail(query));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一盘点单号")
    public Result<String> nextNo() {
        return Result.ok(stockTakeService.generateNo());
    }

    @GetMapping("/unchecked-products")
    @Operation(summary = "未盘商品查询(按仓库列出库存商品，排除已盘))")
    public Result<List<StockTakeUncheckedVO>> uncheckedProducts(@ModelAttribute StockTakeQuery query) {
        return Result.ok(stockTakeService.uncheckedProducts(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取盘点单详情")
    public Result<StockTake> getById(@PathVariable Long id) {
        return Result.ok(stockTakeService.getDetail(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取盘点明细")
    public Result<List<StockTakeItem>> getItems(@PathVariable Long id) {
        return Result.ok(stockTakeService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建盘点单(保存)")
    public Result<StockTake> create(@RequestBody StockTake stockTake) {
        List<StockTakeItem> items = stockTake.getItems();
        stockTake.setItems(null);
        return Result.ok(stockTakeService.createStockTake(stockTake, items));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新盘点单(保存)")
    public Result<StockTake> update(@PathVariable Long id, @RequestBody StockTake stockTake) {
        List<StockTakeItem> items = stockTake.getItems();
        stockTake.setItems(null);
        return Result.ok(stockTakeService.updateStockTake(id, stockTake, items));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除盘点单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockTake d = stockTakeService.getById(id);
        if (d != null && d.getStatus() == 2) {
            return Result.fail("已盘点的盘点单不能删除");
        }
        return Result.ok(stockTakeService.removeById(id));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "盘点处理(生成报损单/报溢单)")
    public Result<StockTake> process(@PathVariable Long id) {
        return Result.ok(stockTakeService.process(id));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除盘点单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockTake d = stockTakeService.getById(id);
            if (d != null && d.getStatus() == 2) {
                return Result.fail("存在已盘点的盘点单，不能批量删除");
            }
        }
        return Result.ok(stockTakeService.removeBatchByIds(ids));
    }
}
