package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockCostAdjustItemVO;
import cn.aiedge.erp.stock.dto.StockCostAdjustQuery;
import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import cn.aiedge.erp.stock.service.StockCostAdjustService;
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
@RequestMapping("/api/erp/stock/cost-adjust")
@RequiredArgsConstructor
@Tag(name = "成本调价管理", description = "成本调价单创建、审批、记账执行等操作")
public class StockCostAdjustController {

    private final StockCostAdjustService adjustService;

    @GetMapping("/page")
    @Operation(summary = "分页查询成本调价单(按单据)")
    public Result<Page<StockCostAdjust>> page(@ModelAttribute StockCostAdjustQuery query) {
        return Result.ok(adjustService.pageList(query));
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询成本调价明细(按明细)")
    public Result<Page<StockCostAdjustItemVO>> pageDetail(@ModelAttribute StockCostAdjustQuery query) {
        return Result.ok(adjustService.pageDetail(query));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一成本调价单号")
    public Result<String> nextNo() {
        return Result.ok(adjustService.generateNo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取成本调价单详情")
    public Result<StockCostAdjust> getById(@PathVariable Long id) {
        return Result.ok(adjustService.getDetail(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取成本调价明细")
    public Result<List<StockCostAdjustItem>> getItems(@PathVariable Long id) {
        return Result.ok(adjustService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建成本调价单(保存草稿)")
    public Result<StockCostAdjust> create(@RequestBody StockCostAdjust adjust) {
        List<StockCostAdjustItem> items = adjust.getItems();
        adjust.setItems(null);
        if (adjust.getHandlerId() == null) {
            adjust.setHandlerId(StpUtil.getLoginIdAsLong());
        }
        return Result.ok(adjustService.createAdjust(adjust, items));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新成本调价单(草稿)")
    public Result<StockCostAdjust> update(@PathVariable Long id, @RequestBody StockCostAdjust adjust) {
        List<StockCostAdjustItem> items = adjust.getItems();
        adjust.setItems(null);
        return Result.ok(adjustService.updateAdjust(id, adjust, items));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除成本调价单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockCostAdjust a = adjustService.getById(id);
        if (a != null && a.getStatus() != 0) {
            return Result.fail("只有草稿状态的成本调价单可以删除");
        }
        return Result.ok(adjustService.removeById(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockCostAdjust> submit(@PathVariable Long id) {
        return Result.ok(adjustService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockCostAdjust> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        return Result.ok(adjustService.approve(id, StpUtil.getLoginIdAsLong(), note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockCostAdjust> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(adjustService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行记账(调整成本)")
    public Result<StockCostAdjust> execute(@PathVariable Long id) {
        return Result.ok(adjustService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消成本调价单")
    public Result<StockCostAdjust> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.ok(adjustService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除成本调价单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockCostAdjust a = adjustService.getById(id);
            if (a != null && a.getStatus() != 0) {
                return Result.fail("存在非草稿状态的成本调价单，不能批量删除");
            }
        }
        return Result.ok(adjustService.removeBatchByIds(ids));
    }
}
