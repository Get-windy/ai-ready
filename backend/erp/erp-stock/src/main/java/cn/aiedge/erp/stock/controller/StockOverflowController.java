package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockOverflow;
import cn.aiedge.erp.stock.entity.StockOverflowItem;
import cn.aiedge.erp.stock.service.StockOverflowService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/overflow")
@RequiredArgsConstructor
@Tag(name = "报溢管理", description = "报溢单创建、审批、入库等操作")
public class StockOverflowController {

    private final StockOverflowService overflowService;

    @lombok.Data
    public static class CreateOverflowRequest {
        private LocalDate overflowDate;
        private Long warehouseId;
        private Long locationId;
        private String sourceType;
        private String remark;
        private List<StockOverflowItem> items;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询报溢单")
    public Result<Page<StockOverflow>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(overflowService.pageList(keyword, warehouseId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取报溢单详情")
    public Result<StockOverflow> getById(@PathVariable Long id) {
        StockOverflow o = overflowService.getById(id);
        if (o == null) throw BusinessException.notFound("报溢单不存在");
        return Result.ok(o);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取报溢明细")
    public Result<List<StockOverflowItem>> getItems(@PathVariable Long id) {
        return Result.ok(overflowService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建报溢单")
    public Result<StockOverflow> create(@RequestBody CreateOverflowRequest request) {
        StockOverflow overflow = new StockOverflow();
        overflow.setTenantId(1L);
        overflow.setOverflowDate(request.getOverflowDate());
        overflow.setWarehouseId(request.getWarehouseId());
        overflow.setLocationId(request.getLocationId());
        overflow.setSourceType(request.getSourceType());
        overflow.setRemark(request.getRemark());
        return Result.ok(overflowService.createOverflow(overflow, request.getItems()));
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

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行入库")
    public Result<StockOverflow> execute(@PathVariable Long id) {
        return Result.ok(overflowService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消报溢单")
    public Result<StockOverflow> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(overflowService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return Result.ok(overflowService.removeBatchByIds(ids));
    }
}
