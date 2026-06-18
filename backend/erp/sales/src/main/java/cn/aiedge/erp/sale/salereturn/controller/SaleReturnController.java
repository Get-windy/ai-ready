package cn.aiedge.erp.sale.salereturn.controller;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
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
@RequestMapping("/api/erp/sale/return")
@RequiredArgsConstructor
@Tag(name = "销售退货管理", description = "销售退货单的创建、审批、执行等操作")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    @GetMapping("/page")
    @Operation(summary = "分页查询退货单")
    public Page<SaleReturn> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return saleReturnService.pageList(keyword, customerId, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取退货单详情")
    public SaleReturn getById(@PathVariable Long id) {
        return saleReturnService.getById(id);
    }

    @GetMapping("/returnNo/{returnNo}")
    @Operation(summary = "根据退货单号获取退货单")
    public SaleReturn getByReturnNo(@PathVariable String returnNo) {
        return saleReturnService.getByReturnNo(returnNo);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户退货单列表")
    public List<SaleReturn> listByCustomerId(@PathVariable Long customerId) {
        return saleReturnService.listByCustomerId(customerId);
    }

    @PostMapping
    @Operation(summary = "创建退货单")
    public SaleReturn create(@RequestBody SaleReturn returnOrder, @RequestBody List<SaleReturnItem> items) {
        return saleReturnService.createReturn(returnOrder, items);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleReturn submit(@PathVariable Long id) {
        return saleReturnService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批退货单")
    public SaleReturn approve(
            @PathVariable Long id,
            @Parameter(description = "审批人ID") @RequestParam Long approverId,
            @Parameter(description = "审批意见") @RequestParam(required = false) String note) {
        return saleReturnService.approve(id, approverId, note);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝退货单")
    public SaleReturn reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String reason) {
        return saleReturnService.reject(id, reason);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成退货单")
    public SaleReturn complete(@PathVariable Long id) {
        return saleReturnService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消退货单")
    public SaleReturn cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return saleReturnService.cancel(id, reason);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取退货单明细")
    public List<SaleReturnItem> getItems(@PathVariable Long id) {
        return saleReturnService.getItems(id);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加退货明细")
    public SaleReturnItem addItem(@PathVariable Long id, @RequestBody SaleReturnItem item) {
        return saleReturnService.addItem(id, item);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "更新退货明细")
    public SaleReturnItem updateItem(@PathVariable Long itemId, @RequestBody SaleReturnItem item) {
        return saleReturnService.updateItem(itemId, item);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "删除退货明细")
    public void removeItem(@PathVariable Long itemId) {
        saleReturnService.removeItem(itemId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除退货单")
    public void delete(@PathVariable Long id) {
        saleReturnService.removeById(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除退货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return saleReturnService.removeBatchByIds(ids);
    }

    @GetMapping("/export")
    @Operation(summary = "导出退货单列表")
    public List<SaleReturn> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return saleReturnService.exportList(keyword, customerId, status);
    }
}
