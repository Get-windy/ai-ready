package cn.aiedge.base.log;

import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.mapper.SysOperLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 高级日志查询服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedLogQueryServiceImpl implements AdvancedLogQueryService {

    private final SysOperLogMapper operLogMapper;
    private static final Long DEFAULT_TENANT_ID = 1L;

    @Override
    public LogQueryResponse query(LogQueryRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<SysOperLog> wrapper = buildQueryWrapper(request);

        // 分页查询
        Page<SysOperLog> pageParam = new Page<>(request.getPage(), request.getPageSize());
        
        // 排序
        if ("costTime".equals(request.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(request.getSortOrder()), SysOperLog::getCostTime);
        } else {
            wrapper.orderByDesc(SysOperLog::getOperTime);
        }

        Page<SysOperLog> result = operLogMapper.selectPage(pageParam, wrapper);

        // 构建响应
        LogQueryResponse.LogAggregation aggregation = buildAggregation(wrapper);

        return LogQueryResponse.builder()
                .total(result.getTotal())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .records(result.getRecords())
                .aggregation(aggregation)
                .build();
    }

    @Override
    public Page<SysOperLog> fullTextSearch(String keyword, int page, int pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new Page<>(page, pageSize);
        }

        Page<SysOperLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        
        // 全文检索：搜索请求参数、响应结果、错误信息
        String likePattern = "%" + keyword.trim() + "%";
        wrapper.and(w -> w
                .like(SysOperLog::getRequestParams, likePattern)
                .or().like(SysOperLog::getResponseResult, likePattern)
                .or().like(SysOperLog::getErrorMsg, likePattern)
                .or().like(SysOperLog::getAction, likePattern)
                .or().like(SysOperLog::getModule, likePattern)
        );
        
        wrapper.orderByDesc(SysOperLog::getOperTime);
        
        return operLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<SysOperLog> queryByIp(String ip, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        
        if (ip != null && !ip.isEmpty()) {
            wrapper.like(SysOperLog::getOperIp, ip);
        }
        if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }
        
        wrapper.orderByDesc(SysOperLog::getOperTime);
        wrapper.last("LIMIT 1000");
        
        return operLogMapper.selectList(wrapper);
    }

    @Override
    public List<SysOperLog> querySlowOperations(Long minCost, Long maxCost, int limit) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        
        if (minCost != null) {
            wrapper.ge(SysOperLog::getCostTime, minCost);
        }
        if (maxCost != null) {
            wrapper.le(SysOperLog::getCostTime, maxCost);
        }
        
        wrapper.orderByDesc(SysOperLog::getCostTime);
        wrapper.last("LIMIT " + limit);
        
        return operLogMapper.selectList(wrapper);
    }

    @Override
    public List<SysOperLog> queryFailedOperations(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        wrapper.eq(SysOperLog::getStatus, 1); // 失败状态
        
        if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }
        
        wrapper.orderByDesc(SysOperLog::getOperTime);
        wrapper.last("LIMIT " + limit);
        
        return operLogMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getLogSummary(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        
        if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }

        List<SysOperLog> logs = operLogMapper.selectList(wrapper);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOperations", logs.size());
        summary.put("successCount", logs.stream().filter(l -> l.getStatus() == 0).count());
        summary.put("failCount", logs.stream().filter(l -> l.getStatus() == 1).count());
        
        DoubleSummaryStatistics costStats = logs.stream()
                .filter(l -> l.getCostTime() != null)
                .mapToLong(SysOperLog::getCostTime)
                .summaryStatistics();
        
        summary.put("avgCostTime", costStats.getAverage());
        summary.put("maxCostTime", costStats.getMax());
        summary.put("minCostTime", costStats.getMin());
        
        // 模块分布
        Map<String, Long> moduleDist = new HashMap<>();
        logs.forEach(l -> {
            String module = l.getModule() != null ? l.getModule() : "unknown";
            moduleDist.merge(module, 1L, Long::sum);
        });
        summary.put("moduleDistribution", moduleDist);
        
        return summary;
    }

    @Override
    public List<Map<String, Object>> getOperationTrend(LocalDateTime startTime, LocalDateTime endTime, String timeUnit) {
        List<SysOperLog> logs = getLogsInTimeRange(startTime, endTime);
        
        Map<String, Long> trendMap = new LinkedHashMap<>();
        
        for (SysOperLog log : logs) {
            String timeKey = getTimeKey(log.getOperTime(), timeUnit);
            trendMap.merge(timeKey, 1L, Long::sum);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        trendMap.forEach((time, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("time", time);
            item.put("count", count);
            result.add(item);
        });
        
        return result;
    }

    @Override
    public Map<String, Object> getUserActivityAnalysis(Long userId, int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        wrapper.eq(SysOperLog::getUserId, userId);
        wrapper.ge(SysOperLog::getOperTime, startTime);
        wrapper.le(SysOperLog::getOperTime, endTime);

        List<SysOperLog> logs = operLogMapper.selectList(wrapper);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("userId", userId);
        analysis.put("totalOperations", logs.size());
        analysis.put("successRate", logs.isEmpty() ? 0 : 
                (double) logs.stream().filter(l -> l.getStatus() == 0).count() / logs.size() * 100);
        
        // 按模块统计
        Map<String, Long> moduleStats = new HashMap<>();
        logs.forEach(l -> {
            String module = l.getModule() != null ? l.getModule() : "unknown";
            moduleStats.merge(module, 1L, Long::sum);
        });
        analysis.put("moduleStats", moduleStats);
        
        // 活跃时段（按小时）
        Map<Integer, Long> hourlyActivity = new HashMap<>();
        logs.forEach(l -> {
            if (l.getOperTime() != null) {
                int hour = l.getOperTime().getHour();
                hourlyActivity.merge(hour, 1L, Long::sum);
            }
        });
        analysis.put("hourlyActivity", hourlyActivity);
        
        // 常用IP
        Map<String, Long> ipStats = new HashMap<>();
        logs.forEach(l -> {
            if (l.getOperIp() != null) {
                ipStats.merge(l.getOperIp(), 1L, Long::sum);
            }
        });
        analysis.put("ipStats", ipStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList());
        
        return analysis;
    }

    @Override
    public List<Map<String, Object>> detectAnomalousOperations(Long userId) {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // 检测频繁失败
        LambdaQueryWrapper<SysOperLog> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        failWrapper.eq(SysOperLog::getStatus, 1);
        failWrapper.ge(SysOperLog::getOperTime, oneHourAgo);
        if (userId != null) {
            failWrapper.eq(SysOperLog::getUserId, userId);
        }
        
        long failCount = operLogMapper.selectCount(failWrapper);
        if (failCount > 10) {
            Map<String, Object> anomaly = new HashMap<>();
            anomaly.put("type", "FREQUENT_FAILURES");
            anomaly.put("severity", "HIGH");
            anomaly.put("count", failCount);
            anomaly.put("message", "1小时内失败操作超过10次: " + failCount);
            anomalies.add(anomaly);
        }
        
        // 检测慢操作
        LambdaQueryWrapper<SysOperLog> slowWrapper = new LambdaQueryWrapper<>();
        slowWrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        slowWrapper.ge(SysOperLog::getCostTime, 5000L); // 超过5秒
        slowWrapper.ge(SysOperLog::getOperTime, oneHourAgo);
        if (userId != null) {
            slowWrapper.eq(SysOperLog::getUserId, userId);
        }
        
        long slowCount = operLogMapper.selectCount(slowWrapper);
        if (slowCount > 5) {
            Map<String, Object> anomaly = new HashMap<>();
            anomaly.put("type", "SLOW_OPERATIONS");
            anomaly.put("severity", "MEDIUM");
            anomaly.put("count", slowCount);
            anomaly.put("message", "1小时内慢操作(>5s)超过5次: " + slowCount);
            anomalies.add(anomaly);
        }
        
        return anomalies;
    }

    // ==================== 辅助方法 ====================

    private LambdaQueryWrapper<SysOperLog> buildQueryWrapper(LogQueryRequest request) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);

        // 用户过滤
        if (request.getUserId() != null) {
            wrapper.eq(SysOperLog::getUserId, request.getUserId());
        }
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            wrapper.like(SysOperLog::getUsername, request.getUsername());
        }

        // 模块过滤
        if (request.getModules() != null && !request.getModules().isEmpty()) {
            wrapper.in(SysOperLog::getModule, request.getModules());
        }

        // 操作过滤
        if (request.getAction() != null && !request.getAction().isEmpty()) {
            wrapper.like(SysOperLog::getAction, request.getAction());
        }

        // 状态过滤
        if (request.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, request.getStatus());
        }

        // 时间范围
        if (request.getStartTime() != null) {
            wrapper.ge(SysOperLog::getOperTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(SysOperLog::getOperTime, request.getEndTime());
        }

        // IP过滤
        if (request.getOperIp() != null && !request.getOperIp().isEmpty()) {
            wrapper.like(SysOperLog::getOperIp, request.getOperIp());
        }

        // 地点过滤
        if (request.getOperLocation() != null && !request.getOperLocation().isEmpty()) {
            wrapper.like(SysOperLog::getOperLocation, request.getOperLocation());
        }

        // 耗时范围
        if (request.getMinCostTime() != null) {
            wrapper.ge(SysOperLog::getCostTime, request.getMinCostTime());
        }
        if (request.getMaxCostTime() != null) {
            wrapper.le(SysOperLog::getCostTime, request.getMaxCostTime());
        }

        // 关键词搜索
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            String likePattern = "%" + request.getKeyword() + "%";
            wrapper.and(w -> w
                    .like(SysOperLog::getRequestParams, likePattern)
                    .or().like(SysOperLog::getErrorMsg, likePattern)
                    .or().like(SysOperLog::getAction, likePattern)
            );
        }

        // 请求方法和URL
        if (request.getRequestMethod() != null && !request.getRequestMethod().isEmpty()) {
            wrapper.eq(SysOperLog::getRequestMethod, request.getRequestMethod());
        }
        if (request.getRequestUrl() != null && !request.getRequestUrl().isEmpty()) {
            wrapper.like(SysOperLog::getRequestUrl, request.getRequestUrl());
        }

        return wrapper;
    }

    private LogQueryResponse.LogAggregation buildAggregation(LambdaQueryWrapper<SysOperLog> wrapper) {
        List<SysOperLog> logs = operLogMapper.selectList(wrapper);

        long totalCount = logs.size();
        long successCount = logs.stream().filter(l -> l.getStatus() == 0).count();
        long failCount = totalCount - successCount;

        DoubleSummaryStatistics costStats = logs.stream()
                .filter(l -> l.getCostTime() != null)
                .mapToLong(SysOperLog::getCostTime)
                .summaryStatistics();

        // 模块统计
        Map<String, Long> moduleCount = new HashMap<>();
        logs.forEach(l -> {
            String module = l.getModule() != null ? l.getModule() : "unknown";
            moduleCount.merge(module, 1L, Long::sum);
        });

        List<Map<String, Object>> moduleStats = new ArrayList<>();
        moduleCount.forEach((module, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("module", module);
            item.put("count", count);
            moduleStats.add(item);
        });
        moduleStats.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));

        // 用户统计
        Map<Long, Long> userCount = new HashMap<>();
        Map<Long, String> userNames = new HashMap<>();
        logs.forEach(l -> {
            userCount.merge(l.getUserId(), 1L, Long::sum);
            if (l.getUsername() != null) {
                userNames.putIfAbsent(l.getUserId(), l.getUsername());
            }
        });

        List<Map<String, Object>> userStats = new ArrayList<>();
        userCount.forEach((userId, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", userId);
            item.put("username", userNames.getOrDefault(userId, "unknown"));
            item.put("count", count);
            userStats.add(item);
        });
        userStats.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));

        return LogQueryResponse.LogAggregation.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failCount)
                .avgCostTime(costStats.getAverage())
                .maxCostTime(costStats.getMax())
                .minCostTime(costStats.getMin())
                .moduleStats(moduleStats)
                .userStats(userStats)
                .build();
    }

    private List<SysOperLog> getLogsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperLog::getTenantId, DEFAULT_TENANT_ID);
        
        if (startTime != null) {
            wrapper.ge(SysOperLog::getOperTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getOperTime, endTime);
        }
        
        return operLogMapper.selectList(wrapper);
    }

    private String getTimeKey(LocalDateTime time, String timeUnit) {
        if (time == null) return "unknown";
        
        return switch (timeUnit.toLowerCase()) {
            case "hour" -> String.format("%04d-%02d-%02d %02d:00", 
                    time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour());
            case "day" -> String.format("%04d-%02d-%02d", 
                    time.getYear(), time.getMonthValue(), time.getDayOfMonth());
            case "week" -> String.format("%04d-W%02d", 
                    time.getYear(), time.getDayOfYear() / 7 + 1);
            case "month" -> String.format("%04d-%02d", 
                    time.getYear(), time.getMonthValue());
            default -> time.toString();
        };
    }
}
