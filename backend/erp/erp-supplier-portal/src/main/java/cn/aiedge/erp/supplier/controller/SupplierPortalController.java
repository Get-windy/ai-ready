package cn.aiedge.erp.supplier.controller;

import cn.aiedge.erp.supplier.model.entity.*;
import cn.aiedge.erp.supplier.service.SupplierPortalService;
import cn.aiedge.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/supplier-portal")
@Tag(name = "供应商门户", description = "供应商等级、绩效、询价报价、积分管理接口")
@RequiredArgsConstructor
public class SupplierPortalController {
    
    private final SupplierPortalService supplierPortalService;
    
    @GetMapping("/levels")
    @Operation(summary = "获取活跃等级列表", description = "获取所有活跃的供应商等级")
    public R<List<SupplierLevelEntity>> getActiveLevels() {
        return R.ok(supplierPortalService.getActiveLevels());
    }
    
    @GetMapping("/levels/score/{score}")
    @Operation(summary = "根据分数获取等级", description = "根据绩效分数获取对应的供应商等级")
    public R<SupplierLevelEntity> getLevelByScore(@PathVariable Double score) {
        return R.ok(supplierPortalService.getLevelByScore(score));
    }
    
    @PostMapping("/levels")
    @Operation(summary = "创建供应商等级", description = "创建新的供应商等级")
    public R<SupplierLevelEntity> createLevel(@RequestBody SupplierLevelEntity level) {
        return R.ok(supplierPortalService.createLevel(level));
    }
    
    @PutMapping("/levels/{id}")
    @Operation(summary = "更新供应商等级", description = "更新供应商等级信息")
    public R<SupplierLevelEntity> updateLevel(@PathVariable Long id, @RequestBody SupplierLevelEntity level) {
        return R.ok(supplierPortalService.updateLevel(id, level));
    }
    
    @DeleteMapping("/levels/{id}")
    @Operation(summary = "删除供应商等级", description = "删除供应商等级")
    public R<Void> deleteLevel(@PathVariable Long id) {
        supplierPortalService.deleteLevel(id);
        return R.ok();
    }
    
    @PostMapping("/performances")
    @Operation(summary = "创建绩效评估", description = "创建供应商绩效评估记录")
    public R<SupplierPerformanceEntity> createPerformance(@RequestBody SupplierPerformanceEntity performance) {
        return R.ok(supplierPortalService.createPerformance(performance));
    }
    
    @GetMapping("/performances/{id}")
    @Operation(summary = "获取绩效评估", description = "获取单个绩效评估详情")
    public R<SupplierPerformanceEntity> getPerformance(@PathVariable Long id) {
        return R.ok(supplierPortalService.getPerformance(id));
    }
    
    @GetMapping("/performances/supplier/{supplierId}")
    @Operation(summary = "获取供应商绩效列表", description = "获取供应商的所有绩效评估记录")
    public R<List<SupplierPerformanceEntity>> getSupplierPerformances(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getSupplierPerformances(supplierId));
    }
    
    @GetMapping("/performances/supplier/{supplierId}/latest/{period}")
    @Operation(summary = "获取最新绩效", description = "获取供应商指定周期的最新绩效评估")
    public R<SupplierPerformanceEntity> getLatestPerformance(@PathVariable Long supplierId, @PathVariable String period) {
        return R.ok(supplierPortalService.getLatestPerformance(supplierId, period));
    }
    
    @GetMapping("/performances/supplier/{supplierId}/average")
    @Operation(summary = "获取平均绩效分数", description = "获取供应商的平均绩效分数")
    public R<Double> getAveragePerformanceScore(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getAveragePerformanceScore(supplierId));
    }
    
    @GetMapping("/performances/supplier/{supplierId}/summary")
    @Operation(summary = "获取绩效汇总", description = "获取供应商绩效汇总信息")
    public R<Map<String, Object>> getPerformanceSummary(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.calculatePerformanceSummary(supplierId));
    }
    
    @PostMapping("/inquiries")
    @Operation(summary = "创建询价单", description = "创建新的询价单")
    public R<InquiryQuotationEntity> createInquiry(@RequestBody InquiryQuotationEntity inquiry) {
        return R.ok(supplierPortalService.createInquiry(inquiry));
    }
    
    @GetMapping("/inquiries/{id}")
    @Operation(summary = "获取询价单", description = "获取询价单详情")
    public R<InquiryQuotationEntity> getInquiry(@PathVariable Long id) {
        return R.ok(supplierPortalService.getInquiry(id));
    }
    
    @GetMapping("/inquiries/supplier/{supplierId}")
    @Operation(summary = "获取供应商询价单", description = "获取供应商的所有询价单")
    public R<List<InquiryQuotationEntity>> getSupplierInquiries(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getSupplierInquiries(supplierId));
    }
    
    @PostMapping("/inquiries/{inquiryId}/quotation")
    @Operation(summary = "提交报价", description = "供应商提交报价")
    public R<InquiryQuotationEntity> submitQuotation(@PathVariable Long inquiryId, @RequestBody InquiryQuotationEntity quotation) {
        return R.ok(supplierPortalService.submitQuotation(inquiryId, quotation));
    }
    
    @PostMapping("/quotations/{quotationId}/accept")
    @Operation(summary = "接受报价", description = "采购方接受报价")
    public R<InquiryQuotationEntity> acceptQuotation(@PathVariable Long quotationId) {
        return R.ok(supplierPortalService.acceptQuotation(quotationId));
    }
    
    @PostMapping("/quotations/{quotationId}/reject")
    @Operation(summary = "拒绝报价", description = "采购方拒绝报价")
    public R<InquiryQuotationEntity> rejectQuotation(@PathVariable Long quotationId, @RequestParam String reason) {
        return R.ok(supplierPortalService.rejectQuotation(quotationId, reason));
    }
    
    @GetMapping("/inquiries/pending")
    @Operation(summary = "获取待处理询价单", description = "获取所有待处理的询价单")
    public R<List<InquiryQuotationEntity>> getPendingInquiries() {
        return R.ok(supplierPortalService.getPendingInquiries());
    }
    
    @PostMapping("/points/{supplierId}/add")
    @Operation(summary = "增加积分", description = "为供应商增加积分")
    public R<SupplierPointsRecordEntity> addPoints(
            @PathVariable Long supplierId,
            @RequestParam Integer points,
            @RequestParam String reason,
            @RequestParam(required = false) String ruleId) {
        return R.ok(supplierPortalService.addPoints(supplierId, points, reason, ruleId));
    }
    
    @PostMapping("/points/{supplierId}/consume")
    @Operation(summary = "消费积分", description = "供应商消费积分")
    public R<SupplierPointsRecordEntity> consumePoints(
            @PathVariable Long supplierId,
            @RequestParam Integer points,
            @RequestParam String reason) {
        return R.ok(supplierPortalService.consumePoints(supplierId, points, reason));
    }
    
    @GetMapping("/points/{supplierId}/total")
    @Operation(summary = "获取总积分", description = "获取供应商的总积分")
    public R<Integer> getTotalPoints(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getTotalPoints(supplierId));
    }
    
    @GetMapping("/points/{supplierId}/records")
    @Operation(summary = "获取积分记录", description = "获取供应商的积分记录")
    public R<List<SupplierPointsRecordEntity>> getPointsRecords(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getPointsRecords(supplierId));
    }
    
    @GetMapping("/dashboard/{supplierId}")
    @Operation(summary = "获取供应商仪表盘", description = "获取供应商仪表盘数据")
    public R<Map<String, Object>> getSupplierDashboard(@PathVariable Long supplierId) {
        return R.ok(supplierPortalService.getSupplierDashboard(supplierId));
    }
}