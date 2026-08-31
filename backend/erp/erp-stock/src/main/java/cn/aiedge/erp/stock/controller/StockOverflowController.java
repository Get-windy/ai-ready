package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockOverflowItemVO;
import cn.aiedge.erp.stock.dto.StockOverflowQuery;
import cn.aiedge.erp.stock.entity.StockOverflow;
import cn.aiedge.erp.stock.entity.StockOverflowItem;
import cn.aiedge.erp.stock.service.StockOverflowService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/overflow")
@RequiredArgsConstructor
@Tag(name = "报溢单管理", description = "报溢单创建、送审、记账、入库等操作")
public class StockOverflowController {

    private final StockOverflowService overflowService;

    @GetMapping("/page")
    @Operation(summary = "分页查询报溢单(按单据)")
    public Result<Page<StockOverflow>> page(@org.springframework.web.bind.annotation.ModelAttribute StockOverflowQuery query) {
        return Result.ok(overflowService.pageList(query));
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询报溢明细(按明细)")
    public Result<Page<StockOverflowItemVO>> pageDetail(@org.springframework.web.bind.annotation.ModelAttribute StockOverflowQuery query) {
        return Result.ok(overflowService.pageDetail(query));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一报溢单号")
    public Result<String> nextNo() {
        return Result.ok(overflowService.generateNo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取报溢单详情")
    public Result<StockOverflow> getById(@PathVariable Long id) {
        return Result.ok(overflowService.getDetail(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取报溢明细")
    public Result<List<StockOverflowItem>> getItems(@PathVariable Long id) {
        return Result.ok(overflowService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建报溢单(保存草稿)")
    public Result<StockOverflow> create(@RequestBody StockOverflow overflow) {
        List<StockOverflowItem> items = overflow.getItems();
        overflow.setItems(null);
        return Result.ok(overflowService.createOverflow(overflow, items));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报溢单(草稿)")
    public Result<StockOverflow> update(@PathVariable Long id, @RequestBody StockOverflow overflow) {
        List<StockOverflowItem> items = overflow.getItems();
        overflow.setItems(null);
        return Result.ok(overflowService.updateOverflow(id, overflow, items));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报溢单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockOverflow o = overflowService.getById(id);
        if (o != null && o.getStatus() != 0) {
            return Result.fail("只有草稿状态的报溢单可以删除");
        }
        return Result.ok(overflowService.removeById(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockOverflow> submit(@PathVariable Long id) {
        return Result.ok(overflowService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockOverflow> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        return Result.ok(overflowService.approve(id, StpUtil.getLoginIdAsLong(), note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockOverflow> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(overflowService.reject(id, reason));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "执行记账(入库)")
    public Result<StockOverflow> complete(@PathVariable Long id) {
        return Result.ok(overflowService.execute(id));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行入库(记账)")
    public Result<StockOverflow> execute(@PathVariable Long id) {
        return Result.ok(overflowService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消报溢单")
    public Result<StockOverflow> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.ok(overflowService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除报溢单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockOverflow o = overflowService.getById(id);
            if (o != null && o.getStatus() != 0) {
                return Result.fail("存在非草稿状态的报溢单，不能批量删除");
            }
        }
        return Result.ok(overflowService.removeBatchByIds(ids));
    }
}
