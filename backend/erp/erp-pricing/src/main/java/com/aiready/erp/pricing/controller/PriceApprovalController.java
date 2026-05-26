package com.aiready.erp.pricing.controller;

import com.aiready.erp.pricing.entity.PriceApproval;
import com.aiready.erp.pricing.service.PriceApprovalService;
import com.aiready.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "价格审批管理")
@RestController
@RequestMapping("/api/erp/pricing/approval")
@RequiredArgsConstructor
public class PriceApprovalController {

    private final PriceApprovalService approvalService;

    @Operation(summary = "申请价格变更")
    @PostMapping("/apply")
    public R<PriceApproval> applyPriceChange(@RequestBody PriceApprovalApplyDTO dto) {
        return R.ok(approvalService.applyPriceChange(dto));
    }

    @Operation(summary = "审批通过")
    @PutMapping("/{id}/approve")
    public R<PriceApproval> approve(@PathVariable Long id, @RequestParam Long approverId, @RequestParam(required = false) String remark) {
        return R.ok(approvalService.approvePriceChange(id, approverId, remark));
    }

    @Operation(summary = "审批拒绝")
    @PutMapping("/{id}/reject")
    public R<PriceApproval> reject(@PathVariable Long id, @RequestParam Long approverId, @RequestParam String remark) {
        return R.ok(approvalService.rejectPriceChange(id, approverId, remark));
    }

    @Operation(summary = "获取待审批列表")
    @GetMapping("/pending")
    public R<List<PriceApproval>> getPendingApprovals() {
        return R.ok(approvalService.getPendingApprovals());
    }

    @Operation(summary = "按状态获取审批列表")
    @GetMapping("/list/{status}")
    public R<List<PriceApproval>> getApprovalsByStatus(@PathVariable String status) {
        return R.ok(approvalService.getApprovalsByStatus(status));
    }

    @Operation(summary = "获取我的申请")
    @GetMapping("/my/{applicantId}")
    public R<List<PriceApproval>> getMyApprovals(@PathVariable Long applicantId) {
        return R.ok(approvalService.getApprovalsByApplicant(applicantId));
    }

    @Operation(summary = "获取审批详情")
    @GetMapping("/{id}")
    public R<PriceApproval> getById(@PathVariable Long id) {
        return R.ok(approvalService.getById(id));
    }

    @Operation(summary = "获取审批统计")
    @GetMapping("/statistics")
    public R<PriceApprovalStatistics> getStatistics() {
        return R.ok(approvalService.getStatistics());
    }
}