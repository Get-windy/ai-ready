package cn.aiedge.scheduler.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 调度任务列表控制器
 * 提供任务列表查询接口，配合前端 /scheduler/tasks 路由使用
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "调度任务列表", description = "任务列表查询接口")
@SaCheckLogin
public class SchedulerListController {

    /**
     * 模拟的调度任务数据
     * 格式匹配前端预期：id, name, code, cron, lastRun, nextRun, status
     */
    private static final List<Map<String, Object>> MOCK_TASKS = List.of(
            createTask(1L, "数据同步任务", "dataSync", "0 0 2 * * ?", "2026-06-17 02:00:00", "2026-06-18 02:00:00", "RUNNING"),
            createTask(2L, "日志清理任务", "logCleanup", "0 0 3 * * ?", "2026-06-17 03:00:00", "2026-06-18 03:00:00", "RUNNING"),
            createTask(3L, "报表生成任务", "reportGen", "0 0 4 * * ?", "2026-06-16 04:00:00", "2026-06-18 04:00:00", "RUNNING"),
            createTask(4L, "用户状态检查", "userCheck", "0 */30 * * * ?", "2026-06-17 12:30:00", "2026-06-17 13:00:00", "RUNNING"),
            createTask(5L, "订单超时处理", "orderTimeout", "0 */5 * * * ?", "2026-06-17 12:55:00", "2026-06-17 13:00:00", "RUNNING"),
            createTask(6L, "缓存预热任务", "cacheWarmup", "0 0 5 * * ?", "2026-06-17 05:00:00", "2026-06-18 05:00:00", "PAUSED"),
            createTask(7L, "数据备份任务", "dataBackup", "0 0 1 * * ?", "2026-06-17 01:00:00", "2026-06-18 01:00:00", "RUNNING"),
            createTask(8L, "消息推送任务", "msgPush", "0 0/10 * * * ?", "2026-06-17 12:50:00", "2026-06-17 13:00:00", "RUNNING"),
            createTask(9L, "统计分析任务", "statAnalysis", "0 0 6 * * ?", null, "2026-06-18 06:00:00", "STOPPED"),
            createTask(10L, "索引重建任务", "indexRebuild", "0 0 23 * * ?", "2026-06-16 23:00:00", "2026-06-17 23:00:00", "PAUSED")
    );

    private static Map<String, Object> createTask(Long id, String name, String code,
                                                   String cron, String lastRun, String nextRun, String status) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("code", code);
        task.put("cron", cron);
        task.put("lastRun", lastRun);
        task.put("nextRun", nextRun);
        task.put("status", status);
        return task;
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取所有调度任务列表")
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {
        return ResponseEntity.ok(MOCK_TASKS);
    }
}
