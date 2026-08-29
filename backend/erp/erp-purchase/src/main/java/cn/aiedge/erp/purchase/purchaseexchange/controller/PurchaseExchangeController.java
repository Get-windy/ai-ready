package cn.aiedge.erp.purchase.purchaseexchange.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.purchaseexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchangeItem;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
import cn.dev33.satoken.stp.StpUtil;
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
@RequestMapping("/api/erp/purchase/exchange")
@RequiredArgsConstructor
@Tag(name = "采购换货管理", description = "采购换货单的创建、审批、执行等操作")
public class PurchaseExchangeController {

    private final PurchaseExchangeService purchaseExchangeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询换货单（扩展版）")
    public ApiResponse<Page<PurchaseExchange>> page(
            @Parameter(description = "查询参数") @RequestParam Map<String, Object> params,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(purchaseExchangeService.pageListExtended(params, pageNum, pageSize));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一换货单号")
    public ApiResponse<String> nextNo() {
        return ApiResponse.ok(purchaseExchangeService.generateExchangeNo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取换货单详情")
    public ApiResponse<PurchaseExchange> getById(@PathVariable Long id) {
        PurchaseExchange exchange = purchaseExchangeService.getById(id);
        if (exchange != null) {
            exchange.setItems(purchaseExchangeService.getItems(id));
        }
        return ApiResponse.ok(exchange);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取换货单明细")
    public ApiResponse<List<PurchaseExchangeItem>> getItems(@PathVariable Long id) {
        return ApiResponse.ok(purchaseExchangeService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建换货单")
    public ApiResponse<PurchaseExchange> create(@RequestBody PurchaseExchange exchange) {
        return ApiResponse.ok(purchaseExchangeService.createExchange(exchange, exchange.getItems()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新换货单")
    public ApiResponse<PurchaseExchange> update(@PathVariable Long id, @RequestBody PurchaseExchange exchange) {
        return ApiResponse.ok(purchaseExchangeService.updateExchange(id, exchange, exchange.getItems()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除换货单")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseExchangeService.removeById(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除换货单")
    public ApiResponse<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return ApiResponse.ok(purchaseExchangeService.removeBatchByIds(ids));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public ApiResponse<PurchaseExchange> submit(@PathVariable Long id) {
        return ApiResponse.ok(purchaseExchangeService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批换货单")
    public ApiResponse<PurchaseExchange> approve(
            @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String remark,
            @Parameter(description = "审批人姓名") @RequestParam(required = false) String approvedByName) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(purchaseExchangeService.approve(id, approverId, approvedByName, remark));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝换货单")
    public ApiResponse<PurchaseExchange> reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam(required = false) String remark) {
        return ApiResponse.ok(purchaseExchangeService.reject(id, remark == null ? "人工驳回" : remark));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换货单")
    public ApiResponse<PurchaseExchange> cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return ApiResponse.ok(purchaseExchangeService.cancel(id, reason));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换货单")
    public ApiResponse<PurchaseExchange> complete(@PathVariable Long id) {
        return ApiResponse.ok(purchaseExchangeService.complete(id));
    }

    @PostMapping("/{id}/print")
    @Operation(summary = "打印（打印次数+1）")
    public ApiResponse<Void> print(@PathVariable Long id) {
        purchaseExchangeService.print(id);
        return ApiResponse.ok();
    }

    @PostMapping("/batch-print")
    @Operation(summary = "批量打印")
    public ApiResponse<Void> batchPrint(@RequestBody(required = false) Map<String, Object> body) {
        List<Long> ids = new java.util.ArrayList<>();
        if (body != null) {
            Object idsObj = body.get("ids");
            if (idsObj instanceof List) {
                for (Object o : (List<?>) idsObj) {
                    if (o != null) ids.add(Long.valueOf(o.toString()));
                }
            }
        }
        purchaseExchangeService.batchPrint(ids);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/approval-records")
    @Operation(summary = "获取审批记录")
    public ApiResponse<List<ExchangeApprovalRecord>> getApprovalRecords(@PathVariable Long id) {
        return ApiResponse.ok(purchaseExchangeService.getApprovalRecords(id));
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "获取换货单跟踪信息")
    public ApiResponse<Map<String, Object>> getTracking(@PathVariable Long id) {
        return ApiResponse.ok(purchaseExchangeService.getTracking(id));
    }

    @GetMapping("/export")
    @Operation(summary = "导出换货单列表")
    public ApiResponse<List<PurchaseExchange>> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "换货类型") @RequestParam(required = false) Integer exchangeType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(purchaseExchangeService.exportList(keyword, supplierId, status, exchangeType, startDate, endDate));
    }
}
