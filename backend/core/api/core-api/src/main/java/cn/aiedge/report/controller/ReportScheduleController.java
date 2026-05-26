package cn.aiedge.report.controller;

import cn.aiedge.report.service.ReportScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定时报表控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/report/schedule")
@RequiredArgsConstructor
@Tag(name = "定时报表服务", description = "定时报表任务管理功能")
public class ReportScheduleController {

    private final ReportScheduleService reportScheduleService;

    /**
     * 创建定时报表任务
     */
    @PostMapping
    @Operation(summary = "创建定时报表任务", description = "创建一个新的定时报表任务")
    public ResponseEntity<Map<String, Object>> createScheduleReport(
            @Parameter(description = "报表ID") @RequestParam String reportId,
            @Parameter(description = "CRON表达式") @RequestParam String scheduleCron,
            @Parameter(description = "邮件接收人列表") @RequestParam List<String> emailRecipients,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "excel") String exportFormat,
            @Parameter(description = "报表参数") @RequestBody(required = false) Map<String, Object> parameters,
            HttpServletRequest request) {
        
        Long tenantId = getTenantId(request);
        
        Long scheduleId = reportScheduleService.createScheduleReport(
            reportId, scheduleCron, emailRecipients, exportFormat, parameters, tenantId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "scheduleId", scheduleId,
            "message", "定时报表任务创建成功"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新定时报表任务
     */
    @PutMapping("/{scheduleId}")
    @Operation(summary = "更新定时报表任务", description = "更新已存在的定时报表任务")
    public ResponseEntity<Map<String, Object>> updateScheduleReport(
            @Parameter(description = "定时报表任务ID") @PathVariable Long scheduleId,
            @Parameter(description = "CRON表达式") @RequestParam(required = false) String scheduleCron,
            @Parameter(description = "邮件接收人列表") @RequestParam(required = false) List<String> emailRecipients,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled,
            HttpServletRequest request) {
        
        boolean success = reportScheduleService.updateScheduleReport(
            scheduleId, scheduleCron, emailRecipients, enabled);
        
        Map<String, Object> result = Map.of(
            "success", success,
            "message", success ? "定时报表任务更新成功" : "定时报表任务更新失败"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除定时报表任务
     */
    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "删除定时报表任务", description = "删除指定的定时报表任务")
    public ResponseEntity<Map<String, Object>> deleteScheduleReport(
            @Parameter(description = "定时报表任务ID") @PathVariable Long scheduleId) {
        
        boolean success = reportScheduleService.deleteScheduleReport(scheduleId);
        
        Map<String, Object> result = Map.of(
            "success", success,
            "message", success ? "定时报表任务删除成功" : "定时报表任务删除失败"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取定时报表任务列表
     */
    @GetMapping
    @Operation(summary = "获取定时报表任务列表", description = "获取所有定时报表任务")
    public ResponseEntity<Map<String, Object>> getScheduleReports(
            @Parameter(description = "报表ID") @RequestParam(required = false) String reportId,
            HttpServletRequest request) {
        
        Long tenantId = getTenantId(request);
        
        List<Map<String, Object>> schedules = reportScheduleService.getScheduleReports(reportId, tenantId);
        
        Map<String, Object> result = Map.of(
            "schedules", schedules,
            "total", schedules.size()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 立即执行定时报表任务
     */
    @PostMapping("/{scheduleId}/trigger")
    @Operation(summary = "立即执行定时报表", description = "立即执行一次定时报表任务")
    public ResponseEntity<Map<String, Object>> triggerScheduleReport(
            @Parameter(description = "定时报表任务ID") @PathVariable Long scheduleId) {
        
        boolean success = reportScheduleService.triggerScheduleReport(scheduleId);
        
        Map<String, Object> result = Map.of(
            "success", success,
            "message", success ? "定时报表执行触发成功" : "定时报表执行触发失败"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 暂停定时报表任务
     */
    @PostMapping("/{scheduleId}/pause")
    @Operation(summary = "暂停定时报表任务", description = "暂停指定的定时报表任务")
    public ResponseEntity<Map<String, Object>> pauseScheduleReport(
            @Parameter(description = "定时报表任务ID") @PathVariable Long scheduleId) {
        
        boolean success = reportScheduleService.pauseScheduleReport(scheduleId);
        
        Map<String, Object> result = Map.of(
            "success", success,
            "message", success ? "定时报表任务暂停成功" : "定时报表任务暂停失败"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 恢复定时报表任务
     */
    @PostMapping("/{scheduleId}/resume")
    @Operation(summary = "恢复定时报表任务", description = "恢复指定的定时报表任务")
    public ResponseEntity<Map<String, Object>> resumeScheduleReport(
            @Parameter(description = "定时报表任务ID") @PathVariable Long scheduleId) {
        
        boolean success = reportScheduleService.resumeScheduleReport(scheduleId);
        
        Map<String, Object> result = Map.of(
            "success", success,
            "message", success ? "定时报表任务恢复成功" : "定时报表任务恢复失败"
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取定时报表执行历史
     */
    @GetMapping("/{scheduleId}/history")
    @Operation(summary = "获取定时报表执行历史", description = "获取定时报表任务的执行历史")
    public ResponseEntity<Map<String, Object>> getExecutionHistory(
            @Parameter(description = "定时报表任务ID") @PathVariable Long scheduleId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {
        
        List<Map<String, Object>> history = reportScheduleService.getExecutionHistory(scheduleId, limit);
        
        Map<String, Object> result = Map.of(
            "history", history,
            "total", history.size()
        );
        
        return ResponseEntity.ok(result);
    }

    // ==================== 辅助方法 ====================

    private Long getTenantId(HttpServletRequest request) {
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        return tenantIdHeader != null ? Long.parseLong(tenantIdHeader) : 1L;
    }
}
