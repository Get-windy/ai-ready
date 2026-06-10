package cn.aiedge.erp.printing.controller;

import cn.aiedge.common.cache.RedisCache;
import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.entity.PrintTask;
import cn.aiedge.erp.printing.service.PrintTaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "打印任务管理", description = "打印任务的创建、队列管理、状态查询、批量打印等操作")
@RestController
@RequestMapping("/api/v1/print/tasks")
@RequiredArgsConstructor
public class PrintTaskController {

    private final PrintTaskService taskService;
    private final RedisCache redisCache;

    /** 幂等性键 Redis 前缀 */
    private static final String IDEMPOTENT_PREFIX = "idempotent:print:create:";
    /** 幂等性键 TTL（24 小时） */
    private static final long IDEMPOTENT_TTL_SECONDS = 86400;

    @Operation(summary = "创建打印任务（支持幂等性）")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(
            @RequestBody PrintTaskCreateRequest request,
            HttpServletRequest httpRequest) {
        // 幂等性检查
        String idempotentKey = httpRequest.getHeader("Idempotency-Key");
        if (idempotentKey != null && !idempotentKey.isBlank()) {
            String redisKey = IDEMPOTENT_PREFIX + idempotentKey;
            if (!redisCache.setIfAbsent(redisKey, "1", IDEMPOTENT_TTL_SECONDS, TimeUnit.SECONDS)) {
                return ResponseEntity.status(409).body(Map.of(
                    "code", "409",
                    "message", "重复请求，请勿重复提交",
                    "data", Map.of("idempotentKey", idempotentKey)
                ));
            }
        }

        String clientIp = getClientIp(httpRequest);
        PrintTask task = taskService.createTask(request, clientIp);
        return ResponseEntity.ok(success(task));
    }

    @Operation(summary = "批量打印（支持幂等性）")
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchPrint(
            @RequestBody BatchPrintRequest request,
            HttpServletRequest httpRequest) {
        // 幂等性检查
        String idempotentKey = httpRequest.getHeader("Idempotency-Key");
        if (idempotentKey != null && !idempotentKey.isBlank()) {
            String redisKey = IDEMPOTENT_PREFIX + "batch:" + idempotentKey;
            if (!redisCache.setIfAbsent(redisKey, "1", IDEMPOTENT_TTL_SECONDS, TimeUnit.SECONDS)) {
                return ResponseEntity.status(409).body(Map.of(
                    "code", "409",
                    "message", "重复请求，请勿重复提交",
                    "data", Map.of("idempotentKey", idempotentKey)
                ));
            }
        }

        String clientIp = getClientIp(httpRequest);
        BatchPrintResult result = taskService.batchPrint(request, clientIp);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable Long id) {
        PrintTask task = taskService.getTaskById(id);
        return ResponseEntity.ok(success(task));
    }

    @Operation(summary = "任务列表查询")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType) {
        Page<PrintTask> pageResult = taskService.listTasks(page, size, status, documentType);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取打印队列")
    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> getPrintQueue() {
        List<PrintTask> queue = taskService.getPrintQueue();
        return ResponseEntity.ok(success(queue));
    }

    @Operation(summary = "取消打印任务")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable Long id) {
        taskService.cancelTask(id);
        PrintTask task = taskService.getTaskById(id);
        return ResponseEntity.ok(success(task));
    }

    @Operation(summary = "重试打印任务")
    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retryTask(@PathVariable Long id) {
        taskService.retryTask(id);
        PrintTask task = taskService.getTaskById(id);
        return ResponseEntity.ok(success(task));
    }

    @Operation(summary = "打印历史查询")
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String documentType) {
        Page<PrintTask> pageResult = taskService.getHistory(page, size, startDate, endDate, documentType);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取队列长度")
    @GetMapping("/queue/length")
    public ResponseEntity<Map<String, Object>> getQueueLength() {
        int length = taskService.getQueueLength();
        return ResponseEntity.ok(success(Map.of("length", length)));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", "200");
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}