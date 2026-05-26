package cn.aiedge.erp.stock.controller;

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

    @GetMapping("/page")
    @Operation(summary = "分页查询调拨单")
    public Page<StockTransfer> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "调出仓库") @RequestParam(required = false) Long fromWarehouseId,
            @Parameter(description = "调入仓库") @RequestParam(required = false) Long toWarehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return transferService.pageList(keyword, fromWarehouseId, toWarehouseId, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取调拨单详情")
    public StockTransfer getById(@PathVariable Long id) {
        StockTransfer transfer = transferService.getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        return transfer;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取调拨明细")
    public List<StockTransferItem> getItems(@PathVariable Long id) {
        return transferService.getItems(id);
    }

    @PostMapping
    @Operation(summary = "创建调拨单")
    public StockTransfer create(@RequestBody StockTransfer transfer, @RequestBody(required = false) List<StockTransferItem> items) {
        transfer.setTenantId(1L);
        transfer.setCreateBy(StpUtil.getLoginIdAsLong());
        return transferService.createTransfer(transfer, items);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public StockTransfer submitForApproval(@PathVariable Long id) {
        return transferService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public StockTransfer approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return transferService.approve(id, approverId, note);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public StockTransfer reject(@PathVariable Long id, @RequestParam String reason) {
        return transferService.reject(id, reason);
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行调拨")
    public StockTransfer execute(@PathVariable Long id) {
        return transferService.execute(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消调拨")
    public StockTransfer cancel(@PathVariable Long id, @RequestParam String reason) {
        return transferService.cancel(id, reason);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加调拨明细")
    public StockTransferItem addItem(@PathVariable Long id, @RequestBody StockTransferItem item) {
        return transferService.addItem(id, item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新调拨明细")
    public StockTransferItem updateItem(@PathVariable Long itemId, @RequestBody StockTransferItem item) {
        return transferService.updateItem(itemId, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除调拨明细")
    public void removeItem(@PathVariable Long itemId) {
        transferService.removeItem(itemId);
    }
}