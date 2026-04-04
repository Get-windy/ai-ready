package cn.aiedge.scheduler.controller;

import cn.aiedge.scheduler.monitor.TaskExecutionTracker;
import cn.aiedge.scheduler.monitor.TaskExecutionTracker.*;
import cn.aiedge.scheduler.retry.RetryPolicy;
import cn.aiedge.scheduler.retry.TaskRetryManager;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务监控控制器
 * 提供任务执行统计和监控接口
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/scheduler/monitor")
@RequiredArgsConstructor
@Tag(name = "任务监控", description = "任务执行统计与监控")
public class TaskMonitorController {

    private final TaskExecutionTracker executionTracker;
    private final TaskRetryManager retryManager;
    private final TaskSchedulerService schedulerService;

    // ==================== 执行统计 ====================

    @GetMapping("/summary")
    @Operation(summary = "获取执行摘要")
    public ResponseEntity<ExecutionSummary> getExecutionSummary() {
        return ResponseEntity.ok(executionTracker.getSummary());
    }

    @GetMapping("/global-stats")
    @Operation(summary = "获取全局统计")
    public ResponseEntity<GlobalStatistics> getGlobalStatistics() {
        return ResponseEntity.ok(executionTracker.getGlobalStatistics());
    }

    @GetMapping("/task-stats/{taskId}")
    @Operation(summary = "获取任务统计")
    public ResponseEntity<TaskStatistics> getTaskStatistics(@PathVariable Long taskId) {
        TaskStatistics stats = executionTracker.getStatistics(taskId);
        return stats != null ? ResponseEntity.ok(stats) : ResponseEntity.notFound().build();
    }

    @GetMapping("/all-task-stats")
    @Operation(summary = "获取所有任务统计")
    public ResponseEntity<Map<Long, TaskStatistics>> getAllTaskStatistics() {
        return ResponseEntity.ok(executionTracker.getAllStatistics());
    }

    // ==================== 运行状态 ====================

    @GetMapping("/running")
    @Operation(summary = "获取正在执行的任务")
    public ResponseEntity<List<ExecutionContext>> getRunningTasks() {
        return ResponseEntity.ok(
            executionTracker.getRunningTasks().values().stream().toList()
        );
    }

    @GetMapping("/running-count")
    @Operation(summary = "获取正在执行的任务数量")
    public ResponseEntity<Map<String, Integer>> getRunningCount() {
        return ResponseEntity.ok(Map.of(
            "count", executionTracker.getGlobalStatistics().getCurrentlyRunning()
        ));
    }

    // ==================== 调度器状态 ====================

    @GetMapping("/scheduler-status")
    @Operation(summary = "获取调度器状态")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", schedulerService.isSchedulerRunning());
        status.put("totalTasks", schedulerService.getAllTasks().size());
        status.put("runningTasks", 
            schedulerService.getAllTasks().stream()
                .filter(t -> t.getStatus() == 1)
                .count()
        );
        status.put("pausedTasks",
            schedulerService.getAllTasks().stream()
                .filter(t -> t.getStatus() == 0)
                .count()
        );
        return ResponseEntity.ok(status);
    }

    // ==================== 重试管理 ====================

    @GetMapping("/retry/{taskId}/policy")
    @Operation(summary = "获取任务重试策略")
    public ResponseEntity<RetryPolicy> getRetryPolicy(@PathVariable Long taskId) {
        var task = schedulerService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(retryManager.getRetryPolicy(task));
    }

    @PutMapping("/retry/{taskId}/policy")
    @Operation(summary = "设置任务重试策略")
    public ResponseEntity<Map<String, Object>> setRetryPolicy(
            @PathVariable Long taskId,
            @RequestBody RetryPolicy policy) {
        retryManager.setRetryPolicy(taskId, policy);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "重试策略已更新"
        ));
    }

    @GetMapping("/retry/{taskId}/count")
    @Operation(summary = "获取任务重试计数")
    public ResponseEntity<Map<String, Object>> getRetryCount(@PathVariable Long taskId) {
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "retryCount", retryManager.getRetryCount(taskId)
        ));
    }

    @PostMapping("/retry/{taskId}/reset")
    @Operation(summary = "重置重试计数")
    public ResponseEntity<Map<String, Object>> resetRetryCount(@PathVariable Long taskId) {
        retryManager.clearRetryCount(taskId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "重试计数已重置"
        ));
    }

    @PostMapping("/retry/{taskId}/manual")
    @Operation(summary = "手动触发重试")
    public ResponseEntity<Map<String, Object>> manualRetry(@PathVariable Long taskId) {
        boolean success = retryManager.manualRetry(taskId);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "重试已触发" : "触发重试失败"
        ));
    }

    // ==================== 统计重置 ====================

    @PostMapping("/reset-stats/{taskId}")
    @Operation(summary = "重置任务统计")
    public ResponseEntity<Map<String, Object>> resetTaskStats(@PathVariable Long taskId) {
        executionTracker.resetStatistics(taskId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "任务统计已重置"
        ));
    }

    @PostMapping("/reset-all-stats")
    @Operation(summary = "重置所有统计")
    public ResponseEntity<Map<String, Object>> resetAllStats() {
        executionTracker.resetAllStatistics();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "所有统计已重置"
        ));
    }

    // ==================== 健康检查 ====================

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        boolean schedulerHealthy = schedulerService.isSchedulerRunning();
        ExecutionSummary summary = executionTracker.getSummary();
        
        health.put("status", schedulerHealthy ? "UP" : "DOWN");
        health.put("schedulerRunning", schedulerHealthy);
        health.put("totalExecutions", summary.getTotalExecutions());
        health.put("successRate", summary.getSuccessRate());
        health.put("currentlyRunning", summary.getCurrentlyRunning());
        
        // 检查是否有异常情况
        List<String> issues = new java.util.ArrayList<>();
        if (!schedulerHealthy) {
            issues.add("调度器未运行");
        }
        if (summary.getSuccessRate() < 50 && summary.getTotalExecutions() > 10) {
            issues.add("成功率过低: " + summary.getSuccessRate() + "%");
        }
        
        health.put("issues", issues);
        health.put("healthy", issues.isEmpty());
        
        return ResponseEntity.ok(health);
    }
}
