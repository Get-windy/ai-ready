package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockCostAdjustCreateRequest;
import cn.aiedge.erp.stock.entity.StockCostAdjust;
import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import cn.aiedge.erp.stock.service.StockCostAdjustService;
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
@RequestMapping("/api/erp/stock/cost-adjust")
@RequiredArgsConstructor
@Tag(name = "成本调价管理", description = "成本调价单创建、审批、执行等操作")
public class StockCostAdjustController {

    private final StockCostAdjustService adjustService;

    @GetMapping("/page")
    @Operation(summary = "分页查询成本调价单")
    public Result<Page<StockCostAdjust>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(adjustService.pageList(keyword, warehouseId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取成本调价单详情")
    public Result<StockCostAdjust> getById(@PathVariable Long id) {
        StockCostAdjust adjust = adjustService.getById(id);
        if (adjust == null) {
            throw BusinessException.notFound("成本调价单不存在");
        }
        return Result.ok(adjust);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取成本调价明细")
    public Result<List<StockCostAdjustItem>> getItems(@PathVariable Long id) {
        return Result.ok(adjustService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建成本调价单")
    public Result<StockCostAdjust> create(@RequestBody StockCostAdjustCreateRequest request) {
        StockCostAdjust adjust = new StockCostAdjust();
        adjust.setTenantId(1L);
        adjust.setAdjustType(request.getAdjustType());
        adjust.setAdjustDate(request.getAdjustDate());
        adjust.setWarehouseId(request.getWarehouseId());
        adjust.setWarehouseName(request.getWarehouseName());
        adjust.setReasonType(request.getReasonType());
        adjust.setReasonDesc(request.getReasonDesc());
        adjust.setRemark(request.getRemark());
        adjust.setCreateBy(StpUtil.getLoginIdAsLong());
        return Result.ok(adjustService.createAdjust(adjust, request.getItems()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockCostAdjust> submitForApproval(@PathVariable Long id) {
        return Result.ok(adjustService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockCostAdjust> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return Result.ok(adjustService.approve(id, approverId, note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockCostAdjust> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(adjustService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行成本调价")
    public Result<StockCostAdjust> execute(@PathVariable Long id) {
        return Result.ok(adjustService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消成本调价")
    public Result<StockCostAdjust> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(adjustService.cancel(id, reason));
    }
}
