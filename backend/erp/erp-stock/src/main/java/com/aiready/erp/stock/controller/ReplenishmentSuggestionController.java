package com.aiready.erp.stock.controller;

import com.aiready.erp.stock.entity.ReplenishmentSuggestion;
import com.aiready.erp.stock.entity.ReplenishmentReport;
import com.aiready.erp.stock.service.ReplenishmentSuggestionService;
import com.aiready.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "智能补货建议")
@RestController
@RequestMapping("/api/erp/stock/replenishment")
@RequiredArgsConstructor
public class ReplenishmentSuggestionController {

    private final ReplenishmentSuggestionService suggestionService;

    @Operation(summary = "生成补货建议")
    @GetMapping("/generate/{warehouseId}")
    public R<List<ReplenishmentSuggestion>> generateSuggestions(@PathVariable Long warehouseId) {
        return R.ok(suggestionService.generateSuggestions(warehouseId));
    }

    @Operation(summary = "获取补货报告")
    @GetMapping("/report/{warehouseId}")
    public R<ReplenishmentReport> getReport(@PathVariable Long warehouseId) {
        return R.ok(suggestionService.generateReport(warehouseId));
    }

    @Operation(summary = "获取待处理建议列表")
    @GetMapping("/pending")
    public R<List<ReplenishmentSuggestion>> getPendingSuggestions() {
        return R.ok(suggestionService.getSuggestionsByStatus("pending"));
    }

    @Operation(summary = "获取建议详情")
    @GetMapping("/{id}")
    public R<ReplenishmentSuggestion> getSuggestion(@PathVariable Long id) {
        return R.ok(suggestionService.getSuggestionById(id));
    }

    @Operation(summary = "标记建议已处理")
    @PutMapping("/{id}/process/{purchaseOrderId}")
    public R<Void> markProcessed(@PathVariable Long id, @PathVariable Long purchaseOrderId) {
        suggestionService.markSuggestionAsProcessed(id, purchaseOrderId);
        return R.ok();
    }

    @Operation(summary = "忽略建议")
    @PutMapping("/{id}/ignore")
    public R<Void> ignoreSuggestion(@PathVariable Long id, @RequestParam String reason) {
        suggestionService.ignoreSuggestion(id, reason);
        return R.ok();
    }

    @Operation(summary = "根据建议创建采购订单")
    @PostMapping("/create-purchase-order")
    public R<Long> createPurchaseOrder(@RequestBody List<Long> suggestionIds, @RequestParam Long supplierId) {
        return R.ok(suggestionService.createPurchaseOrderFromSuggestions(suggestionIds, supplierId).getId());
    }
}