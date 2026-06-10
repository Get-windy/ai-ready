package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
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
@Tag(name = "库存调拨管理", description = "库存调拨创建、审批、执行等操作")
public class StockTransferController {

    private final StockTransferService transferService;

    @lombok.Data
    public static class CreateTransferRequest {
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private String remark;
        private List<StockTransferItem> items;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询调拨单")
    public Result<Page<StockTransfer>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "调出仓库") @RequestParam(required = false) Long fromWarehouseId,
            @Parameter(description = "调入仓库") @RequestParam(required = false) Long toWarehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(transferService.pageList(keyword, fromWarehouseId, toWarehouseId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取调拨单详情")
    public Result<StockTransfer> getById(@PathVariable Long id) {
        StockTransfer transfer = transferService.getById(id);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        return Result.ok(transfer);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取调拨明细")
    public Result<List<StockTransferItem>> getItems(@PathVariable Long id) {
        return Result.ok(transferService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建调拨单")
    public Result<StockTransfer> create(@RequestBody CreateTransferRequest request) {
        StockTransfer transfer = new StockTransfer();
        transfer.setTenantId(1L);
        transfer.setFromWarehouseId(request.getFromWarehouseId());
        transfer.setToWarehouseId(request.getToWarehouseId());
        transfer.setRemark(request.getRemark());
        transfer.setCreateBy(StpUtil.getLoginIdAsLong());
        return Result.ok(transferService.createTransfer(transfer, request.getItems()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockTransfer> submitForApproval(@PathVariable Long id) {
        return Result.ok(transferService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockTransfer> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return Result.ok(transferService.approve(id, approverId, note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockTransfer> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(transferService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行调拨")
    public Result<StockTransfer> execute(@PathVariable Long id) {
        return Result.ok(transferService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消调拨")
    public Result<StockTransfer> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(transferService.cancel(id, reason));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加调拨明细")
    public Result<StockTransferItem> addItem(@PathVariable Long id, @RequestBody StockTransferItem item) {
        return Result.ok(transferService.addItem(id, item));
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新调拨明细")
    public Result<StockTransferItem> updateItem(@PathVariable Long itemId, @RequestBody StockTransferItem item) {
        return Result.ok(transferService.updateItem(itemId, item));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除调拨明细")
    public Result<Void> removeItem(@PathVariable Long itemId) {
        transferService.removeItem(itemId);
        return Result.ok();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除调拨单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return Result.ok(transferService.removeBatchByIds(ids));
    }

    @GetMapping("/export")
    @Operation(summary = "导出调拨单列表")
    public Result<List<StockTransfer>> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "调出仓库") @RequestParam(required = false) Long fromWarehouseId,
            @Parameter(description = "调入仓库") @RequestParam(required = false) Long toWarehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.ok(transferService.exportList(keyword, fromWarehouseId, toWarehouseId, status));
    }
}