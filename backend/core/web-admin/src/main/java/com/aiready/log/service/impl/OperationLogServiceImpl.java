package com.aiready.log.service.impl;

import com.aiready.log.dto.OperationLogDTO;
import com.aiready.log.dto.OperationLogQueryRequest;
import com.aiready.log.entity.OperationLog;
import com.aiready.log.mapper.OperationLogMapper;
import com.aiready.log.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
    
    private final OperationLogMapper operationLogMapper;
    
    @Override
    public void saveLog(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }
    
    @Async
    @Override
    public void saveLogAsync(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("异步保存操作日志失败", e);
        }
    }
    
    @Override
    public IPage<OperationLogDTO> queryLogs(OperationLogQueryRequest request) {
        Page<OperationLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(request.getModule())) {
            wrapper.eq(OperationLog::getModule, request.getModule());
        }
        
        if (StringUtils.isNotBlank(request.getOperationType())) {
            wrapper.eq(OperationLog::getOperationType, request.getOperationType());
        }
        
        if (request.getOperationTypes() != null && !request.getOperationTypes().isEmpty()) {
            wrapper.in(OperationLog::getOperationType, request.getOperationTypes());
        }
        
        if (StringUtils.isNotBlank(request.getOperatorName())) {
            wrapper.like(OperationLog::getOperatorName, request.getOperatorName());
        }
        
        if (request.getOperatorId() != null) {
            wrapper.eq(OperationLog::getOperatorId, request.getOperatorId());
        }
        
        if (request.getStatus() != null) {
            wrapper.eq(OperationLog::getStatus, request.getStatus());
        }
        
        if (request.getStartTime() != null) {
            wrapper.ge(OperationLog::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(OperationLog::getCreateTime, request.getEndTime());
        }
        
        if (StringUtils.isNotBlank(request.getRequestUrl())) {
            wrapper.like(OperationLog::getRequestUrl, request.getRequestUrl());
        }
        
        if (StringUtils.isNotBlank(request.getOperatorIp())) {
            wrapper.eq(OperationLog::getOperatorIp, request.getOperatorIp());
        }
        
        if (StringUtils.isNotBlank(request.getKeyword())) {
            wrapper.and(w -> w.like(OperationLog::getOperationDesc, request.getKeyword())
                    .or()
                    .like(OperationLog::getModule, request.getKeyword()));
        }
        
        wrapper.orderByDesc(OperationLog::getCreateTime);
        
        IPage<OperationLog> logPage = operationLogMapper.selectPage(page, wrapper);
        return logPage.convert(this::convertToDTO);
    }
    
    @Override
    public OperationLogDTO getLogDetail(Long id) {
        OperationLog operationLog = operationLogMapper.selectById(id);
        return operationLog != null ? convertToDTO(operationLog) : null;
    }
    
    @Override
    public void deleteLog(Long id) {
        operationLogMapper.deleteById(id);
    }
    
    @Override
    public void batchDeleteLogs(List<Long> ids) {
        operationLogMapper.deleteBatchIds(ids);
    }
    
    @Override
    public int clearLogsBefore(LocalDateTime date) {
        return operationLogMapper.deleteBeforeDate(date);
    }
    
    @Override
    public String exportLogs(OperationLogQueryRequest request) {
        return "operation_logs_" + System.currentTimeMillis() + ".xlsx";
    }
    
    @Override
    public Map<String, Long> getOperationTypeStats(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(OperationLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(OperationLog::getCreateTime, endTime);
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        return logs.stream()
                .collect(Collectors.groupingBy(OperationLog::getOperationType, Collectors.counting()));
    }
    
    @Override
    public Map<String, Long> getModuleStats(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(OperationLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(OperationLog::getCreateTime, endTime);
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        return logs.stream()
                .collect(Collectors.groupingBy(
                    log -> log.getModule() != null ? log.getModule() : "未知模块",
                    Collectors.counting()
                ));
    }
    
    @Override
    public List<Map<String, Object>> getOperationTrend(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(OperationLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(OperationLog::getCreateTime, endTime);
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        Map<LocalDateTime, Long> dailyStats = logs.stream()
                .collect(Collectors.groupingBy(
                    log -> log.getCreateTime().toLocalDate().atStartOfDay(),
                    Collectors.counting()
                ));
        
        return dailyStats.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", entry.getKey().toLocalDate().toString());
                    map.put("count", entry.getValue());
                    return map;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("date")))
                .collect(Collectors.toList());
    }
    
    private OperationLogDTO convertToDTO(OperationLog log) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(log.getId());
        dto.setModule(log.getModule());
        dto.setOperationType(log.getOperationType());
        dto.setOperationTypeDesc(getOperationTypeDesc(log.getOperationType()));
        dto.setOperationDesc(log.getOperationDesc());
        dto.setRequestMethod(log.getRequestMethod());
        dto.setRequestUrl(log.getRequestUrl());
        dto.setOperatorId(log.getOperatorId());
        dto.setOperatorName(log.getOperatorName());
        dto.setOperatorIp(log.getOperatorIp());
        dto.setOperatorLocation(log.getOperatorLocation());
        dto.setStatus(log.getStatus());
        dto.setStatusDesc(log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败");
        dto.setErrorMsg(log.getErrorMsg());
        dto.setExecutionTime(log.getExecutionTime());
        dto.setCreateTime(log.getCreateTime());
        return dto;
    }
    
    private String getOperationTypeDesc(String type) {
        if (type == null) return "其他";
        switch (type) {
            case "CREATE": return "创建";
            case "UPDATE": return "更新";
            case "DELETE": return "删除";
            case "QUERY": return "查询";
            case "EXPORT": return "导出";
            case "IMPORT": return "导入";
            case "LOGIN": return "登录";
            case "LOGOUT": return "登出";
            default: return "其他";
        }
    }
}
