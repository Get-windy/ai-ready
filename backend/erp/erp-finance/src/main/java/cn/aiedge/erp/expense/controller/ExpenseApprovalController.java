package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.expense.dto.ApiResponse;
import cn.aiedge.erp.expense.model.ExpenseApproval;
import cn.aiedge.erp.expense.model.ExpenseApplication;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.repository.ExpenseApplicationRepository;
import cn.aiedge.erp.expense.repository.ExpenseApprovalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/expense/approval")
@Tag(name = "费用审批管理", description = "费用审批处理、记录查询、待审批列表")
@RequiredArgsConstructor
public class ExpenseApprovalController {

    private final ExpenseApplicationRepository applicationRepository;
    private final ExpenseApprovalRepository approvalRepository;

    @Operation(summary = "处理审批", description = "审批通过或拒绝费用单")
    @PostMapping("/process")
    public ApiResponse<Map<String, Object>> processApproval(@RequestBody Map<String, Object> request) {
        Long applicationId = Long.valueOf(request.get("applicationId").toString());
        String action = (String) request.get("action"); // APPROVE or REJECT
        String comment = (String) request.get("comment");
        String approverId = (String) request.get("approverId");
        String approverName = (String) request.get("approverName");

        ExpenseApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> BusinessException.notFound("费用单不存在: " + applicationId));

        if (application.isDeleted()) {
            throw BusinessException.badRequest("费用单已被删除: " + applicationId);
        }
        if (!application.canApprove()) {
            throw BusinessException.badRequest("费用单状态不允许审批: " + application.getStatus().getDescription());
        }

        ExpenseStatus previousStatus = application.getStatus();

        ExpenseApproval approval = new ExpenseApproval();
        approval.setApplicationId(application.getApplicationCode());
        approval.setApprovalLevel(application.getCurrentApprovalLevel());
        approval.setApproverId(approverId != null ? approverId : "system");
        approval.setApproverName(approverName != null ? approverName : "系统");
        approval.setApprovalAction(action);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setPreviousStatus(previousStatus.name());

        if ("APPROVE".equals(action)) {
            int currentLevel = application.getCurrentApprovalLevel();
            int totalLevel = application.getTotalApprovalLevel();
            if (currentLevel >= totalLevel) {
                application.setStatus(ExpenseStatus.APPROVED);
                approval.setCurrentStatus(ExpenseStatus.APPROVED.name());
            } else {
                application.setCurrentApprovalLevel(currentLevel + 1);
                application.setStatus(ExpenseStatus.FINANCE_APPROVING);
                approval.setCurrentStatus(ExpenseStatus.FINANCE_APPROVING.name());
            }
        } else if ("REJECT".equals(action)) {
            application.setStatus(ExpenseStatus.REJECTED);
            application.setRejectReason(comment);
            approval.setCurrentStatus(ExpenseStatus.REJECTED.name());
        } else {
            throw BusinessException.badRequest("不支持的审批动作: " + action);
        }

        application.updateStatusDesc();
        applicationRepository.save(application);
        approvalRepository.save(approval);

        Map<String, Object> result = new HashMap<>();
        result.put("applicationId", application.getId());
        result.put("applicationCode", application.getApplicationCode());
        result.put("status", application.getStatus().name());
        result.put("statusDesc", application.getStatus().getDescription());
        result.put("action", action);

        log.info("审批处理成功: applicationId={}, action={}", applicationId, action);
        return ApiResponse.success("审批处理成功", result);
    }

    @Operation(summary = "审批记录", description = "获取费用单的审批记录")
    @SaCheckPermission("erp:expense:approval:list")
    @GetMapping("/records")
    public ApiResponse<List<ExpenseApproval>> getApprovalRecords(
            @Parameter(description = "费用申请ID") @RequestParam(required = false) Long applicationId,
            @Parameter(description = "费用申请编号") @RequestParam(required = false) String applicationCode,
            @Parameter(description = "审批人ID") @RequestParam(required = false) String approverId) {

        List<ExpenseApproval> records;
        if (applicationId != null) {
            ExpenseApplication app = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> BusinessException.notFound("费用单不存在: " + applicationId));
            records = approvalRepository.findByApplicationId(app.getApplicationCode());
        } else if (applicationCode != null && !applicationCode.isEmpty()) {
            records = approvalRepository.findByApplicationId(applicationCode);
        } else if (approverId != null && !approverId.isEmpty()) {
            records = approvalRepository.findByApproverId(approverId);
        } else {
            records = approvalRepository.findAll();
        }
        return ApiResponse.success(records);
    }

    @Operation(summary = "待审批列表", description = "获取当前待审批的费用单列表")
    @SaCheckPermission("erp:expense:approval:list")
    @GetMapping("/pending")
    public ApiResponse<List<ExpenseApplication>> getPendingApprovals(
            @Parameter(description = "审批人ID") @RequestParam(required = false) String approverId) {

        List<ExpenseStatus> pendingStatuses = List.of(
                ExpenseStatus.SUBMITTED,
                ExpenseStatus.DEPARTMENT_APPROVING,
                ExpenseStatus.FINANCE_APPROVING,
                ExpenseStatus.GENERAL_MANAGER_APPROVING
        );

        List<ExpenseApplication> pendingList;
        if (approverId != null && !approverId.isEmpty()) {
            pendingList = applicationRepository.findByCurrentApproverId(approverId);
            pendingList = pendingList.stream()
                    .filter(app -> pendingStatuses.contains(app.getStatus()) && !app.isDeleted())
                    .toList();
        } else {
            pendingList = applicationRepository.findByStatusIn(pendingStatuses);
            pendingList = pendingList.stream()
                    .filter(app -> !app.isDeleted())
                    .toList();
        }

        return ApiResponse.success(pendingList);
    }
}
