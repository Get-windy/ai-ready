package cn.aiedge.audit.controller;

import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志记录和查询接口")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping("/record")
    @Operation(summary = "记录审计日志")
    public Map<String, Object> record(@RequestBody AuditLog log) {
        Long logId = auditLogService.record(log);
        return Map.of("success", true, "logId", logId);
    }

    @GetMapping("/query")
    @Operation(summary = "查询审计日志")
    public Map<String, Object> query(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "审计类型") @RequestParam(required = false) String auditType,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize) {

        return auditLogService.query(tenantId, auditType, module, userId, startTime, endTime, page, pageSize);
    }

    @GetMapping("/detail/{logId}")
    @Operation(summary = "获取审计日志详情")
    public AuditLog getDetail(
            @Parameter(description = "日志ID") @PathVariable Long logId) {
        return auditLogService.getDetail(logId);
    }

    @GetMapping("/user/{userId}/history")
    @Operation(summary = "获取用户操作历史")
    public List<AuditLog> getUserHistory(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "50") int limit) {
        return auditLogService.getUserHistory(userId, limit);
    }

    @GetMapping("/object/{targetType}/{targetId}/history")
    @Operation(summary = "获取对象操作历史")
    public List<AuditLog> getObjectHistory(
            @Parameter(description = "对象类型") @PathVariable String targetType,
            @Parameter(description = "对象ID") @PathVariable String targetId) {
        return auditLogService.getObjectHistory(targetType, targetId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取审计统计")
    public Map<String, Object> getStatistics(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return auditLogService.getStatistics(tenantId, startTime, endTime);
    }

    @GetMapping("/export")
    @Operation(summary = "导出审计日志")
    public List<AuditLog> exportLogs(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return auditLogService.exportLogs(tenantId, startTime, endTime);
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清理历史日志")
    public Map<String, Object> cleanLogs(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "90") int days) {
        int count = auditLogService.cleanLogs(days);
        return Map.of("success", true, "deletedCount", count);
    }
}
