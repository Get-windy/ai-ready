package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockSplitQuery;
import cn.aiedge.erp.stock.entity.StockSplit;
import cn.aiedge.erp.stock.entity.StockSplitItem;
import cn.aiedge.erp.stock.service.StockSplitService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

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
        private Long inWarehouseId;
        private String inWarehouseName;
        private Long outWarehouseId;
        private String outWarehouseName;
        private String handlerName;
        private String splitDate;
        private BigDecimal splitQuantity;
        private BigDecimal totalCost;
        private String remark;
        private Long productId;
        private String productCode;
        private String productName;
        private String productSpec;
        private String productUnit;
        private Long deptId;
        private String deptName;
        private String summary;
        private String attachment;
        private String creatorName;
        private BigDecimal totalWeight;
        private BigDecimal totalVolume;
        private List<StockSplitItem> items;
    }

    @lombok.Data
    public static class SplitCreateRequest {
        private StockSplit split;
        private List<StockSplitItem> items;
    }

    @SaCheckPermission("stock:split:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询拆分单")
    public Result<Page<StockSplit>> page(@ModelAttribute StockSplitQuery query) {
        return Result.ok(splitService.pageList(query));
    }

    @SaCheckPermission("stock:split:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成拆分单单号")
    public Result<String> nextNo() {
        return Result.ok(splitService.generateNo());
    }

    @SaCheckPermission("stock:split:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取拆分单详情")
    public Result<StockSplit> getById(@PathVariable Long id) {
        StockSplit split = splitService.getById(id);
        if (split == null) {
            throw BusinessException.notFound("拆分单不存在");
        }
        return Result.ok(split);
    }

    @SaCheckPermission("stock:split:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取拆分明细")
    public Result<List<StockSplitItem>> getItems(@PathVariable Long id) {
        return Result.ok(splitService.getItemList(id));
    }

    @SaCheckPermission("stock:split:create")
    @PostMapping
    @Operation(summary = "创建拆分单")
    public Result<StockSplit> create(@RequestBody CreateSplitRequest request) {
        StockSplit split = new StockSplit();
        split.setTenantId(1L);
        split.setBomId(request.getBomId());
        split.setWarehouseId(request.getWarehouseId());
        split.setInWarehouseId(request.getInWarehouseId());
        split.setInWarehouseName(request.getInWarehouseName());
        split.setOutWarehouseId(request.getOutWarehouseId());
        split.setOutWarehouseName(request.getOutWarehouseName());
        split.setHandlerName(request.getHandlerName());
        split.setSplitQuantity(request.getSplitQuantity());
        split.setOutputTotalCost(request.getTotalCost());
        split.setRemark(request.getRemark());
        split.setProductId(request.getProductId());
        split.setProductCode(request.getProductCode());
        split.setProductName(request.getProductName());
        split.setProductSpec(request.getProductSpec());
        split.setProductUnit(request.getProductUnit());
        split.setDeptId(request.getDeptId());
        split.setDeptName(request.getDeptName());
        split.setSummary(request.getSummary());
        split.setAttachment(request.getAttachment());
        split.setCreatorName(request.getCreatorName());
        split.setTotalWeight(request.getTotalWeight());
        split.setTotalVolume(request.getTotalVolume());
        split.setTotalCost(request.getTotalCost());
        split.setCreateBy(StpUtil.getLoginIdAsLong());
        return Result.ok(splitService.createSplit(split, request.getItems()));
    }

    @SaCheckPermission("stock:split:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新草稿拆分单")
    public Result<StockSplit> update(@PathVariable Long id, @RequestBody CreateSplitRequest request) {
        StockSplit split = new StockSplit();
        split.setId(id);
        split.setBomId(request.getBomId());
        split.setWarehouseId(request.getWarehouseId());
        split.setInWarehouseId(request.getInWarehouseId());
        split.setInWarehouseName(request.getInWarehouseName());
        split.setOutWarehouseId(request.getOutWarehouseId());
        split.setOutWarehouseName(request.getOutWarehouseName());
        split.setHandlerName(request.getHandlerName());
        split.setSplitQuantity(request.getSplitQuantity());
        split.setOutputTotalCost(request.getTotalCost());
        split.setRemark(request.getRemark());
        split.setProductId(request.getProductId());
        split.setProductCode(request.getProductCode());
        split.setProductName(request.getProductName());
        split.setProductSpec(request.getProductSpec());
        split.setProductUnit(request.getProductUnit());
        split.setDeptId(request.getDeptId());
        split.setDeptName(request.getDeptName());
        split.setSummary(request.getSummary());
        split.setAttachment(request.getAttachment());
        split.setCreatorName(request.getCreatorName());
        split.setTotalWeight(request.getTotalWeight());
        split.setTotalVolume(request.getTotalVolume());
        split.setTotalCost(request.getTotalCost());
        return Result.ok(splitService.updateSplit(id, split, request.getItems()));
    }

    @SaCheckPermission("stock:split:create")
    @PostMapping("/create-with-items")
    @Operation(summary = "创建拆分单并添加明细")
    public Result<StockSplit> createWithItems(@RequestBody SplitCreateRequest request) {
        return Result.ok(splitService.createSplit(request.getSplit(), request.getItems()));
    }

    @SaCheckPermission("stock:split:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockSplit> submitForApproval(@PathVariable Long id) {
        return Result.ok(splitService.submitForApproval(id));
    }

    @SaCheckPermission("stock:split:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockSplit> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return Result.ok(splitService.approve(id, approverId, note));
    }

    @SaCheckPermission("stock:split:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockSplit> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(splitService.reject(id, reason));
    }

    @SaCheckPermission("stock:split:execute")
    @PostMapping("/{id}/execute")
    @Operation(summary = "执行拆分（扣减原料库存，增加子件库存）")
    public Result<StockSplit> execute(@PathVariable Long id) {
        return Result.ok(splitService.execute(id));
    }

    @SaCheckPermission("stock:split:create")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消拆分单")
    public Result<StockSplit> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(splitService.cancel(id, reason));
    }
}
