package cn.aiedge.erp.sale.saleexchange.controller;

import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.service.SaleExchangeService;
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
@RequestMapping("/api/erp/sale/exchange")
@RequiredArgsConstructor
@Tag(name = "销售换货管理", description = "销售换货单的创建、审批、执行等操作")
public class SaleExchangeController {

    private final SaleExchangeService saleExchangeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询换货单")
    public Page<SaleExchange> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "换货类型") @RequestParam(required = false) Integer exchangeType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return saleExchangeService.pageList(keyword, customerId, status, exchangeType, startDate, endDate, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取换货单详情")
    public SaleExchange getById(@PathVariable Long id) {
        return saleExchangeService.getById(id);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取换货单明细")
    public List<SaleExchangeItem> getItems(@PathVariable Long id) {
        return saleExchangeService.getItems(id);
    }

    @PostMapping
    @Operation(summary = "创建换货单")
    public SaleExchange create(@RequestBody SaleExchange exchange) {
        return saleExchangeService.createExchange(exchange, null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新换货单")
    public SaleExchange update(@PathVariable Long id, @RequestBody SaleExchange exchange) {
        return saleExchangeService.updateExchange(id, exchange, null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除换货单")
    public void delete(@PathVariable Long id) {
        saleExchangeService.removeById(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除换货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return saleExchangeService.removeBatchByIds(ids);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleExchange submit(@PathVariable Long id) {
        return saleExchangeService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批换货单")
    public SaleExchange approve(
            @PathVariable Long id,
            @Parameter(description = "审批人ID") @RequestParam Long approverId,
            @Parameter(description = "审批人姓名") @RequestParam(required = false) String approvedByName,
            @Parameter(description = "审批意见") @RequestParam(required = false) String remark) {
        return saleExchangeService.approve(id, approverId, approvedByName, remark);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝换货单")
    public SaleExchange reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String remark) {
        return saleExchangeService.reject(id, remark);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换货单")
    public SaleExchange cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return saleExchangeService.cancel(id, reason);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换货单")
    public SaleExchange complete(@PathVariable Long id) {
        return saleExchangeService.complete(id);
    }

    @GetMapping("/{id}/approval-records")
    @Operation(summary = "获取审批记录")
    public List<ExchangeApprovalRecord> getApprovalRecords(@PathVariable Long id) {
        return saleExchangeService.getApprovalRecords(id);
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "获取换货单跟踪信息")
    public Map<String, Object> getTracking(@PathVariable Long id) {
        return saleExchangeService.getTracking(id);
    }

    @GetMapping("/export")
    @Operation(summary = "导出换货单列表")
    public List<SaleExchange> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "换货类型") @RequestParam(required = false) Integer exchangeType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return saleExchangeService.exportList(keyword, customerId, status, exchangeType, startDate, endDate);
    }
}
