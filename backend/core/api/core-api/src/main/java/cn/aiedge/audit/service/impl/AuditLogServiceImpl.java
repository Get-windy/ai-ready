package cn.aiedge.audit.service.impl;

import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.audit.service.AuditLogService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 审计日志服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    // 使用内存存储（实际应使用数据库）
    private final List<AuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOGS = 10000;

    @Override
    public Long record(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(IdUtil.getSnowflakeNextId());
        }
        if (auditLog.getOperTime() == null) {
            auditLog.setOperTime(LocalDateTime.now());
        }
        if (auditLog.getCreateTime() == null) {
            auditLog.setCreateTime(LocalDateTime.now());
        }

        synchronized (auditLogs) {
            auditLogs.add(auditLog);
            // 限制数量
            if (auditLogs.size() > MAX_LOGS) {
                auditLogs.remove(0);
            }
        }

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
        List<AuditLog> filtered = new ArrayList<>();

        synchronized (auditLogs) {
            for (AuditLog auditLog : auditLogs) {
                if (tenantId != null && !tenantId.equals(auditLog.getTenantId())) continue;
                if (auditType != null && !auditType.equals(auditLog.getAuditType())) continue;
                if (module != null && !module.equals(auditLog.getModule())) continue;
                if (userId != null && !userId.equals(auditLog.getUserId())) continue;
                if (startTime != null && auditLog.getOperTime().isBefore(startTime)) continue;
                if (endTime != null && auditLog.getOperTime().isAfter(endTime)) continue;

                filtered.add(auditLog);
            }
        }

        // 按时间倒序
        filtered.sort((a, b) -> b.getOperTime().compareTo(a.getOperTime()));

        // 分页
        int total = filtered.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<AuditLog> pagedList = start < total ? filtered.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("records", pagedList);

        return result;
    }

    @Override
    public AuditLog getDetail(Long logId) {
        synchronized (auditLogs) {
            return auditLogs.stream()
                    .filter(l -> l.getId().equals(logId))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public List<AuditLog> getUserHistory(Long userId, int limit) {
        List<AuditLog> result = new ArrayList<>();

        synchronized (auditLogs) {
            for (int i = auditLogs.size() - 1; i >= 0 && result.size() < limit; i--) {
                AuditLog auditLog = auditLogs.get(i);
                if (userId.equals(auditLog.getUserId())) {
                    result.add(auditLog);
                }
            }
        }

        return result;
    }

    @Override
    public List<AuditLog> getObjectHistory(String targetType, String targetId) {
        List<AuditLog> result = new ArrayList<>();

        synchronized (auditLogs) {
            for (AuditLog auditLog : auditLogs) {
                if (targetType.equals(auditLog.getTargetType()) && targetId.equals(auditLog.getTargetId())) {
                    result.add(auditLog);
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getStatistics(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        List<AuditLog> filtered = new ArrayList<>();
        synchronized (auditLogs) {
            for (AuditLog auditLog : auditLogs) {
                if (tenantId != null && !tenantId.equals(auditLog.getTenantId())) continue;
                if (startTime != null && auditLog.getOperTime().isBefore(startTime)) continue;
                if (endTime != null && auditLog.getOperTime().isAfter(endTime)) continue;

                filtered.add(auditLog);
            }
        }

        // 总数统计
        stats.put("total", filtered.size());

        // 成功/失败统计
        long successCount = filtered.stream().filter(l -> "SUCCESS".equals(l.getResult())).count();
        long failureCount = filtered.stream().filter(l -> "FAILURE".equals(l.getResult())).count();
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);

        // 按类型统计
        Map<String, Long> typeStats = new HashMap<>();
        for (AuditLog auditLog : filtered) {
            typeStats.merge(auditLog.getAuditType(), 1L, Long::sum);
        }
        stats.put("typeStats", typeStats);

        // 按模块统计
        Map<String, Long> moduleStats = new HashMap<>();
        for (AuditLog auditLog : filtered) {
            String module = auditLog.getModule() != null ? auditLog.getModule() : "unknown";
            moduleStats.merge(module, 1L, Long::sum);
        }
        stats.put("moduleStats", moduleStats);

        // 按用户统计
        Map<Long, Long> userStats = new HashMap<>();
        for (AuditLog auditLog : filtered) {
            if (auditLog.getUserId() != null) {
                userStats.merge(auditLog.getUserId(), 1L, Long::sum);
            }
        }
        stats.put("userStats", userStats);

        return stats;
    }

    @Override
    public List<AuditLog> exportLogs(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        List<AuditLog> result = new ArrayList<>();

        synchronized (auditLogs) {
            for (AuditLog auditLog : auditLogs) {
                if (tenantId != null && !tenantId.equals(auditLog.getTenantId())) continue;
                if (startTime != null && auditLog.getOperTime().isBefore(startTime)) continue;
                if (endTime != null && auditLog.getOperTime().isAfter(endTime)) continue;

                result.add(auditLog);
            }
        }

        return result;
    }

    @Override
    public int cleanLogs(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);

        int count = 0;
        synchronized (auditLogs) {
            Iterator<AuditLog> iterator = auditLogs.iterator();
            while (iterator.hasNext()) {
                AuditLog auditLog = iterator.next();
                if (auditLog.getOperTime().isBefore(threshold)) {
                    iterator.remove();
                    count++;
                }
            }
        }

        log.info("清理审计日志: 删除 {} 条", count);
        return count;
    }
}
