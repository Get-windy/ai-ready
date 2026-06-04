package cn.aiedge.erp.purchase.purchaseexchange.controller;

import cn.aiedge.erp.purchase.purchaseexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchangeItem;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/erp/purchase/exchange")
@RequiredArgsConstructor
@Tag(name = "采购换货管理", description = "采购换货单的创建、审批、执行等操作")
public class PurchaseExchangeController {

    private final PurchaseExchangeService purchaseExchangeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询换货单")
    public Page<PurchaseExchange> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "换货类型") @RequestParam(required = false) Integer exchangeType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return purchaseExchangeService.pageList(keyword, supplierId, status, exchangeType, startDate, endDate, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取换货单详情")
    public PurchaseExchange getById(@PathVariable Long id) {
        return purchaseExchangeService.getById(id);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取换货单明细")
    public List<PurchaseExchangeItem> getItems(@PathVariable Long id) {
        return purchaseExchangeService.getItems(id);
    }

    @PostMapping
    @Operation(summary = "创建换货单")
    public PurchaseExchange create(@RequestBody PurchaseExchange exchange) {
        return purchaseExchangeService.createExchange(exchange, null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新换货单")
    public PurchaseExchange update(@PathVariable Long id, @RequestBody PurchaseExchange exchange) {
        return purchaseExchangeService.updateExchange(id, exchange, null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除换货单")
    public void delete(@PathVariable Long id) {
        purchaseExchangeService.removeById(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除换货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return purchaseExchangeService.removeBatchByIds(ids);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public PurchaseExchange submit(@PathVariable Long id) {
        return purchaseExchangeService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批换货单")
    public PurchaseExchange approve(
            @PathVariable Long id,
            @Parameter(description = "审批人ID") @RequestParam Long approverId,
            @Parameter(description = "审批人姓名") @RequestParam(required = false) String approvedByName,
            @Parameter(description = "审批意见") @RequestParam(required = false) String remark) {
        return purchaseExchangeService.approve(id, approverId, approvedByName, remark);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝换货单")
    public PurchaseExchange reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String remark) {
        return purchaseExchangeService.reject(id, remark);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换货单")
    public PurchaseExchange cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return purchaseExchangeService.cancel(id, reason);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换货单")
    public PurchaseExchange complete(@PathVariable Long id) {
        return purchaseExchangeService.complete(id);
    }

    @GetMapping("/{id}/approval-records")
    @Operation(summary = "获取审批记录")
    public List<ExchangeApprovalRecord> getApprovalRecords(@PathVariable Long id) {
        return purchaseExchangeService.getApprovalRecords(id);
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "获取换货单跟踪信息")
    public Map<String, Object> getTracking(@PathVariable Long id) {
        return purchaseExchangeService.getTracking(id);
    }

    @GetMapping("/export")
    @Operation(summary = "导出换货单列表")
    public List<PurchaseExchange> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "换货类型") @RequestParam(required = false) Integer exchangeType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return purchaseExchangeService.exportList(keyword, supplierId, status, exchangeType, startDate, endDate);
    }
}
