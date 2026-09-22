package cn.aiedge.workflow.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.workflow.facade.ApprovalCallbackDispatcher;
import cn.aiedge.workflow.model.ApprovalRecord;
import cn.aiedge.workflow.model.AuditRuleSaveRequest;
import cn.aiedge.workflow.model.WorkflowDefinition;
import cn.aiedge.workflow.model.WorkflowInstance;
import cn.aiedge.workflow.service.AuditRuleService;
import cn.aiedge.workflow.service.WorkflowService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
    private final AuditRuleService auditRuleService;
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

    // ==================== 流程定义（流程定义 801 / 流程设计 80610 两页共用，已补方法级权限码） ====================
    //
    // 权限码命名沿用本控制器既有口径（workflow:<域>:<动作>，见 audit-config 的 workflow:audit:*）：
    //   workflow:definition:list     列表 / 详情
    //   workflow:definition:save     新建 / 更新（保存即生成新版本）
    //   workflow:definition:publish  发布（启用，只改 status）
    //   workflow:definition:disable  停用（只改 status）
    //   workflow:definition:delete   删除（有实例引用 → 400）
    // 权限码种子见迁移 V11.403.0；前端 views/workflow/designer/index.vue 已同步加 v-permission 门控。
    //
    // ⚠️ publish / disable 是**只改 status、不动 version** 的独立启停端点：
    //    前端启停开关必须走这两个端点，**不要**再借道「POST /definitions 带 definitionId」
    //    的保存通道（那条路会 version +1 并把 process_config / workflow_node 整份重写）。

    @GetMapping("/definitions")
    @SaCheckPermission("workflow:definition:list")
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
    @SaCheckPermission("workflow:definition:list")
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
    @SaCheckPermission("workflow:definition:save")
    @Operation(summary = "创建流程定义")
    public ApiResponse<WorkflowDefinition> createWorkflowDefinition(
            @RequestBody WorkflowDefinition definition,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        WorkflowDefinition saved = workflowService.saveWorkflowDefinition(definition, tenantId);
        return ApiResponse.ok("创建成功", saved);
    }

    @PutMapping("/definitions/{definitionId}")
    @SaCheckPermission("workflow:definition:save")
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
    @SaCheckPermission("workflow:definition:publish")
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
    @SaCheckPermission("workflow:definition:disable")
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
    @SaCheckPermission("workflow:definition:delete")
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

    // ==================== 审核设置（设置 → 系统配置 → 审核设置，菜单 80622） ====================
    //
    // 本组是本页（views/set/audit-config/index.vue）**唯一**用到的端点，已补方法级权限码；
    // 权限码种子见迁移 V11.398.0（workflow:audit:list / workflow:audit:update）。
    //
    // ⚠️ 除「流程定义组」（workflow:definition:*，见上方 §流程定义）与本组外，
    //    本控制器其余端点**仍是仅类级 @SaCheckLogin**（无方法级权限码）——
    //    这是**刻意不扩面**：补服务端权限码必须连同该页前端 v-permission 门控一起做，
    //    否则会让非超管用户在那些页面上直接 403（跨页回归）。
    //    「流程实例 / 待办 / 已办」的权限码由各自批次连同前端门控一并落地。

    @GetMapping("/audit-config/list")
    @SaCheckPermission("workflow:audit:list")
    @Operation(summary = "审核设置：16 类单据的审核规则与摘要")
    public ApiResponse<Map<String, Object>> listAuditRules() {
        return ApiResponse.ok(auditRuleService.listAuditRules());
    }

    @PutMapping("/audit-config/{docType}")
    @SaCheckPermission("workflow:audit:update")
    @Operation(summary = "审核设置：保存指定单据类型的审核规则（全量覆盖）")
    public ApiResponse<Map<String, Object>> saveAuditRules(
            @PathVariable String docType,
            @RequestBody AuditRuleSaveRequest request) {

        Map<String, Object> saved = auditRuleService.saveAuditRules(docType, request);
        return ApiResponse.ok("保存成功", saved);
    }

    // ==================== 流程实例 ====================

    @SaCheckPermission("workflow:instance:start")
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

    @SaCheckPermission("workflow:instance:view")
    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "获取流程实例详情")
    public ApiResponse<WorkflowInstance> getWorkflowInstance(@PathVariable String instanceId) {
        WorkflowInstance instance = workflowService.getWorkflowInstance(instanceId);
        if (instance == null) {
            return ApiResponse.error(404, "流程实例不存在: " + instanceId);
        }
        return ApiResponse.ok(instance);
    }

    @SaCheckPermission("workflow:instance:view")
    @GetMapping("/instances/{instanceId}/status")
    @Operation(summary = "获取流程状态")
    public ApiResponse<Map<String, Object>> getWorkflowStatus(@PathVariable String instanceId) {
        Map<String, Object> status = workflowService.getWorkflowStatus(instanceId);
        return ApiResponse.ok(status);
    }

    @GetMapping("/instance/page")
    @SaCheckPermission("workflow:instance:view")
    @Operation(summary = "分页查询流程实例")
    public ApiResponse<Map<String, Object>> pageInstances(
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        // 修复：startDate / endDate 此前被接收但未透传给 service（时间区间筛选静默失效）
        Map<String, Object> result = workflowService.pageInstances(pageNum, pageSize, processName, status,
                startDate, endDate, tenantId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/instance/stat")
    @SaCheckPermission("workflow:instance:view")
    @Operation(summary = "流程实例统计（统计卡全量聚合，与列表同筛选条件）")
    public ApiResponse<Map<String, Object>> statInstances(
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        return ApiResponse.ok(workflowService.statInstances(processName, status, startDate, endDate, tenantId));
    }

    @GetMapping("/instance/detail")
    @SaCheckPermission("workflow:instance:view")
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

    @SaCheckPermission("workflow:task:view")
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

    @SaCheckPermission("workflow:task:view")
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

    @SaCheckPermission("workflow:instance:view")
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

    @SaCheckPermission("workflow:task:view")
    @GetMapping("/pending/count")
    @Operation(summary = "获取待办数量")
    public ApiResponse<Map<String, Object>> getPendingCount(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        int count = workflowService.getPendingCount(resolveUserId(userId), tenantId);
        return ApiResponse.ok(Map.of("count", count));
    }

    @GetMapping("/task/page")
    @SaCheckPermission("workflow:task:view")
    @Operation(summary = "分页查询任务（待办/已办）")
    public ApiResponse<Map<String, Object>> pageTasks(
            @Parameter(description = "标签页: todo/done") @RequestParam(defaultValue = "todo") String tab,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "优先级 high/medium/low") @RequestParam(required = false) String priority,
            @Parameter(description = "创建时间下限 YYYY-MM-DD") @RequestParam(required = false) String startDate,
            @Parameter(description = "创建时间上限 YYYY-MM-DD") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        Map<String, Object> result = workflowService.pageTasks(tab, resolveUserId(userId), pageNum, pageSize,
                tenantId, taskName, processName, priority, startDate, endDate);
        return ApiResponse.ok(result);
    }

    @GetMapping("/task/stat")
    @SaCheckPermission("workflow:task:view")
    @Operation(summary = "任务统计（待办/已办统计卡的服务端全量真聚合，与列表同筛选条件）")
    public ApiResponse<Map<String, Object>> statTasks(
            @Parameter(description = "标签页: todo/done") @RequestParam(defaultValue = "todo") String tab,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "优先级 high/medium/low") @RequestParam(required = false) String priority,
            @Parameter(description = "创建时间下限 YYYY-MM-DD") @RequestParam(required = false) String startDate,
            @Parameter(description = "创建时间上限 YYYY-MM-DD") @RequestParam(required = false) String endDate,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        Map<String, Object> stat = workflowService.statTasks(tab, resolveUserId(userId), tenantId,
                taskName, processName, priority, startDate, endDate);
        return ApiResponse.ok(stat);
    }

    @GetMapping("/task/detail")
    @SaCheckPermission("workflow:task:view")
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
    @SaCheckPermission("workflow:task:approve")
    @Operation(summary = "按任务审批（approve/reject/return；return 为真实节点回退）")
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
    @SaCheckPermission("workflow:task:transfer")
    @Operation(summary = "按任务转办他人（本系统只有「转办」一种语义，无「委托」）")
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

    // ==================== 流程分析（设置 → 工作流 → 流程分析，菜单 80611 / set:workflow-analysis） ====================
    //
    // 本系统独有页面（ql361 无工作流域，无对标），两条端点均为只读服务端聚合：
    //   GET /api/workflow/analysis/refresh  → 统计卡 + 按流程耗时 + 按节点耗时（processName 可选，仅过滤「节点耗时」段）
    //   GET /api/workflow/analysis/report   → 按日审批效率（缺省近 7 天）
    // 权限码 workflow:analysis:view 种子见迁移 V11.417.0（本模块口径：读端点同样加方法级权限码，
    // 与 workflow:definition:list / workflow:instance:view / workflow:task:view 一致）。

    @GetMapping("/analysis/refresh")
    @SaCheckPermission("workflow:analysis:view")
    @Operation(summary = "流程分析汇总（真实数据聚合；processName 仅过滤「节点耗时」段）")
    public ApiResponse<Map<String, Object>> getAnalysisSummary(
            @Parameter(description = "流程名称（可选，缺省=全部流程；仅影响节点耗时）") @RequestParam(required = false) String processName,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        return ApiResponse.ok(workflowService.getAnalysisSummary(processName, tenantId));
    }

    @GetMapping("/analysis/report")
    @SaCheckPermission("workflow:analysis:view")
    @Operation(summary = "审批效率报表（按日聚合）")
    public ApiResponse<Map<String, Object>> getAnalysisReport(
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam(required = false) String endDate,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        return ApiResponse.ok(workflowService.getAnalysisReport(startDate, endDate, tenantId));
    }

    // ==================== 审批操作 ====================

    @SaCheckPermission("workflow:task:approve")
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

    @SaCheckPermission("workflow:task:approve")
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

    @SaCheckPermission("workflow:task:transfer")
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

    @SaCheckPermission("workflow:instance:withdraw")
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

    @SaCheckPermission("workflow:instance:cancel")
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
    @SaCheckPermission("workflow:instance:view")
    @Operation(summary = "获取审批记录")
    public ApiResponse<List<ApprovalRecord>> getApprovalRecords(@PathVariable String instanceId) {
        List<ApprovalRecord> records = workflowService.getApprovalRecords(instanceId);
        return ApiResponse.ok(records);
    }

    // ==================== 流程监控 - 流程图 ====================

    @GetMapping("/instance/diagram")
    @SaCheckPermission("workflow:instance:diagram")
    @Operation(summary = "获取流程图（SVG）")
    public ApiResponse<Map<String, Object>> getInstanceDiagram(
            @Parameter(description = "流程实例ID") @RequestParam String instanceId) {

        Map<String, Object> diagram = workflowService.getInstanceDiagram(instanceId);
        return ApiResponse.ok(diagram);
    }

    // ==================== 流程监控 - 流程干预 ====================

    @PostMapping("/instance/{instanceId}/intervene")
    @SaCheckPermission("workflow:instance:intervene")
    @Operation(summary = "流程干预（终止/挂起/恢复）")
    public ApiResponse<Map<String, Object>> interveneInstance(
            @PathVariable String instanceId,
            @RequestBody InterveneRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        // 干预是强审计动作：理由必填必须由**服务端**硬校验（前端 required 只是双保险，
        // 客户端可绕过 → 历史实现会兜底成「未填写理由」落审计记录，审计链失效）
        if (request.getReason() == null || request.getReason().isBlank()) {
            return ApiResponse.error(400, "请填写干预理由");
        }

        log.info("流程干预: instanceId={}, action={}, userId={}", instanceId, request.getAction(), userId);

        boolean success = workflowService.interveneInstance(instanceId, request.getAction(),
                resolveUserId(userId), request.getReason());

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "操作成功" : "操作失败");
        return ApiResponse.ok(result);
    }

    // ==================== 回调补偿（第三期） ====================

    @SaCheckPermission("workflow:callback-log:list")
    @GetMapping("/callback-log/page")
    @Operation(summary = "分页查询审批回调补偿日志")
    public ApiResponse<Map<String, Object>> pageCallbackLogs(
            @Parameter(description = "补偿状态: pending/success/failed/final-failed") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {

        return ApiResponse.ok(callbackDispatcher.pageLogs(status, pageNum, pageSize));
    }

    @SaCheckPermission("workflow:callback-log:retry")
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
        /**
         * 干预理由（**服务端必填**：空白 → POST /instance/{id}/intervene 直接 400「请填写干预理由」；
         * 前端弹窗 required 校验保留作双保险。值落 workflow_task.comment 供审计追溯）
         */
        private String reason;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
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
        /**
         * 兼容字段：历史前端曾用 transfer/delegate 区分「转办」与「委托」。
         * 本系统的任务表**没有 owner 类字段**，「委托（代处理并回交原处理人）」无法实现 →
         * 该字段**不参与任何语义**，两种取值都只有「转办（任务所有权转移）」一种行为。
         * 前端已移除「委托」入口，不再下发本字段（见《我的待办开发文档》§5.4）。
         */
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
