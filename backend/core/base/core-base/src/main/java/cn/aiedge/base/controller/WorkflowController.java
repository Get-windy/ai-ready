package cn.aiedge.base.controller;

import cn.aiedge.base.entity.WorkflowDefinition;
import cn.aiedge.base.entity.WorkflowInstance;
import cn.aiedge.base.entity.WorkflowTask;
import cn.aiedge.base.service.WorkflowService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流管理控制器
 * 审批流程定义、实例、任务管理
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "工作流管理", description = "审批流程定义、实例、任务管理API")
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    // ── 流程定义管理 ────────────────────────────────────

    @Operation(summary = "分页查询流程定义")
    @GetMapping("/definitions/page")
    public Result<PageResult<WorkflowDefinition>> pageDefinitions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<WorkflowDefinition> page = new Page<>(pageNum, pageSize);
        Page<WorkflowDefinition> result = workflowService.pageDefinitions(page, tenantId, processName, status);
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取流程定义详情")
    @GetMapping("/definitions/{id}")
    public Result<WorkflowDefinition> getDefinition(@PathVariable Long id) {
        return Result.success(workflowService.getDefinition(id));
    }

    @Operation(summary = "创建流程定义")
    @PostMapping("/definitions")
    public Result<Long> createDefinition(@RequestBody WorkflowDefinition definition) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        definition.setTenantId(tenantId);
        return Result.success(workflowService.createDefinition(definition));
    }

    @Operation(summary = "更新流程定义")
    @PutMapping("/definitions/{id}")
    public Result<Void> updateDefinition(@PathVariable Long id, @RequestBody WorkflowDefinition definition) {
        definition.setId(id);
        workflowService.updateDefinition(definition);
        return Result.success();
    }

    @Operation(summary = "发布流程定义")
    @PutMapping("/definitions/{id}/publish")
    public Result<Void> publishDefinition(@PathVariable Long id) {
        workflowService.publishDefinition(id);
        return Result.success();
    }

    @Operation(summary = "停用流程定义")
    @PutMapping("/definitions/{id}/disable")
    public Result<Void> disableDefinition(@PathVariable Long id) {
        workflowService.disableDefinition(id);
        return Result.success();
    }

    @Operation(summary = "删除流程定义")
    @DeleteMapping("/definitions/{id}")
    public Result<Void> deleteDefinition(@PathVariable Long id) {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(id);
        def.setDeleted(1);
        workflowService.updateDefinition(def);
        return Result.success();
    }

    // ── 流程实例管理 ────────────────────────────────────

    @Operation(summary = "启动流程实例")
    @PostMapping("/instances/start")
    public Result<Long> startInstance(@RequestBody Map<String, Object> params) {
        Long definitionId = Long.valueOf(params.get("definitionId").toString());
        Long businessId = params.get("businessId") != null ? Long.valueOf(params.get("businessId").toString()) : null;
        String businessType = (String) params.get("businessType");
        String title = (String) params.get("title");
        Long applicantId = SecurityUtils.getCurrentUserId();
        String applicantName = SecurityUtils.getCurrentUsername();

        return Result.success(workflowService.startInstance(definitionId, businessId, businessType, title, applicantId, applicantName));
    }

    @Operation(summary = "获取流程实例详情")
    @GetMapping("/instances/{id}")
    public Result<WorkflowInstance> getInstance(@PathVariable Long id) {
        return Result.success(workflowService.getInstance(id));
    }

    @Operation(summary = "撤回流程实例")
    @PutMapping("/instances/{id}/withdraw")
    public Result<Void> withdrawInstance(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        workflowService.withdraw(id, userId);
        return Result.success();
    }

    @Operation(summary = "获取流程审批历史")
    @GetMapping("/instances/{id}/history")
    public Result<List<WorkflowTask>> getInstanceHistory(@PathVariable Long id) {
        return Result.success(workflowService.getInstanceHistory(id));
    }

    // ── 任务管理 ────────────────────────────────────

    @Operation(summary = "获取我的待办任务")
    @GetMapping("/tasks/todo")
    public Result<List<WorkflowTask>> getTodoTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return Result.success(workflowService.getTodoTasks(userId, tenantId));
    }

    @Operation(summary = "获取我的已办任务")
    @GetMapping("/tasks/done")
    public Result<List<WorkflowTask>> getDoneTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return Result.success(workflowService.getDoneTasks(userId, tenantId));
    }

    @Operation(summary = "审批通过")
    @PutMapping("/tasks/{id}/approve")
    public Result<Void> approveTask(
            @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        Long userId = SecurityUtils.getCurrentUserId();
        workflowService.approve(id, userId, comment);
        return Result.success();
    }

    @Operation(summary = "审批驳回")
    @PutMapping("/tasks/{id}/reject")
    public Result<Void> rejectTask(
            @PathVariable Long id,
            @Parameter(description = "驳回原因") @RequestParam(required = false) String comment) {
        Long userId = SecurityUtils.getCurrentUserId();
        workflowService.reject(id, userId, comment);
        return Result.success();
    }

    @Operation(summary = "转交任务")
    @PutMapping("/tasks/{id}/transfer")
    public Result<Void> transferTask(
            @PathVariable Long id,
            @Parameter(description = "目标用户ID") @RequestParam Long toUserId,
            @Parameter(description = "转交原因") @RequestParam(required = false) String comment) {
        Long fromUserId = SecurityUtils.getCurrentUserId();
        workflowService.transfer(id, fromUserId, toUserId, comment);
        return Result.success();
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/tasks/{id}")
    public Result<WorkflowTask> getTask(@PathVariable Long id) {
        WorkflowTask task = workflowService.getInstanceHistory(id).stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
        return Result.success(task);
    }
}