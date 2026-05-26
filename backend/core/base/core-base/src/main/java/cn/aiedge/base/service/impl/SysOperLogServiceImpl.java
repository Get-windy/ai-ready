package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.mapper.SysOperLogMapper;
import cn.aiedge.base.service.SysOperLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 操作日志服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    // 默认租户ID
    private static final Long DEFAULT_TENANT_ID = 1L;

    @Override
    public Long recordLog(SysOperLog log) {
        if (log.getTenantId() == null) {
            log.setTenantId(DEFAULT_TENANT_ID);
        }
        if (log.getOperTime() == null) {
            log.setOperTime(LocalDateTime.now());
        }
        save(log);
        return log.getId();
    }

    @Override
    @Async
    public void recordLogAsync(SysOperLog log) {
        recordLog(log);
    }

    @Override
    public Page<SysOperLog> pageLogs(Page<SysOperLog> page, Long tenantId, Long userId,
                                      String module, Integer status,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, tenantId);
        
        if (userId != null) {
            wrapper.eq(SysOperLog::getUserId, userId);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }
        
        wrapper.orderByDesc(SysOperLog::getOperTime);
        
        return page(page, wrapper);
    }

    @Override
    public List<SysOperLog> getRecentLogs(Long userId, int limit) {
        return baseMapper.selectRecentLogsByUserId(userId, limit);
    }

    @Override
    public List<Map<String, Object>> getModuleStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        List<SysOperLog> logs = list(new LambdaQueryWrapper<SysOperLog>()
            .eq(SysOperLog::getTenantId, tenantId)
            .ge(startTime != null, SysOperLog::getOperTime, startTime)
            .le(endTime != null, SysOperLog::getOperTime, endTime));
        
        Map<String, Long> stats = new HashMap<>();
        for (SysOperLog log : logs) {
            String module = log.getModule() != null ? log.getModule() : "unknown";
            stats.merge(module, 1L, Long::sum);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : stats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("module", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<SysOperLog> logs = list(new LambdaQueryWrapper<SysOperLog>()
            .eq(SysOperLog::getTenantId, tenantId)
            .ge(startTime != null, SysOperLog::getOperTime, startTime)
            .le(endTime != null, SysOperLog::getOperTime, endTime));
        
        Map<Long, Long> stats = new HashMap<>();
        Map<Long, String> userNames = new HashMap<>();
        
        for (SysOperLog log : logs) {
            Long userId = log.getUserId();
            stats.merge(userId, 1L, Long::sum);
            if (log.getUsername() != null) {
                userNames.putIfAbsent(userId, log.getUsername());
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : stats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", entry.getKey());
            item.put("username", userNames.getOrDefault(entry.getKey(), "unknown"));
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        
        return result;
    }

    @Override
    public int cleanLogs(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return baseMapper.cleanLogsBeforeDate(threshold.toString());
    }

    @Override
    public List<SysOperLog> exportLogs(Long tenantId, Long userId, String module,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, tenantId);
        
        if (userId != null) {
            wrapper.eq(SysOperLog::getUserId, userId);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }
        
        wrapper.orderByDesc(SysOperLog::getOperTime);
        
        return list(wrapper);
    }
}
