package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockOutItemVO;
import cn.aiedge.erp.stock.dto.StockOutQuery;
import cn.aiedge.erp.stock.entity.StockOut;
import cn.aiedge.erp.stock.entity.StockOutItem;
import cn.aiedge.erp.stock.service.StockOutService;
import cn.dev33.satoken.stp.StpUtil;
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
@RequestMapping("/api/erp/stock/out")
@RequiredArgsConstructor
@Tag(name = "其他出库管理", description = "其他出库单创建、送审、记账、出库等操作")
public class StockOutController {

    private final StockOutService stockOutService;

    @GetMapping("/page")
    @Operation(summary = "分页查询出库单(按单据)")
    public Result<Page<StockOut>> page(@Parameter(description = "查询条件") @ModelAttribute StockOutQuery query) {
        return Result.ok(stockOutService.pageList(query));
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询出库明细(按明细)")
    public Result<Page<StockOutItemVO>> pageDetail(@Parameter(description = "查询条件") @ModelAttribute StockOutQuery query) {
        return Result.ok(stockOutService.pageDetail(query));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一出库单号")
    public Result<String> nextNo() {
        return Result.ok(stockOutService.generateNo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取出库单详情")
    public Result<StockOut> getById(@PathVariable Long id) {
        return Result.ok(stockOutService.getDetail(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取出库明细")
    public Result<List<StockOutItem>> getItems(@PathVariable Long id) {
        return Result.ok(stockOutService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建出库单(保存草稿)")
    public Result<StockOut> create(@RequestBody StockOut stockOut) {
        List<StockOutItem> items = stockOut.getItems();
        stockOut.setItems(null);
        return Result.ok(stockOutService.createStockOut(stockOut, items));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新出库单(草稿)")
    public Result<StockOut> update(@PathVariable Long id, @RequestBody StockOut stockOut) {
        List<StockOutItem> items = stockOut.getItems();
        stockOut.setItems(null);
        return Result.ok(stockOutService.updateStockOut(id, stockOut, items));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除出库单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockOut d = stockOutService.getById(id);
        if (d != null && d.getStatus() != 0) {
            return Result.fail("只有草稿状态的出库单可以删除");
        }
        return Result.ok(stockOutService.removeById(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockOut> submit(@PathVariable Long id) {
        return Result.ok(stockOutService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockOut> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        return Result.ok(stockOutService.approve(id, StpUtil.getLoginIdAsLong(), note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockOut> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(stockOutService.reject(id, reason));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "执行出库(记账)")
    public Result<StockOut> complete(@PathVariable Long id) {
        return Result.ok(stockOutService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消出库单")
    public Result<StockOut> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.ok(stockOutService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除出库单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockOut d = stockOutService.getById(id);
            if (d != null && d.getStatus() != 0) {
                return Result.fail("存在非草稿状态的出库单，不能批量删除");
            }
        }
        return Result.ok(stockOutService.removeBatchByIds(ids));
    }
}
