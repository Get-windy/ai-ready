package com.aiready.config.service.impl;

import com.aiready.config.entity.ConfigAuditLog;
import com.aiready.config.mapper.ConfigAuditLogMapper;
import com.aiready.config.service.ConfigAuditService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置审计服务实现
 */
@Service
@RequiredArgsConstructor
public class ConfigAuditServiceImpl extends ServiceImpl<ConfigAuditLogMapper, ConfigAuditLog> 
        implements ConfigAuditService {
    
    private final ConfigAuditLogMapper auditLogMapper;
    
    @Override
    public void audit(Long configId, String configKey, String operationType, String operationDesc,
                      String beforeData, String afterData, Long operatorId, String operatorName,
                      String operatorIp, boolean success, String errorMsg) {
        ConfigAuditLog log = new ConfigAuditLog();
        log.setConfigId(configId);
        log.setConfigKey(configKey);
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorIp(operatorIp);
        log.setOperationTime(LocalDateTime.now());
        log.setSuccess(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        log.setCreateTime(LocalDateTime.now());
        
        auditLogMapper.insert(log);
    }
    
    @Override
    public IPage<ConfigAuditLog> getAuditLogsByConfigId(Long configId, Integer page, Integer size) {
        Page<ConfigAuditLog> pageParam = new Page<>(page, size);
        List<ConfigAuditLog> logs = auditLogMapper.selectByConfigId(configId);
        pageParam.setRecords(logs);
        pageParam.setTotal(logs.size());
        return pageParam;
    }
    
    @Override
    public List<ConfigAuditLog> getAuditLogsByOperationType(String operationType, Integer limit) {
        return auditLogMapper.selectByOperationType(operationType, limit);
    }
    
    @Override
    public IPage<ConfigAuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, 
                                                          Integer page, Integer size) {
        Page<ConfigAuditLog> pageParam = new Page<>(page, size);
        List<ConfigAuditLog> logs = auditLogMapper.selectByTimeRange(startTime, endTime);
        pageParam.setRecords(logs);
        pageParam.setTotal(logs.size());
        return pageParam;
    }
    
    @Override
    public Integer countOperations(String operationType, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.countByOperationTypeAndTime(operationType, startTime, endTime);
    }
}
