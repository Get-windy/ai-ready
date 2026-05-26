package com.aiready.config.service;

import com.aiready.config.entity.ConfigAuditLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置审计服务接口
 */
public interface ConfigAuditService extends IService<ConfigAuditLog> {
    
    /**
     * 记录审计日志
     */
    void audit(Long configId, String configKey, String operationType, String operationDesc,
               String beforeData, String afterData, Long operatorId, String operatorName,
               String operatorIp, boolean success, String errorMsg);
    
    /**
     * 查询配置的审计日志
     */
    IPage<ConfigAuditLog> getAuditLogsByConfigId(Long configId, Integer page, Integer size);
    
    /**
     * 查询操作类型日志
     */
    List<ConfigAuditLog> getAuditLogsByOperationType(String operationType, Integer limit);
    
    /**
     * 查询时间范围内的日志
     */
    IPage<ConfigAuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, 
                                                   Integer page, Integer size);
    
    /**
     * 统计操作次数
     */
    Integer countOperations(String operationType, LocalDateTime startTime, LocalDateTime endTime);
}
