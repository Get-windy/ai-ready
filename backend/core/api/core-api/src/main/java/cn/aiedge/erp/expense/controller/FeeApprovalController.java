package cn.aiedge.erp.expense.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeApprovalRecordVO;
import cn.aiedge.erp.expense.dto.FeeApprovalRequest;
import cn.aiedge.erp.expense.service.FeeApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 费用审批控制器
 */
@Tag(name = "费用审批管理", description = "费用申请/报销的审批操作接口")
@RestController
@RequestMapping("/api/erp/expense/approval")
@RequiredArgsConstructor
public class FeeApprovalController {

    private final FeeApprovalService feeApprovalService;

    @Operation(summary = "执行审批操作")
    @PostMapping("/process")
    @RequiresPermission("erp:expense:approval:process")
    public ApiResponse<Void> processApproval(@Valid @RequestBody FeeApprovalRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        feeApprovalService.processApproval(request, operatorId, operatorName);
        return ApiResponse.success();
    }

    @Operation(summary = "获取审批记录")
    @GetMapping("/records")
    @RequiresPermission("erp:expense:approval:query")
    public ApiResponse<List<FeeApprovalRecordVO>> getApprovalRecords(
            @RequestParam String businessType,
            @RequestParam Long businessId) {
        return ApiResponse.ok(feeApprovalService.getApprovalRecords(businessType, businessId));
    }

    @Operation(summary = "获取待审批列表")
    @GetMapping("/pending")
    @RequiresPermission("erp:expense:approval:list")
    public ApiResponse<PageResult<FeeApprovalRecordVO>> pendingApprovals(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long approverId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(feeApprovalService.getPendingApprovals(approverId, pageNum, pageSize));
    }
}
