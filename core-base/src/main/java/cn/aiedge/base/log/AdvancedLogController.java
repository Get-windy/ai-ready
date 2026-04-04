package cn.aiedge.base.log;

import cn.aiedge.base.entity.SysOperLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 高级日志查询控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/system/log/advanced")
@RequiredArgsConstructor
@Tag(name = "高级日志查询", description = "日志高级查询、过滤、导出接口")
public class AdvancedLogController {

    private final AdvancedLogQueryService advancedLogQueryService;
    private final LogExportService logExportService;

    @PostMapping("/query")
    @Operation(summary = "高级日志查询")
    public LogQueryResponse advancedQuery(@RequestBody LogQueryRequest request) {
        return advancedLogQueryService.query(request);
    }

    @GetMapping("/fulltext")
    @Operation(summary = "全文检索日志")
    public Object fullTextSearch(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int pageSize) {
        return advancedLogQueryService.fullTextSearch(keyword, page, pageSize);
    }

    @GetMapping("/ip/{ip}")
    @Operation(summary = "按IP查询日志")
    public List<SysOperLog> queryByIp(
            @Parameter(description = "IP地址") @PathVariable String ip,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return advancedLogQueryService.queryByIp(ip, startTime, endTime);
    }

    @GetMapping("/slow")
    @Operation(summary = "查询慢操作日志")
    public List<SysOperLog> querySlowOperations(
            @Parameter(description = "最小耗时(ms)") @RequestParam(defaultValue = "1000") Long minCost,
            @Parameter(description = "最大耗时(ms)") @RequestParam(required = false) Long maxCost,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "100") int limit) {
        return advancedLogQueryService.querySlowOperations(minCost, maxCost, limit);
    }

    @GetMapping("/failed")
    @Operation(summary = "查询失败操作日志")
    public List<SysOperLog> queryFailedOperations(
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "100") int limit) {
        return advancedLogQueryService.queryFailedOperations(startTime, endTime, limit);
    }

    @GetMapping("/summary")
    @Operation(summary = "获取日志统计摘要")
    public Map<String, Object> getLogSummary(
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return advancedLogQueryService.getLogSummary(startTime, endTime);
    }

    @GetMapping("/trend")
    @Operation(summary = "获取操作趋势")
    public List<Map<String, Object>> getOperationTrend(
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "时间单位") @RequestParam(defaultValue = "day") String timeUnit) {
        return advancedLogQueryService.getOperationTrend(startTime, endTime, timeUnit);
    }

    @GetMapping("/user-activity/{userId}")
    @Operation(summary = "获取用户活动分析")
    public Map<String, Object> getUserActivityAnalysis(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "分析天数") @RequestParam(defaultValue = "7") int days) {
        return advancedLogQueryService.getUserActivityAnalysis(userId, days);
    }

    @GetMapping("/anomaly")
    @Operation(summary = "检测异常操作")
    public List<Map<String, Object>> detectAnomalies(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        return advancedLogQueryService.detectAnomalousOperations(userId);
    }

    @GetMapping("/export/excel")
    @Operation(summary = "导出日志为Excel")
    public void exportToExcel(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {

        LogQueryRequest request = new LogQueryRequest();
        request.setUserId(userId);
        request.setModules(module != null ? List.of(module) : null);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        LogQueryResponse queryResponse = advancedLogQueryService.query(request);
        @SuppressWarnings("unchecked")
        List<SysOperLog> logs = (List<SysOperLog>) queryResponse.getRecords();

        logExportService.exportToExcel(logs, response);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "导出日志为CSV")
    public void exportToCsv(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {

        LogQueryRequest request = new LogQueryRequest();
        request.setUserId(userId);
        request.setModules(module != null ? List.of(module) : null);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        LogQueryResponse queryResponse = advancedLogQueryService.query(request);
        @SuppressWarnings("unchecked")
        List<SysOperLog> logs = (List<SysOperLog>) queryResponse.getRecords();

        logExportService.exportToCsv(logs, response);
    }

    @GetMapping("/export/json")
    @Operation(summary = "导出日志为JSON")
    public void exportToJson(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {

        LogQueryRequest request = new LogQueryRequest();
        request.setUserId(userId);
        request.setModules(module != null ? List.of(module) : null);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        LogQueryResponse queryResponse = advancedLogQueryService.query(request);
        @SuppressWarnings("unchecked")
        List<SysOperLog> logs = (List<SysOperLog>) queryResponse.getRecords();

        logExportService.exportToJson(logs, response);
    }
}
