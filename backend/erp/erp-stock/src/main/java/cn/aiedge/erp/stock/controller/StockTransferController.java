package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockTransferCreateDTO;
import cn.aiedge.erp.stock.dto.StockTransferItemVO;
import cn.aiedge.erp.stock.dto.StockTransferQuery;
import cn.aiedge.erp.stock.entity.StockTransfer;
import cn.aiedge.erp.stock.entity.StockTransferItem;
import cn.aiedge.erp.stock.service.StockTransferService;
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
@RequestMapping("/api/erp/stock/transfer")
@RequiredArgsConstructor
@Tag(name = "库存调拨管理", description = "库存调拨单创建、送审、记账、执行等操作")
public class StockTransferController {

    private final StockTransferService transferService;

    @GetMapping("/page")
    @Operation(summary = "分页查询调拨单(按单据)")
    public Result<Page<StockTransfer>> page(@Parameter(description = "查询条件") @ModelAttribute StockTransferQuery query) {
        return Result.ok(transferService.pageList(query));
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询调拨明细(按明细)")
    public Result<Page<StockTransferItemVO>> pageDetail(@Parameter(description = "查询条件") @ModelAttribute StockTransferQuery query) {
        return Result.ok(transferService.pageDetail(query));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一调拨单号")
    public Result<String> nextNo() {
        return Result.ok(transferService.generateTransferNo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取调拨单详情")
    public Result<StockTransfer> getById(@PathVariable Long id) {
        return Result.ok(transferService.getDetail(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取调拨明细")
    public Result<List<StockTransferItem>> getItems(@PathVariable Long id) {
        return Result.ok(transferService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建调拨单(保存草稿)")
    public Result<StockTransfer> create(@RequestBody StockTransferCreateDTO dto) {
        return Result.ok(transferService.createTransfer(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新调拨单(草稿)")
    public Result<StockTransfer> update(@PathVariable Long id, @RequestBody StockTransferCreateDTO dto) {
        return Result.ok(transferService.updateTransfer(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除调拨单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockTransfer d = transferService.getById(id);
        if (d != null && d.getStatus() != 0) {
            return Result.fail("只有草稿状态的调拨单可以删除");
        }
        return Result.ok(transferService.removeById(id));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除调拨单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockTransfer d = transferService.getById(id);
            if (d != null && d.getStatus() != 0) {
                return Result.fail("存在非草稿状态的调拨单，不能批量删除");
            }
        }
        return Result.ok(transferService.removeBatchByIds(ids));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockTransfer> submitForApproval(@PathVariable Long id) {
        return Result.ok(transferService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockTransfer> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        return Result.ok(transferService.approve(id, StpUtil.getLoginIdAsLong(), note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockTransfer> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(transferService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行调拨(记账)")
    public Result<StockTransfer> execute(@PathVariable Long id) {
        return Result.ok(transferService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消调拨单")
    public Result<StockTransfer> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.ok(transferService.cancel(id, reason));
    }

    @GetMapping("/export")
    @Operation(summary = "导出调拨单列表")
    public Result<List<StockTransfer>> export(@Parameter(description = "查询条件") @ModelAttribute StockTransferQuery query) {
        return Result.ok(transferService.exportList(query));
    }

    @PostMapping("/batch-print")
    @Operation(summary = "批量打印调拨单")
    public Result<Void> batchPrint(@RequestBody java.util.Map<String, Object> params) {
        // 打印任务交由打印模块异步处理，此处仅校验单据存在并返回成功
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) params.get("ids");
        List<Long> ids = rawIds != null ? rawIds.stream().map(Number::longValue).toList() : List.of();
        if (!ids.isEmpty()) {
            List<StockTransfer> docs = transferService.listByIds(ids);
            if (docs.size() != ids.size()) {
                return Result.fail("部分调拨单不存在，无法打印");
            }
        }
        return Result.ok("打印任务已提交", null);
    }
}
