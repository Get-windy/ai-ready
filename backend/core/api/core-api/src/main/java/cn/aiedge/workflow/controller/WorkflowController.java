package cn.aiedge.workflow.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.workflow.facade.ApprovalCallbackDispatcher;
import cn.aiedge.workflow.model.ApprovalRecord;
import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;
import cn.aiedge.workflow.service.WorkflowService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批流程控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "审批流程", description = "审批流程发起、审批、查询功能")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final ApprovalCallbackDispatcher callbackDispatcher;

    /**
     * 当前用户ID：优先取 X-User-Id 头，缺省时回退 Sa-Token 登录会话
     * （前端不发送 X-User-Id 头，仅靠 Sa-Token 鉴权）
     */
    private static Long resolveUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 流程定义 ====================

    @GetMapping("/definitions")
    @Operation(summary = "获取流程定义列表")
    public ApiResponse<Map<String, Object>> getWorkflowDefinitions(
            @Parameter(description = "流程类型") @RequestParam(required = false) String type,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<WorkflowDefinition> definitions = workflowService.getWorkflowDefinitions(type, tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("definitions", definitions);
        result.put("total", definitions.size());
        return ApiResponse.ok(result);
    }

    @GetMapping("/definitions/{definitionId}")
    @Operation(summary = "获取流程定义详情")
    public ApiResponse<WorkflowDefinition> getWorkflowDefinition(
            @PathVariable String definitionId) {

        WorkflowDefinition definition = workflowService.getWorkflowDefinition(definitionId);
        if (definition == null) {
            return ApiResponse.error(404, "流程定义不存在: " + definitionId);
        }
        return ApiResponse.ok(definition);
    }

    @PostMapping("/definitions")
    @Operation(summary = "创建流程定义")
    public ApiResponse<WorkflowDefinition> createWorkflowDefinition(
            @RequestBody WorkflowDefinition definition,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        WorkflowDefinition saved = workflowService.saveWorkflowDefinition(definition, tenantId);
        return ApiResponse.ok("创建成功", saved);
    }

    @PutMapping("/definitions/{definitionId}")
    @Operation(summary = "更新流程定义（含节点，版本+1）")
    public ApiResponse<WorkflowDefinition> updateWorkflowDefinition(
            @PathVariable String definitionId,
            @RequestBody WorkflowDefinition definition,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        if (workflowService.getWorkflowDefinition(definitionId) == null) {
            return ApiResponse.error(404, "流程定义不存在: " + definitionId);
        }
        WorkflowDefinition saved = workflowService.updateWorkflowDefinition(definitionId, definition, tenantId);
        return ApiResponse.ok("更新成功", saved);
    }

    @PostMapping("/definitions/{definitionId}/publish")
    @Operation(summary = "发布流程定义")
    public ApiResponse<Map<String, Object>> publishWorkflowDefinition(
            @PathVariable String definitionId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        boolean success = workflowService.publishWorkflowDefinition(definitionId, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "发布成功" : "流程定义不存在");
        return success ? ApiResponse.ok(result) : ApiResponse.error(404, "流程定义不存在: " + definitionId);
    }

    @PostMapping("/definitions/{definitionId}/disable")
    @Operation(summary = "停用流程定义")
    public ApiResponse<Map<String, Object>> disableWorkflowDefinition(
            @PathVariable String definitionId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        boolean success = workflowService.disableWorkflowDefinition(definitionId, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "停用成功" : "流程定义不存在");
        return success ? ApiResponse.ok(result) : ApiResponse.error(404, "流程定义不存在: " + definitionId);
    }

    @DeleteMapping("/definitions/{definitionId}")
    @Operation(summary = "删除流程定义（存在实例引用时禁止删除）")
    public ApiResponse<Map<String, Object>> deleteWorkflowDefinition(
            @PathVariable String definitionId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        if (workflowService.getWorkflowDefinition(definitionId) == null) {
            return ApiResponse.error(404, "流程定义不存在: " + definitionId);
        }
        long instanceCount = workflowService.countDefinitionInstances(definitionId);
        if (instanceCount > 0) {
            return ApiResponse.error(400, "流程定义存在 " + instanceCount + " 个实例引用，禁止删除");
        }
        boolean success = workflowService.deleteWorkflowDefinition(definitionId, tenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return ApiResponse.ok(result);
    }

    // ==================== 流程实例 ====================

    @PostMapping("/start")
    @Operation(summary = "发起流程")
    public ApiResponse<Map<String, Object>> startWorkflow(
            @RequestBody StartWorkflowRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        log.info("发起流程: definitionId={}, businessType={}",
                request.getDefinitionId(), request.getBusinessType());

        WorkflowInstance instance = workflowService.startWorkflow(
                request.getDefinitionId(),
                request.getBusinessType(),
                request.getBusinessId(),
                request.getBusinessData(),
                resolveUserId(userId),
                tenantId
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("instanceId", instance.getInstanceId());
        result.put("status", instance.getStatus());
        result.put("message", "流程发起成功");
        return ApiResponse.ok(result);
    }

    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "获取流程实例详情")
    public ApiResponse<WorkflowInstance> getWorkflowInstance(@PathVariable String instanceId) {
        WorkflowInstance instance = workflowService.getWorkflowInstance(instanceId);
        if (instance == null) {
            return ApiResponse.error(404, "流程实例不存在: " + instanceId);
        }
        return ApiResponse.ok(instance);
    }

    @GetMapping("/instances/{instanceId}/status")
    @Operation(summary = "获取流程状态")
    public ApiResponse<Map<String, Object>> getWorkflowStatus(@PathVariable String instanceId) {
        Map<String, Object> status = workflowService.getWorkflowStatus(instanceId);
        return ApiResponse.ok(status);
    }

    @GetMapping("/instance/page")
    @Operation(summary = "分页查询流程实例")
    public ApiResponse<Map<String, Object>> pageInstances(
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        Map<String, Object> result = workflowService.pageInstances(pageNum, pageSize, processName, status, tenantId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/instance/detail")
    @Operation(summary = "获取流程实例详情（监控页）")
    public ApiResponse<Map<String, Object>> getInstanceDetail(
            @Parameter(description = "流程实例ID") @RequestParam(required = false) String instanceId,
            @Parameter(description = "流程实例ID（兼容参数）") @RequestParam(required = false) String id) {

        String effectiveId = instanceId != null ? instanceId : id;
        Map<String, Object> detail = workflowService.getInstanceDetail(effectiveId);
        if (detail == null) {
            return ApiResponse.error(404, "流程实例不存在: " + effectiveId);
        }
        return ApiResponse.ok(detail);
    }

    // ==================== 待办/已办 ====================

    @GetMapping("/pending")
    @Operation(summary = "获取我的待办")
    public ApiResponse<Map<String, Object>> getMyPendingApprovals(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<WorkflowInstance> instances = workflowService.getMyPendingApprovals(resolveUserId(userId), page, pageSize, tenantId);
        int count = workflowService.getPendingCount(resolveUserId(userId), tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("instances", instances);
        result.put("total", count);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.ok(result);
    }

    @GetMapping("/approved")
    @Operation(summary = "获取我的已办")
    public ApiResponse<Map<String, Object>> getMyApproved(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<WorkflowInstance> instances = workflowService.getMyApproved(resolveUserId(userId), page, pageSize, tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("instances", instances);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.ok(result);
    }

    @GetMapping("/my-applications")
    @Operation(summary = "获取我发起的流程")
    public ApiResponse<Map<String, Object>> getMyApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<WorkflowInstance> instances = workflowService.getMyApplications(resolveUserId(userId), page, pageSize, tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("instances", instances);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.ok(result);
    }

    @GetMapping("/pending/count")
    @Operation(summary = "获取待办数量")
    public ApiResponse<Map<String, Object>> getPendingCount(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        int count = workflowService.getPendingCount(resolveUserId(userId), tenantId);
        return ApiResponse.ok(Map.of("count", count));
    }

    @GetMapping("/task/page")
    @Operation(summary = "分页查询任务（待办/已办）")
    public ApiResponse<Map<String, Object>> pageTasks(
            @Parameter(description = "标签页: todo/done") @RequestParam(defaultValue = "todo") String tab,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        Map<String, Object> result = workflowService.pageTasks(tab, resolveUserId(userId), pageNum, pageSize, tenantId, taskName, processName);
        return ApiResponse.ok(result);
    }

    @GetMapping("/task/detail")
    @Operation(summary = "获取任务详情")
    public ApiResponse<Map<String, Object>> getTaskDetail(
            @Parameter(description = "任务ID") @RequestParam(required = false) String taskId,
            @Parameter(description = "任务ID（兼容参数）") @RequestParam(required = false) String id) {

        String effectiveId = taskId != null ? taskId : id;
        Map<String, Object> detail = workflowService.getTaskDetail(effectiveId);
        if (detail == null) {
            return ApiResponse.error(404, "任务不存在: " + effectiveId);
        }
        return ApiResponse.ok(detail);
    }

    @PostMapping("/task/approve")
    @Operation(summary = "按任务审批（approve/reject/return）")
    public ApiResponse<Map<String, Object>> approveTask(
            @RequestBody TaskApproveRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        // action 与 approval 同义（前端 task-management 传 approval）
        String action = request.getAction() != null ? request.getAction() : request.getApproval();
        log.info("任务审批: taskId={}, action={}, userId={}", request.getTaskId(), action, userId);

        boolean success = workflowService.approveTask(request.getTaskId(), resolveUserId(userId), action,
                request.getComment(), request.getReturnNode());

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "审批成功" : "审批失败（任务不存在或已处理）");
        return ApiResponse.ok(result);
    }

    @PostMapping("/task/transfer")
    @Operation(summary = "按任务转交他人")
    public ApiResponse<Map<String, Object>> transferTask(
            @RequestBody TaskTransferRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        // targetUserId 与 targetUser 同义（前端 task-management 传 targetUser）
        Long targetUserId = request.resolveTargetUserId();
        log.info("任务转交: taskId={}, targetUserId={}, userId={}", request.getTaskId(), targetUserId, userId);

        if (targetUserId == null) {
            return ApiResponse.error(400, "目标用户不能为空");
        }
        boolean success = workflowService.transferTask(request.getTaskId(), resolveUserId(userId), targetUserId, request.getComment());

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "转交成功" : "转交失败（任务不存在或已处理）");
        return ApiResponse.ok(result);
    }

    // ==================== 流程分析 ====================

    @GetMapping("/analysis/refresh")
    @Operation(summary = "流程分析汇总（真实数据聚合）")
    public ApiResponse<Map<String, Object>> getAnalysisSummary(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        return ApiResponse.ok(workflowService.getAnalysisSummary(tenantId));
    }

    @GetMapping("/analysis/report")
    @Operation(summary = "审批效率报表（按日聚合）")
    public ApiResponse<Map<String, Object>> getAnalysisReport(
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam(required = false) String endDate,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        return ApiResponse.ok(workflowService.getAnalysisReport(startDate, endDate, tenantId));
    }

    // ==================== 审批操作 ====================

    @PostMapping("/{instanceId}/approve")
    @Operation(summary = "审批通过")
    public ApiResponse<Map<String, Object>> approve(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("审批通过: instanceId={}, userId={}", instanceId, userId);

        String comment = request != null ? request.getComment() : null;
        boolean success = workflowService.approve(instanceId, resolveUserId(userId), comment);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "审批成功" : "审批失败");
        return ApiResponse.ok(result);
    }

    @PostMapping("/{instanceId}/reject")
    @Operation(summary = "审批拒绝")
    public ApiResponse<Map<String, Object>> reject(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("审批拒绝: instanceId={}, userId={}", instanceId, userId);

        String comment = request != null ? request.getComment() : null;
        boolean success = workflowService.reject(instanceId, resolveUserId(userId), comment);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "已拒绝" : "操作失败");
        return ApiResponse.ok(result);
    }

    @PostMapping("/{instanceId}/transfer")
    @Operation(summary = "转交他人")
    public ApiResponse<Map<String, Object>> transfer(
            @PathVariable String instanceId,
            @RequestBody TransferRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("转交审批: instanceId={}, from={}, to={}", instanceId, userId, request.getToUserId());

        boolean success = workflowService.transfer(instanceId, resolveUserId(userId), request.getToUserId(), request.getComment());

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "转交成功" : "转交失败");
        return ApiResponse.ok(result);
    }

    @PostMapping("/{instanceId}/withdraw")
    @Operation(summary = "撤回流程")
    public ApiResponse<Map<String, Object>> withdraw(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("撤回流程: instanceId={}, userId={}", instanceId, userId);

        String reason = request != null ? request.getComment() : null;
        boolean success = workflowService.withdraw(instanceId, resolveUserId(userId), reason);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "撤回成功" : "撤回失败");
        return ApiResponse.ok(result);
    }

    @PostMapping("/{instanceId}/cancel")
    @Operation(summary = "取消流程")
    public ApiResponse<Map<String, Object>> cancel(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("取消流程: instanceId={}, userId={}", instanceId, userId);

        String reason = request != null ? request.getComment() : null;
        boolean success = workflowService.cancelWorkflow(instanceId, resolveUserId(userId), reason);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "取消成功" : "取消失败");
        return ApiResponse.ok(result);
    }

    // ==================== 审批记录 ====================

    @GetMapping("/{instanceId}/records")
    @Operation(summary = "获取审批记录")
    public ApiResponse<List<ApprovalRecord>> getApprovalRecords(@PathVariable String instanceId) {
        List<ApprovalRecord> records = workflowService.getApprovalRecords(instanceId);
        return ApiResponse.ok(records);
    }

    // ==================== 流程监控 - 流程图 ====================

    @GetMapping("/instance/diagram")
    @Operation(summary = "获取流程图（SVG）")
    public ApiResponse<Map<String, Object>> getInstanceDiagram(
            @Parameter(description = "流程实例ID") @RequestParam String instanceId) {

        Map<String, Object> diagram = workflowService.getInstanceDiagram(instanceId);
        return ApiResponse.ok(diagram);
    }

    // ==================== 流程监控 - 流程干预 ====================

    @PostMapping("/instance/{instanceId}/intervene")
    @Operation(summary = "流程干预（终止/挂起/恢复）")
    public ApiResponse<Map<String, Object>> interveneInstance(
            @PathVariable String instanceId,
            @RequestBody InterveneRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        log.info("流程干预: instanceId={}, action={}, userId={}", instanceId, request.getAction(), userId);

        boolean success = workflowService.interveneInstance(instanceId, request.getAction(), resolveUserId(userId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "操作成功" : "操作失败");
        return ApiResponse.ok(result);
    }

    // ==================== 回调补偿（第三期） ====================

    @GetMapping("/callback-log/page")
    @Operation(summary = "分页查询审批回调补偿日志")
    public ApiResponse<Map<String, Object>> pageCallbackLogs(
            @Parameter(description = "补偿状态: pending/success/failed/final-failed") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {

        return ApiResponse.ok(callbackDispatcher.pageLogs(status, pageNum, pageSize));
    }

    @PostMapping("/callback-log/{id}/retry")
    @Operation(summary = "人工重试回调（重置重试周期并立即分发）")
    public ApiResponse<Map<String, Object>> retryCallbackLog(@PathVariable Long id) {

        log.info("人工重试审批回调: logId={}", id);
        return ApiResponse.ok(callbackDispatcher.manualRetry(id));
    }

    // ==================== 请求DTO ====================

    public static class StartWorkflowRequest {
        private String definitionId;
        private String businessType;
        private String businessId;
        private Map<String, Object> businessData;

        // Getters and Setters
        public String getDefinitionId() { return definitionId; }
        public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public String getBusinessId() { return businessId; }
        public void setBusinessId(String businessId) { this.businessId = businessId; }
        public Map<String, Object> getBusinessData() { return businessData; }
        public void setBusinessData(Map<String, Object> businessData) { this.businessData = businessData; }
    }

    public static class ApprovalRequest {
        private String comment;

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class TransferRequest {
        private Long toUserId;
        private String comment;

        public Long getToUserId() { return toUserId; }
        public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class InterveneRequest {
        private String action;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class TaskApproveRequest {
        private String taskId;
        /** 审批动作: approve/reject/return（与 approval 同义，优先取 action） */
        private String action;
        private String approval;
        private String comment;
        private String returnNode;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getApproval() { return approval; }
        public void setApproval(String approval) { this.approval = approval; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getReturnNode() { return returnNode; }
        public void setReturnNode(String returnNode) { this.returnNode = returnNode; }
    }

    public static class TaskTransferRequest {
        private String taskId;
        private Long targetUserId;
        /** 目标用户（前端 task-management 传 targetUser，为用户ID） */
        private String targetUser;
        private String type;
        private String comment;

        public Long resolveTargetUserId() {
            if (targetUserId != null) {
                return targetUserId;
            }
            if (targetUser != null && !targetUser.isBlank()) {
                try {
                    return Long.parseLong(targetUser.trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public Long getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
        public String getTargetUser() { return targetUser; }
        public void setTargetUser(String targetUser) { this.targetUser = targetUser; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
