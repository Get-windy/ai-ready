package cn.aiedge.base.log.service.impl;

import cn.aiedge.base.log.entity.SystemLog;
import cn.aiedge.base.log.mapper.SystemLogMapper;
import cn.aiedge.base.log.service.SystemLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 系统日志服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl extends ServiceImpl<SystemLogMapper, SystemLog> 
        implements SystemLogService {

    private final SystemLogMapper systemLogMapper;

    @Override
    public void saveLog(SystemLog log) {
        try {
            save(log);
            SystemLogServiceImpl.log.info("保存系统日志成功: id={}, type={}, module={}", log.getId(), log.getLogType(), log.getModuleName());
        } catch (Exception e) {
            SystemLogServiceImpl.log.error("保存系统日志失败", e);
            throw e;
        }
    }

    @Override
    @Async
    public void saveLogAsync(SystemLog log) {
        try {
            save(log);
            SystemLogServiceImpl.log.debug("异步保存系统日志成功: id={}, type={}", log.getId(), log.getLogType());
        } catch (Exception e) {
            SystemLogServiceImpl.log.error("异步保存系统日志失败", e);
        }
    }

    @Override
    public Page<SystemLog> pageLogs(Page<SystemLog> page, Integer logType, Long userId, String moduleName, 
                                   Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(logType != null, SystemLog::getLogType, logType)
               .eq(userId != null, SystemLog::getUserId, userId)
               .like(moduleName != null && !moduleName.isEmpty(), SystemLog::getModuleName, moduleName)
               .eq(status != null, SystemLog::getStatus, status)
               .ge(startTime != null, SystemLog::getCreateTime, startTime)
               .le(endTime != null, SystemLog::getCreateTime, endTime)
               .orderByDesc(SystemLog::getCreateTime);

        return page(page, wrapper);
    }

    @Override
    public SystemLog getLogDetail(Long id) {
        return getById(id);
    }

    @Override
    public List<SystemLog> getRecentLogsByUser(Long userId, int limit) {
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemLog::getUserId, userId)
               .orderByDesc(SystemLog::getCreateTime)
               .last("LIMIT " + limit);

        return list(wrapper);
    }

    @Override
    public List<Map<String, Object>> getModuleStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        return systemLogMapper.getModuleStats(tenantId, startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getUserStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return systemLogMapper.getUserStats(tenantId, startTime, endTime, limit);
    }

    @Override
    public List<Map<String, Object>> getOperationTypeStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        return systemLogMapper.getOperationTypeStats(tenantId, startTime, endTime);
    }

    @Override
    public List<SystemLog> exportLogs(Long tenantId, Long userId, String moduleName, 
                                     LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, SystemLog::getUserId, userId)
               .like(moduleName != null && !moduleName.isEmpty(), SystemLog::getModuleName, moduleName)
               .ge(startTime != null, SystemLog::getCreateTime, startTime)
               .le(endTime != null, SystemLog::getCreateTime, endTime)
               .orderByDesc(SystemLog::getCreateTime);

        return list(wrapper);
    }

    @Override
    public Integer cleanLogs(int days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SystemLog::getCreateTime, beforeTime);

        int deletedCount = systemLogMapper.delete(wrapper);
        log.info("清理历史日志完成，删除了{}条记录，保留{}天", deletedCount, days);
        return deletedCount;
    }

    @Override
    public void deleteLog(Long id) {
        removeById(id);
        log.info("删除日志记录: id={}", id);
    }

    @Override
    public void batchDeleteLogs(List<Long> ids) {
        removeBatchByIds(ids);
        log.info("批量删除日志记录: count={}", ids.size());
    }
}