package cn.aiedge.base.log.controller;

import cn.aiedge.base.log.entity.SystemLog;
import cn.aiedge.base.log.service.SystemLogService;
import cn.aiedge.base.vo.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统日志管理控制器
 * 提供系统日志的增删改查、统计分析、导出等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "系统日志管理", description = "系统日志查询、统计和导出")
public class SystemLogController {

    private final SystemLogService systemLogService;

    /**
     * 分页查询系统日志
     */
    @GetMapping("/system/page")
    @Operation(summary = "分页查询系统日志")
    public Result<Page<SystemLog>> pageSystemLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer logType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        Page<SystemLog> pageParam = new Page<>(page, pageSize);
        return Result.ok(systemLogService.pageLogs(pageParam, logType, userId, moduleName, status, startTime, endTime));
    }

    /**
     * 获取系统日志详情
     */
    @GetMapping("/system/{id}")
    @Operation(summary = "获取系统日志详情")
    public Result<SystemLog> getSystemLog(@PathVariable Long id) {
        return Result.ok(systemLogService.getLogDetail(id));
    }

    /**
     * 获取用户最近日志
     */
    @GetMapping("/system/recent/{userId}")
    @Operation(summary = "获取用户最近日志")
    public Result<List<SystemLog>> getRecentLogsByUser(@PathVariable Long userId, @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(systemLogService.getRecentLogsByUser(userId, limit));
    }

    /**
     * 获取模块统计
     */
    @GetMapping("/system/stats/module")
    @Operation(summary = "获取模块统计")
    public Result<List<Map<String, Object>>> getModuleStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(systemLogService.getModuleStats(1L, startTime, endTime)); // 默认租户ID
    }

    /**
     * 获取用户操作统计
     */
    @GetMapping("/system/stats/user")
    @Operation(summary = "获取用户操作统计")
    public Result<List<Map<String, Object>>> getUserStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(systemLogService.getUserStats(1L, startTime, endTime, limit));
    }

    /**
     * 获取操作类型统计
     */
    @GetMapping("/system/stats/type")
    @Operation(summary = "获取操作类型统计")
    public Result<List<Map<String, Object>>> getOperationTypeStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(systemLogService.getOperationTypeStats(1L, startTime, endTime));
    }

    /**
     * 导出系统日志
     */
    @GetMapping("/system/export")
    @Operation(summary = "导出系统日志")
    public void exportSystemLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {
        
        List<SystemLog> logs = systemLogService.exportLogs(1L, userId, moduleName, startTime, endTime); // 默认租户ID
        
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodeFilename("system_log") + ".csv");
        
        OutputStream out = response.getOutputStream();
        StringBuilder sb = new StringBuilder();
        
        // BOM头
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        
        // 表头
        sb.append("ID,日志类型,日志级别,模块名称,操作类型,用户名,IP地址,操作状态,执行时长,创建时间,备注\n");
        
        // 数据
        for (SystemLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(getLogTypeName(log.getLogType())).append(",");
            sb.append(log.getLogLevel()).append(",");
            sb.append(escapeCsv(log.getModuleName())).append(",");
            sb.append(escapeCsv(log.getOperationType())).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(escapeCsv(log.getIpAddress())).append(",");
            sb.append(log.getStatus() == 1 ? "成功" : "失败").append(",");
            sb.append(log.getExecutionTime() != null ? log.getExecutionTime() : "").append(",");
            sb.append(log.getCreateTime() != null ? log.getCreateTime() : "").append(",");
            sb.append(escapeCsv(log.getRemark())).append("\n");
        }
        
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * 清理历史系统日志
     */
    @DeleteMapping("/system/clean")
    @Operation(summary = "清理历史系统日志")
    public Result<Integer> cleanSystemLogs(@RequestParam(defaultValue = "90") int days) {
        return Result.ok(systemLogService.cleanLogs(days));
    }

    /**
     * 删除系统日志
     */
    @DeleteMapping("/system/{id}")
    @Operation(summary = "删除系统日志")
    public Result<Void> deleteSystemLog(@PathVariable Long id) {
        systemLogService.deleteLog(id);
        return Result.ok();
    }

    /**
     * 批量删除系统日志
     */
    @DeleteMapping("/system/batch")
    @Operation(summary = "批量删除系统日志")
    public Result<Void> batchDeleteSystemLogs(@RequestBody List<Long> ids) {
        systemLogService.batchDeleteLogs(ids);
        return Result.ok();
    }

    // 辅助方法
    private String encodeFilename(String filename) throws IOException {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getLogTypeName(Integer logType) {
        switch (logType) {
            case 1: return "操作日志";
            case 2: return "登录日志";
            case 3: return "异常日志";
            case 4: return "系统日志";
            case 5: return "安全日志";
            default: return "未知类型";
        }
    }
}