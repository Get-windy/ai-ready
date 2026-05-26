package com.aiready.log.controller;

import com.aiready.log.dto.OperationLogDTO;
import com.aiready.log.dto.OperationLogQueryRequest;
import com.aiready.log.service.OperationLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/log/operation")
@RequiredArgsConstructor
@Tag(name = "操作日志管理", description = "操作日志管理接口")
public class OperationLogController {
    
    private final OperationLogService operationLogService;
    
    /**
     * 查询操作日志列表
     */
    @PostMapping("/list")
    @Operation(summary = "查询操作日志列表")
    public IPage<OperationLogDTO> queryLogs(@RequestBody OperationLogQueryRequest request) {
        return operationLogService.queryLogs(request);
    }
    
    /**
     * 获取日志详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取日志详情")
    public OperationLogDTO getLogDetail(@PathVariable Long id) {
        return operationLogService.getLogDetail(id);
    }
    
    /**
     * 删除日志
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除日志")
    public void deleteLog(@PathVariable Long id) {
        operationLogService.deleteLog(id);
    }
    
    /**
     * 批量删除日志
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除日志")
    public void batchDeleteLogs(@RequestBody List<Long> ids) {
        operationLogService.batchDeleteLogs(ids);
    }
    
    /**
     * 清理指定日期之前的日志
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清理指定日期之前的日志")
    public int clearLogsBefore(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime date) {
        return operationLogService.clearLogsBefore(date);
    }
    
    /**
     * 导出日志
     */
    @PostMapping("/export")
    @Operation(summary = "导出日志")
    public String exportLogs(@RequestBody OperationLogQueryRequest request) {
        return operationLogService.exportLogs(request);
    }
    
    /**
     * 获取操作类型统计
     */
    @GetMapping("/stats/type")
    @Operation(summary = "获取操作类型统计")
    public Map<String, Long> getOperationTypeStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return operationLogService.getOperationTypeStats(startTime, endTime);
    }
    
    /**
     * 获取模块操作统计
     */
    @GetMapping("/stats/module")
    @Operation(summary = "获取模块操作统计")
    public Map<String, Long> getModuleStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return operationLogService.getModuleStats(startTime, endTime);
    }
    
    /**
     * 获取操作趋势
     */
    @GetMapping("/stats/trend")
    @Operation(summary = "获取操作趋势")
    public List<Map<String, Object>> getOperationTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return operationLogService.getOperationTrend(startTime, endTime);
    }
}
