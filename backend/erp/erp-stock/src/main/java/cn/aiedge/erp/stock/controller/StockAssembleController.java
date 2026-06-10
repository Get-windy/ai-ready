package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockAssemble;
import cn.aiedge.erp.stock.entity.StockAssembleItem;
import cn.aiedge.erp.stock.service.StockAssembleService;
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
@RequestMapping("/api/erp/stock/assemble")
@RequiredArgsConstructor
@Tag(name = "组装管理", description = "组装单创建、审批、执行等操作")
public class StockAssembleController {

    private final StockAssembleService assembleService;

    @lombok.Data
    public static class CreateAssembleRequest {
        private Long bomId;
        private Long warehouseId;
        private BigDecimal assembleQuantity;
        private BigDecimal assembleFee;
        private String remark;
        private List<StockAssembleItem> items;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询组装单")
    public Result<Page<StockAssemble>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(assembleService.pageList(keyword, warehouseId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组装单详情")
    public Result<StockAssemble> getById(@PathVariable Long id) {
        StockAssemble assemble = assembleService.getById(id);
        if (assemble == null) {
            throw BusinessException.notFound("组装单不存在");
        }
        return Result.ok(assemble);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取组装明细")
    public Result<List<StockAssembleItem>> getItems(@PathVariable Long id) {
        return Result.ok(assembleService.getItemList(id));
    }

    @PostMapping
    @Operation(summary = "创建组装单")
    public Result<StockAssemble> create(@RequestBody CreateAssembleRequest request) {
        StockAssemble assemble = new StockAssemble();
        assemble.setTenantId(1L);
        assemble.setBomId(request.getBomId());
        assemble.setWarehouseId(request.getWarehouseId());
        assemble.setAssembleQuantity(request.getAssembleQuantity());
        assemble.setAssembleFee(request.getAssembleFee());
        assemble.setRemark(request.getRemark());
        assemble.setCreateBy(StpUtil.getLoginIdAsLong());
        return Result.ok(assembleService.createAssemble(assemble, request.getItems()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockAssemble> submitForApproval(@PathVariable Long id) {
        return Result.ok(assembleService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockAssemble> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return Result.ok(assembleService.approve(id, approverId, note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockAssemble> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(assembleService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行组装（扣减子件库存，增加成品库存）")
    public Result<StockAssemble> execute(@PathVariable Long id) {
        return Result.ok(assembleService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消组装单")
    public Result<StockAssemble> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(assembleService.cancel(id, reason));
    }
}
