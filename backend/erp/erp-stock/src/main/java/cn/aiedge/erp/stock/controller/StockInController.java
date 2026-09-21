package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockInItemVO;
import cn.aiedge.erp.stock.dto.StockInQuery;
import cn.aiedge.erp.stock.entity.StockIn;
import cn.aiedge.erp.stock.entity.StockInItem;
import cn.aiedge.erp.stock.service.StockInService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/in")
@RequiredArgsConstructor
@Tag(name = "其他入库管理", description = "其他入库单创建、送审、记账、入库等操作")
public class StockInController {

    private final StockInService stockInService;

    @SaCheckPermission("stock:in:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询入库单(按单据)")
    public Result<Page<StockIn>> page(@Parameter(description = "查询条件") @ModelAttribute StockInQuery query) {
        return Result.ok(stockInService.pageList(query));
    }

    @SaCheckPermission("stock:in:view")
    @GetMapping("/page-detail")
    @Operation(summary = "分页查询入库明细(按明细)")
    public Result<Page<StockInItemVO>> pageDetail(@Parameter(description = "查询条件") @ModelAttribute StockInQuery query) {
        return Result.ok(stockInService.pageDetail(query));
    }

    @SaCheckPermission("stock:in:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成下一入库单号")
    public Result<String> nextNo() {
        return Result.ok(stockInService.generateNo());
    }

    @SaCheckPermission("stock:in:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取入库单详情")
    public Result<StockIn> getById(@PathVariable Long id) {
        return Result.ok(stockInService.getDetail(id));
    }

    @SaCheckPermission("stock:in:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取入库明细")
    public Result<List<StockInItem>> getItems(@PathVariable Long id) {
        return Result.ok(stockInService.getItems(id));
    }

    @SaCheckPermission("stock:in:create")
    @PostMapping
    @Operation(summary = "创建入库单(保存草稿)")
    public Result<StockIn> create(@RequestBody StockIn stockIn) {
        List<StockInItem> items = stockIn.getItems();
        stockIn.setItems(null);
        return Result.ok(stockInService.createStockIn(stockIn, items));
    }

    @SaCheckPermission("stock:in:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新入库单(草稿)")
    public Result<StockIn> update(@PathVariable Long id, @RequestBody StockIn stockIn) {
        List<StockInItem> items = stockIn.getItems();
        stockIn.setItems(null);
        return Result.ok(stockInService.updateStockIn(id, stockIn, items));
    }

    @SaCheckPermission("stock:in:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除入库单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockIn d = stockInService.getById(id);
        if (d != null && d.getStatus() != 0) {
            return Result.fail("只有草稿状态的入库单可以删除");
        }
        return Result.ok(stockInService.removeById(id));
    }

    @SaCheckPermission("stock:in:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockIn> submit(@PathVariable Long id) {
        return Result.ok(stockInService.submitForApproval(id));
    }

    @SaCheckPermission("stock:in:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockIn> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        return Result.ok(stockInService.approve(id, StpUtil.getLoginIdAsLong(), note));
    }

    @SaCheckPermission("stock:in:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockIn> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(stockInService.reject(id, reason));
    }

    @SaCheckPermission("stock:in:create")
    @PostMapping("/{id}/complete")
    @Operation(summary = "执行入库(记账)")
    public Result<StockIn> complete(@PathVariable Long id) {
        return Result.ok(stockInService.execute(id));
    }

    @SaCheckPermission("stock:in:create")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消入库单")
    public Result<StockIn> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.ok(stockInService.cancel(id, reason));
    }

    @SaCheckPermission("stock:in:delete")
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除入库单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockIn d = stockInService.getById(id);
            if (d != null && d.getStatus() != 0) {
                return Result.fail("存在非草稿状态的入库单，不能批量删除");
            }
        }
        return Result.ok(stockInService.removeBatchByIds(ids));
    }
}
