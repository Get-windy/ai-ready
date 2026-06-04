package cn.aiedge.erp.purchase.purchasereturn.controller;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
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
@RequestMapping("/api/erp/purchase-return")
@RequiredArgsConstructor
@Tag(name = "采购退货管理", description = "采购退货单的创建、审批、执行等操作")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @GetMapping("/page")
    @Operation(summary = "分页查询退货单")
    public Page<PurchaseReturn> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return purchaseReturnService.pageList(keyword, supplierId, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取退货单详情")
    public PurchaseReturn getById(@PathVariable Long id) {
        return purchaseReturnService.getById(id);
    }

    @GetMapping("/returnNo/{returnNo}")
    @Operation(summary = "根据退货单号获取退货单")
    public PurchaseReturn getByReturnNo(@PathVariable String returnNo) {
        return purchaseReturnService.getByReturnNo(returnNo);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "获取供应商退货单列表")
    public List<PurchaseReturn> listBySupplierId(@PathVariable Long supplierId) {
        return purchaseReturnService.listBySupplierId(supplierId);
    }

    @PostMapping
    @Operation(summary = "创建退货单")
    public PurchaseReturn create(@RequestBody PurchaseReturn returnOrder, @RequestBody List<PurchaseReturnItem> items) {
        return purchaseReturnService.createReturn(returnOrder, items);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public PurchaseReturn submit(@PathVariable Long id) {
        return purchaseReturnService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批退货单")
    public PurchaseReturn approve(
            @PathVariable Long id,
            @Parameter(description = "审批人ID") @RequestParam Long approverId,
            @Parameter(description = "审批意见") @RequestParam(required = false) String note) {
        return purchaseReturnService.approve(id, approverId, note);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝退货单")
    public PurchaseReturn reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String reason) {
        return purchaseReturnService.reject(id, reason);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成退货单")
    public PurchaseReturn complete(@PathVariable Long id) {
        return purchaseReturnService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消退货单")
    public PurchaseReturn cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return purchaseReturnService.cancel(id, reason);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取退货单明细")
    public List<PurchaseReturnItem> getItems(@PathVariable Long id) {
        return purchaseReturnService.getItems(id);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加退货明细")
    public PurchaseReturnItem addItem(@PathVariable Long id, @RequestBody PurchaseReturnItem item) {
        return purchaseReturnService.addItem(id, item);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "更新退货明细")
    public PurchaseReturnItem updateItem(@PathVariable Long itemId, @RequestBody PurchaseReturnItem item) {
        return purchaseReturnService.updateItem(itemId, item);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "删除退货明细")
    public void removeItem(@PathVariable Long itemId) {
        purchaseReturnService.removeItem(itemId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除退货单")
    public void delete(@PathVariable Long id) {
        purchaseReturnService.removeById(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除退货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return purchaseReturnService.removeBatchByIds(ids);
    }

    @GetMapping("/export")
    @Operation(summary = "导出退货单列表")
    public List<PurchaseReturn> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return purchaseReturnService.exportList(keyword, supplierId, status);
    }
}