package cn.aiedge.scheduler.controller;

import cn.aiedge.scheduler.entity.ScheduledTask;
import cn.aiedge.scheduler.entity.TaskExecuteLog;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/scheduler/tasks")
@RequiredArgsConstructor
@Tag(name = "定时任务管理", description = "任务调度管理接口")
public class TaskSchedulerController {

    private final TaskSchedulerService taskSchedulerService;

    @PostMapping
    @Operation(summary = "创建定时任务")
    public ResponseEntity<ScheduledTask> createTask(@RequestBody ScheduledTask task) {
        return ResponseEntity.ok(taskSchedulerService.createTask(task));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "更新定时任务")
    public ResponseEntity<ScheduledTask> updateTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @RequestBody ScheduledTask task) {
        task.setId(taskId);
        return ResponseEntity.ok(taskSchedulerService.updateTask(task));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除定时任务")
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        taskSchedulerService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情")
    public ResponseEntity<ScheduledTask> getTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        ScheduledTask task = taskSchedulerService.getTask(taskId);
        return task != null ? ResponseEntity.ok(task) : ResponseEntity.notFound().build();
    }

    @GetMapping
    @Operation(summary = "获取所有任务")
    public ResponseEntity<List<ScheduledTask>> getAllTasks() {
        return ResponseEntity.ok(taskSchedulerService.getAllTasks());
    }

    @GetMapping("/group/{taskGroup}")
    @Operation(summary = "按分组获取任务")
    public ResponseEntity<List<ScheduledTask>> getTasksByGroup(
            @Parameter(description = "任务分组") @PathVariable String taskGroup) {
        return ResponseEntity.ok(taskSchedulerService.getTasksByGroup(taskGroup));
    }

    @PostMapping("/{taskId}/start")
    @Operation(summary = "启动任务")
    public ResponseEntity<Void> startTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        taskSchedulerService.startTask(taskId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/pause")
    @Operation(summary = "暂停任务")
    public ResponseEntity<Void> pauseTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        taskSchedulerService.pauseTask(taskId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/resume")
    @Operation(summary = "恢复任务")
    public ResponseEntity<Void> resumeTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        taskSchedulerService.resumeTask(taskId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/trigger")
    @Operation(summary = "立即执行任务")
    public ResponseEntity<Void> triggerTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        taskSchedulerService.triggerTask(taskId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{taskId}/logs")
    @Operation(summary = "获取任务执行日志")
    public ResponseEntity<List<TaskExecuteLog>> getTaskLogs(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(taskSchedulerService.getTaskLogs(taskId, limit));
    }

    @DeleteMapping("/logs/clean")
    @Operation(summary = "清理过期日志")
    public ResponseEntity<Integer> cleanOldLogs(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") int daysToKeep) {
        return ResponseEntity.ok(taskSchedulerService.cleanOldLogs(daysToKeep));
    }

    @PostMapping("/scheduler/start")
    @Operation(summary = "启动调度器")
    public ResponseEntity<Void> startScheduler() {
        taskSchedulerService.startScheduler();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/scheduler/stop")
    @Operation(summary = "停止调度器")
    public ResponseEntity<Void> stopScheduler() {
        taskSchedulerService.stopScheduler();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/scheduler/status")
    @Operation(summary = "获取调度器状态")
    public ResponseEntity<Boolean> getSchedulerStatus() {
        return ResponseEntity.ok(taskSchedulerService.isSchedulerRunning());
    }
}
