package cn.aiedge.audit.service.impl;

import cn.aiedge.audit.mapper.AuditLogMapper;
import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.audit.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public Long record(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextId());
        }
        if (auditLog.getOperTime() == null) {
            auditLog.setOperTime(LocalDateTime.now());
        }
        if (auditLog.getCreateTime() == null) {
            auditLog.setCreateTime(LocalDateTime.now());
        }
        auditLogMapper.insert(auditLog);
        log.debug("记录审计日志: id={}, type={}, action={}", auditLog.getId(), auditLog.getAuditType(), auditLog.getAction());
        return auditLog.getId();
    }

    @Override
    @Async
    public void recordAsync(AuditLog auditLog) {
        record(auditLog);
    }

    @Override
    public Map<String, Object> query(Long tenantId, String auditType, String module,
                                       Long userId, LocalDateTime startTime, LocalDateTime endTime,
                                       int page, int pageSize) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(tenantId != null, AuditLog::getTenantId, tenantId)
                .eq(auditType != null && !auditType.isEmpty(), AuditLog::getAuditType, auditType)
                .eq(module != null && !module.isEmpty(), AuditLog::getModule, module)
                .eq(userId != null, AuditLog::getUserId, userId)
                .ge(startTime != null, AuditLog::getOperTime, startTime)
                .le(endTime != null, AuditLog::getOperTime, endTime)
                .orderByDesc(AuditLog::getOperTime);

        Page<AuditLog> p = auditLogMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("page", (int) p.getCurrent());
        result.put("pageSize", (int) p.getSize());
        result.put("records", p.getRecords());
        return result;
    }

    @Override
    public AuditLog getDetail(Long logId) {
        return auditLogMapper.selectById(logId);
    }

    @Override
    public List<AuditLog> getUserHistory(Long userId, int limit) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getUserId, userId)
                .orderByDesc(AuditLog::getOperTime)
                .last("LIMIT " + limit);
        return auditLogMapper.selectList(wrapper);
    }

    @Override
    public List<AuditLog> getObjectHistory(String targetType, String targetId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getTargetType, targetType)
                .eq(AuditLog::getTargetId, targetId)
                .orderByDesc(AuditLog::getOperTime);
        return auditLogMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getStatistics(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(tenantId != null, AuditLog::getTenantId, tenantId)
                .ge(startTime != null, AuditLog::getOperTime, startTime)
                .le(endTime != null, AuditLog::getOperTime, endTime);

        List<AuditLog> allLogs = auditLogMapper.selectList(wrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allLogs.size());

        long successCount = allLogs.stream().filter(l -> "SUCCESS".equals(l.getResult())).count();
        long failureCount = allLogs.stream().filter(l -> "FAILURE".equals(l.getResult())).count();
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);

        Map<String, Long> typeStats = allLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAuditType, Collectors.counting()));
        stats.put("typeStats", typeStats);

        Map<String, Long> moduleStats = allLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getModule() != null ? l.getModule() : "unknown", Collectors.counting()));
        stats.put("moduleStats", moduleStats);

        Map<Long, Long> userStats = allLogs.stream()
                .filter(l -> l.getUserId() != null)
                .collect(Collectors.groupingBy(AuditLog::getUserId, Collectors.counting()));
        stats.put("userStats", userStats);

        return stats;
    }

    @Override
    public List<AuditLog> exportLogs(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(tenantId != null, AuditLog::getTenantId, tenantId)
                .ge(startTime != null, AuditLog::getOperTime, startTime)
                .le(endTime != null, AuditLog::getOperTime, endTime)
                .orderByDesc(AuditLog::getOperTime);
        return auditLogMapper.selectList(wrapper);
    }

    @Override
    public int cleanLogs(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .lt(AuditLog::getOperTime, threshold);
        Long count = auditLogMapper.selectCount(wrapper);
        auditLogMapper.delete(wrapper);
        log.info("清理审计日志: 删除 {} 条", count);
        return count != null ? count.intValue() : 0;
    }
}
