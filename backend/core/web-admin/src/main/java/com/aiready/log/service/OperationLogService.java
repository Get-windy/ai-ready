package com.aiready.log.service;

import com.aiready.log.dto.OperationLogDTO;
import com.aiready.log.dto.OperationLogQueryRequest;
import com.aiready.log.entity.OperationLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {
    
    /**
     * 保存操作日志
     */
    void saveLog(OperationLog log);
    
    /**
     * 异步保存操作日志
     */
    void saveLogAsync(OperationLog log);
    
    /**
     * 查询操作日志列表
     */
    IPage<OperationLogDTO> queryLogs(OperationLogQueryRequest request);
    
    /**
     * 获取日志详情
     */
    OperationLogDTO getLogDetail(Long id);
    
    /**
     * 删除日志
     */
    void deleteLog(Long id);
    
    /**
     * 批量删除日志
     */
    void batchDeleteLogs(List<Long> ids);
    
    /**
     * 清理指定日期之前的日志
     */
    int clearLogsBefore(LocalDateTime date);
    
    /**
     * 导出日志
     */
    String exportLogs(OperationLogQueryRequest request);
    
    /**
     * 获取操作类型统计
     */
    Map<String, Long> getOperationTypeStats(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取模块操作统计
     */
    Map<String, Long> getModuleStats(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取操作趋势（按天统计）
     */
    List<Map<String, Object>> getOperationTrend(LocalDateTime startTime, LocalDateTime endTime);
}
