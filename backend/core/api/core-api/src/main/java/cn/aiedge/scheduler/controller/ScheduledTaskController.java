package cn.aiedge.scheduler.controller;

import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.aiedge.scheduler.model.ScheduledTask;
import cn.aiedge.scheduler.model.ScheduledTaskLog;
import cn.aiedge.scheduler.service.ScheduledTaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务控制器
 */
@Tag(name = "定时任务管理")
@RestController
@RequestMapping("/api/scheduler/task")
@RequiredArgsConstructor
@SaCheckLogin
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;

    @Operation(summary = "创建任务")
    @PostMapping
    public Result<ScheduledTask> create(@RequestBody ScheduledTask task) {
        return Result.success(taskService.createTask(task));
    }

    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    public Result<ScheduledTask> update(@PathVariable Long id, @RequestBody ScheduledTask task) {
        task.setId(id);
        return Result.success(taskService.updateTask(task));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(taskService.deleteTask(id));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public Result<ScheduledTask> getById(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @Operation(summary = "分页查询任务")
    @GetMapping("/page")
    public Result<IPage<ScheduledTask>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String status) {
        Page<ScheduledTask> page = new Page<>(current, size);
        return Result.success(taskService.getTaskPage(page, taskName, status));
    }

    @Operation(summary = "启用任务")
    @PostMapping("/{id}/enable")
    public Result<Boolean> enable(@PathVariable Long id) {
        return Result.success(taskService.enableTask(id));
    }

    @Operation(summary = "禁用任务")
    @PostMapping("/{id}/disable")
    public Result<Boolean> disable(@PathVariable Long id) {
        return Result.success(taskService.disableTask(id));
    }

    @Operation(summary = "立即执行任务")
    @PostMapping("/{id}/execute")
    public Result<Boolean> execute(@PathVariable Long id) {
        return Result.success(taskService.executeTask(id));
    }

    @Operation(summary = "暂停任务")
    @PostMapping("/{id}/pause")
    public Result<Boolean> pause(@PathVariable Long id) {
        return Result.success(taskService.pauseTask(id));
    }

    @Operation(summary = "恢复任务")
    @PostMapping("/{id}/resume")
    public Result<Boolean> resume(@PathVariable Long id) {
        return Result.success(taskService.resumeTask(id));
    }

    @Operation(summary = "手动重试任务")
    @PostMapping("/log/{logId}/retry")
    public Result<Boolean> retry(@PathVariable Long logId) {
        return Result.success(taskService.retryTask(logId));
    }

    @Operation(summary = "查询任务日志")
    @GetMapping("/log/page")
    public Result<IPage<ScheduledTaskLog>> logPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<ScheduledTaskLog> page = new Page<>(current, size);
        return Result.success(taskService.getTaskLogPage(page, taskId, status, startTime, endTime));
    }

    @Operation(summary = "获取任务统计")
    @GetMapping("/{id}/statistics")
    public Result<ScheduledTaskService.TaskStatistics> statistics(@PathVariable Long id) {
        return Result.success(taskService.getTaskStatistics(id));
    }

    @Operation(summary = "批量执行任务")
    @PostMapping("/batch-execute")
    public Result<Void> batchExecute(@RequestBody List<Long> taskIds) {
        taskService.batchExecute(taskIds);
        return Result.success();
    }
}
