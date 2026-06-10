package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockCheck;
import cn.aiedge.erp.stock.entity.StockCheckItem;
import cn.aiedge.erp.stock.service.StockCheckService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/check")
@RequiredArgsConstructor
@Tag(name = "库存盘点管理", description = "库存盘点创建、执行、调整等操作")
public class StockCheckController {

    private final StockCheckService checkService;

    @GetMapping("/page")
    @Operation(summary = "分页查询盘点单")
    public Result<Page<StockCheck>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "盘点类型") @RequestParam(required = false) Integer checkType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(checkService.pageList(keyword, warehouseId, status, checkType, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取盘点单详情")
    public Result<StockCheck> getById(@PathVariable Long id) {
        StockCheck check = checkService.getById(id);
        if (check == null) {
            throw BusinessException.notFound("盘点单不存在");
        }
        return Result.ok(check);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取盘点明细")
    public Result<List<StockCheckItem>> getItems(@PathVariable Long id) {
        return Result.ok(checkService.getItems(id));
    }

    @GetMapping("/{id}/diff-items")
    @Operation(summary = "获取差异明细")
    public Result<List<StockCheckItem>> getDiffItems(@PathVariable Long id) {
        return Result.ok(checkService.getDiffItems(id));
    }

    @PostMapping
    @Operation(summary = "创建盘点单")
    public Result<StockCheck> create(@RequestBody StockCheck check) {
        check.setTenantId(1L);
        check.setCreateBy(StpUtil.getLoginIdAsLong());
        return Result.ok(checkService.createCheck(check));
    }

    @PostMapping("/create-with-items/{warehouseId}")
    @Operation(summary = "创建盘点单并生成明细")
    public Result<StockCheck> createWithItems(@PathVariable Long warehouseId) {
        return Result.ok(checkService.createCheckWithItems(warehouseId));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockCheck> submitForApproval(@PathVariable Long id) {
        return Result.ok(checkService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockCheck> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return Result.ok(checkService.approve(id, approverId, note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockCheck> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(checkService.reject(id, reason));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开始盘点")
    public Result<StockCheck> startCheck(@PathVariable Long id) {
        return Result.ok(checkService.startCheck(id));
    }

    @PostMapping("/{id}/items/{itemId}/check")
    @Operation(summary = "盘点明细")
    public Result<StockCheckItem> checkItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal actualQuantity,
            @RequestParam(required = false) String note) {
        return Result.ok(checkService.checkItem(itemId, actualQuantity, note));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成盘点")
    public Result<StockCheck> completeCheck(@PathVariable Long id) {
        return Result.ok(checkService.completeCheck(id));
    }

    @PostMapping("/{id}/adjust")
    @Operation(summary = "库存调整")
    public Result<StockCheck> adjust(@PathVariable Long id) {
        return Result.ok(checkService.adjust(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消盘点")
    public Result<StockCheck> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(checkService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除盘点单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return Result.ok(checkService.removeBatchByIds(ids));
    }

    @GetMapping("/export")
    @Operation(summary = "导出盘点单列表")
    public Result<List<StockCheck>> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "盘点类型") @RequestParam(required = false) Integer checkType) {
        return Result.ok(checkService.exportList(keyword, warehouseId, status, checkType));
    }
}