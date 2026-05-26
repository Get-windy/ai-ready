package cn.aiedge.workflow.controller;

import cn.aiedge.workflow.model.ApprovalRecord;
import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;
import cn.aiedge.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "审批流程", description = "审批流程发起、审批、查询功能")
public class WorkflowController {

    private final WorkflowService workflowService;

    // ==================== 流程定义 ====================

    @GetMapping("/definitions")
    @Operation(summary = "获取流程定义列表")
    public ResponseEntity<Map<String, Object>> getWorkflowDefinitions(
            @Parameter(description = "流程类型") @RequestParam(required = false) String type,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<WorkflowDefinition> definitions = workflowService.getWorkflowDefinitions(type, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("definitions", definitions);
        result.put("total", definitions.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/definitions/{definitionId}")
    @Operation(summary = "获取流程定义详情")
    public ResponseEntity<WorkflowDefinition> getWorkflowDefinition(
            @PathVariable String definitionId) {
        
        WorkflowDefinition definition = workflowService.getWorkflowDefinition(definitionId);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(definition);
    }

    @PostMapping("/definitions")
    @Operation(summary = "创建流程定义")
    public ResponseEntity<WorkflowDefinition> createWorkflowDefinition(
            @RequestBody WorkflowDefinition definition,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        WorkflowDefinition saved = workflowService.saveWorkflowDefinition(definition, tenantId);
        return ResponseEntity.ok(saved);
    }

    // ==================== 流程实例 ====================

    @PostMapping("/start")
    @Operation(summary = "发起流程")
    public ResponseEntity<Map<String, Object>> startWorkflow(
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
                userId,
                tenantId
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("instanceId", instance.getInstanceId());
        result.put("status", instance.getStatus());
        result.put("message", "流程发起成功");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "获取流程实例详情")
    public ResponseEntity<WorkflowInstance> getWorkflowInstance(@PathVariable String instanceId) {
        WorkflowInstance instance = workflowService.getWorkflowInstance(instanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/instances/{instanceId}/status")
    @Operation(summary = "获取流程状态")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(@PathVariable String instanceId) {
        Map<String, Object> status = workflowService.getWorkflowStatus(instanceId);
        return ResponseEntity.ok(status);
    }

    // ==================== 待办/已办 ====================

    @GetMapping("/pending")
    @Operation(summary = "获取我的待办")
    public ResponseEntity<Map<String, Object>> getMyPendingApprovals(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<WorkflowInstance> instances = workflowService.getMyPendingApprovals(userId, page, pageSize, tenantId);
        int count = workflowService.getPendingCount(userId, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("instances", instances);
        result.put("total", count);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/approved")
    @Operation(summary = "获取我的已办")
    public ResponseEntity<Map<String, Object>> getMyApproved(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<WorkflowInstance> instances = workflowService.getMyApproved(userId, page, pageSize, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("instances", instances);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-applications")
    @Operation(summary = "获取我发起的流程")
    public ResponseEntity<Map<String, Object>> getMyApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<WorkflowInstance> instances = workflowService.getMyApplications(userId, page, pageSize, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("instances", instances);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending/count")
    @Operation(summary = "获取待办数量")
    public ResponseEntity<Map<String, Object>> getPendingCount(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        int count = workflowService.getPendingCount(userId, tenantId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ==================== 审批操作 ====================

    @PostMapping("/{instanceId}/approve")
    @Operation(summary = "审批通过")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        log.info("审批通过: instanceId={}, userId={}", instanceId, userId);
        
        String comment = request != null ? request.getComment() : null;
        boolean success = workflowService.approve(instanceId, userId, comment);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "审批成功" : "审批失败");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{instanceId}/reject")
    @Operation(summary = "审批拒绝")
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        log.info("审批拒绝: instanceId={}, userId={}", instanceId, userId);
        
        String comment = request != null ? request.getComment() : null;
        boolean success = workflowService.reject(instanceId, userId, comment);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "已拒绝" : "操作失败");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{instanceId}/transfer")
    @Operation(summary = "转交他人")
    public ResponseEntity<Map<String, Object>> transfer(
            @PathVariable String instanceId,
            @RequestBody TransferRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        log.info("转交审批: instanceId={}, from={}, to={}", instanceId, userId, request.getToUserId());
        
        boolean success = workflowService.transfer(instanceId, userId, request.getToUserId(), request.getComment());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "转交成功" : "转交失败");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{instanceId}/withdraw")
    @Operation(summary = "撤回流程")
    public ResponseEntity<Map<String, Object>> withdraw(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        log.info("撤回流程: instanceId={}, userId={}", instanceId, userId);
        
        String reason = request != null ? request.getComment() : null;
        boolean success = workflowService.withdraw(instanceId, userId, reason);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "撤回成功" : "撤回失败");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{instanceId}/cancel")
    @Operation(summary = "取消流程")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable String instanceId,
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        log.info("取消流程: instanceId={}, userId={}", instanceId, userId);
        
        String reason = request != null ? request.getComment() : null;
        boolean success = workflowService.cancelWorkflow(instanceId, userId, reason);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "取消成功" : "取消失败");
        return ResponseEntity.ok(result);
    }

    // ==================== 审批记录 ====================

    @GetMapping("/{instanceId}/records")
    @Operation(summary = "获取审批记录")
    public ResponseEntity<List<ApprovalRecord>> getApprovalRecords(@PathVariable String instanceId) {
        List<ApprovalRecord> records = workflowService.getApprovalRecords(instanceId);
        return ResponseEntity.ok(records);
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
}
