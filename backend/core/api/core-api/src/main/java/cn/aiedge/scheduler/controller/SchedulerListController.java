package cn.aiedge.scheduler.controller;

import cn.aiedge.scheduler.mapper.ScheduledTaskMapper;
import cn.aiedge.scheduler.model.ScheduledTask;
import cn.aiedge.scheduler.service.ScheduledTaskService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 调度任务列表控制器
 * 配合前端 /scheduler/tasks 路由使用
 */
@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "调度任务列表")
@SaCheckLogin
@RequiredArgsConstructor
public class SchedulerListController {

    private final ScheduledTaskService scheduledTaskService;
    private final ScheduledTaskMapper scheduledTaskMapper;

    @GetMapping("/tasks")
    @Operation(summary = "获取所有调度任务列表")
    public List<Map<String, Object>> getAllTasks() {
        List<ScheduledTask> tasks = scheduledTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScheduledTask>()
                        .orderByAsc(ScheduledTask::getId)
        );

        return tasks.stream().map(this::toTaskMap).collect(Collectors.toList());
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除定时任务")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        boolean deleted = scheduledTaskService.deleteTask(id);
        return Map.of("success", deleted, "message", deleted ? "任务已删除" : "任务不存在");
    }

    @PostMapping("/tasks/{id}/start")
    @Operation(summary = "启动定时任务")
    public Map<String, Object> startTask(@PathVariable Long id) {
        boolean success = scheduledTaskService.resumeTask(id);
        return Map.of("success", success, "message", success ? "任务已启动" : "任务启动失败");
    }

    @PostMapping("/tasks/{id}/pause")
    @Operation(summary = "暂停定时任务")
    public Map<String, Object> pauseTask(@PathVariable Long id) {
        boolean success = scheduledTaskService.pauseTask(id);
        return Map.of("success", success, "message", success ? "任务已暂停" : "任务暂停失败");
    }

    @PostMapping("/tasks/{id}/trigger")
    @Operation(summary = "立即执行定时任务")
    public Map<String, Object> triggerTask(@PathVariable Long id) {
        boolean success = scheduledTaskService.executeTask(id);
        return Map.of("success", success, "message", success ? "任务已执行" : "任务执行失败");
    }

    /**
     * 将 ScheduledTask 转为前端所需的格式
     */
    private Map<String, Object> toTaskMap(ScheduledTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("name", task.getTaskName());
        // 从任务名生成编码
        String code = task.getTaskName() != null
                ? task.getTaskName().replaceAll("\\s+", "").toLowerCase()
                : "task_" + task.getId();
        map.put("code", code);
        map.put("cron", task.getCronExpression());
        map.put("lastRun", task.getLastExecuteTime() != null ? task.getLastExecuteTime().toString().replace("T", " ") : null);
        map.put("nextRun", task.getNextExecuteTime() != null ? task.getNextExecuteTime().toString().replace("T", " ") : null);
        // enabled字段作为状态: 1=运行中(true), 0=暂停(false)
        map.put("status", task.getEnabled() != null && task.getEnabled() == 1);
        map.put("taskDesc", task.getTaskDesc());
        map.put("executeCount", task.getExecuteCount());
        return map;
    }
}
