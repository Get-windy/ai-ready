package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockSplit;
import cn.aiedge.erp.stock.entity.StockSplitItem;
import cn.aiedge.erp.stock.service.StockSplitService;
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
@RequestMapping("/api/erp/stock/split")
@RequiredArgsConstructor
@Tag(name = "拆分管理", description = "拆分单创建、审批、执行等操作")
public class StockSplitController {

    private final StockSplitService splitService;

    @lombok.Data
    public static class CreateSplitRequest {
        private Long bomId;
        private Long warehouseId;
        private BigDecimal splitQuantity;
        private String remark;
        private List<StockSplitItem> items;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询拆分单")
    public Result<Page<StockSplit>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(splitService.pageList(keyword, warehouseId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取拆分单详情")
    public Result<StockSplit> getById(@PathVariable Long id) {
        StockSplit split = splitService.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        return Result.ok(split);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取拆分明细")
    public Result<List<StockSplitItem>> getItems(@PathVariable Long id) {
        return Result.ok(splitService.getItemList(id));
    }

    @PostMapping
    @Operation(summary = "创建拆分单")
    public Result<StockSplit> create(@RequestBody CreateSplitRequest request) {
        StockSplit split = new StockSplit();
        split.setTenantId(1L);
        split.setBomId(request.getBomId());
        split.setWarehouseId(request.getWarehouseId());
        split.setSplitQuantity(request.getSplitQuantity());
        split.setRemark(request.getRemark());
        split.setCreateBy(StpUtil.getLoginIdAsLong());
        return Result.ok(splitService.createSplit(split, request.getItems()));
    }

    @PostMapping("/create-with-items")
    @Operation(summary = "创建拆分单并添加明细")
    public Result<StockSplit> createWithItems(@RequestBody SplitCreateRequest request) {
        return Result.ok(splitService.createSplit(request.getSplit(), request.getItems()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockSplit> submitForApproval(@PathVariable Long id) {
        return Result.ok(splitService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockSplit> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return Result.ok(splitService.approve(id, approverId, note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockSplit> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(splitService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行拆分（扣减原料库存，增加子件库存）")
    public Result<StockSplit> execute(@PathVariable Long id) {
        return splitService.execute(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消拆分单")
    public Result<StockSplit> cancel(@PathVariable Long id, @RequestParam String reason) {
        return splitService.cancel(id, reason);
    }
}
