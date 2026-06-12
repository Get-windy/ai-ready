package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeApprovalRecordVO;
import cn.aiedge.erp.expense.dto.FeeApprovalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 费用审批占位 Controller
 * 数据库表尚未创建，返回空数据避免前端报错
 */
@Tag(name = "费用审批管理（占位）")
@RestController
@RequestMapping("/api/erp/expense/approval")
public class FeeApprovalController {

    @Operation(summary = "执行审批操作（占位）")
    @PostMapping("/process")
    public ApiResponse<Void> processApproval(@Valid @RequestBody FeeApprovalRequest request) {
        return ApiResponse.success();
    }

    @Operation(summary = "获取审批记录（占位）")
    @GetMapping("/records")
    public ApiResponse<List<FeeApprovalRecordVO>> getApprovalRecords(
            @RequestParam String businessType,
            @RequestParam Long businessId) {
        return ApiResponse.ok(Collections.emptyList());
    }

    @Operation(summary = "获取待审批列表（占位）")
    @GetMapping("/pending")
    public ApiResponse<PageResult<FeeApprovalRecordVO>> pendingApprovals(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, pageNum, pageSize));
    }
}
