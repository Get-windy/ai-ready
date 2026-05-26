package cn.aiedge.monitor.service;

import cn.aiedge.monitor.model.SystemMetrics;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 日志聚合服务
 * 将分散的日志聚合为结构化指标，支持查询和统计
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class LogAggregationService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String LOG_AGG_PREFIX = "log:agg:";
    private static final String ERROR_COUNT_KEY = LOG_AGG_PREFIX + "error:count";
    private static final String REQUEST_COUNT_KEY = LOG_AGG_PREFIX + "request:count";
    private static final String SLOW_REQUEST_KEY = LOG_AGG_PREFIX + "slow:request";
    private static final String API_STATS_KEY = LOG_AGG_PREFIX + "api:stats";
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    public LogAggregationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== 日志采集 ====================

    /**
     * 记录API请求日志
     */
    public void recordApiRequest(String apiPath, String method, long duration, 
                                  int statusCode, String userId) {
        String hourKey = LocalDateTime.now().format(HOUR_FORMATTER);
        
        // 1. 请求计数
        String countKey = REQUEST_COUNT_KEY + ":" + hourKey;
        redisTemplate.opsForHash().increment(countKey, apiPath + ":" + method, 1);
        redisTemplate.expire(countKey, 48, TimeUnit.HOURS);
        
        // 2. API统计
        String statsKey = API_STATS_KEY + ":" + hourKey;
        Map<String, Object> stats = new HashMap<>();
        stats.put("path", apiPath);
        stats.put("method", method);
        stats.put("duration", duration);
        stats.put("statusCode", statusCode);
        stats.put("userId", userId);
        stats.put("timestamp", System.currentTimeMillis());
        redisTemplate.opsForList().rightPush(statsKey, stats);
        redisTemplate.expire(statsKey, 48, TimeUnit.HOURS);
        
        // 3. 慢请求记录（超过1秒）
        if (duration > 1000) {
            String slowKey = SLOW_REQUEST_KEY + ":" + hourKey;
            Map<String, Object> slowInfo = new HashMap<>();
            slowInfo.put("path", apiPath);
            slowInfo.put("method", method);
            slowInfo.put("duration", duration);
            slowInfo.put("timestamp", System.currentTimeMillis());
            redisTemplate.opsForList().rightPush(slowKey, slowInfo);
            redisTemplate.expire(slowKey, 48, TimeUnit.HOURS);
        }
    }

    /**
     * 记录错误日志
     */
    public void recordError(String errorType, String errorMessage, 
                            String stackTrace, String module) {
        String hourKey = LocalDateTime.now().format(HOUR_FORMATTER);
        
        // 错误计数
        String countKey = ERROR_COUNT_KEY + ":" + hourKey;
        String field = module + ":" + errorType;
        redisTemplate.opsForHash().increment(countKey, field, 1);
        redisTemplate.expire(countKey, 48, TimeUnit.HOURS);
    }

    // ==================== 日志查询 ====================

    /**
     * 获取API请求统计
     */
    public ApiStatsResult getApiStats(String hourKey) {
        ApiStatsResult result = new ApiStatsResult();
        
        // 请求计数
        String countKey = REQUEST_COUNT_KEY + ":" + hourKey;
        Map<Object, Object> counts = redisTemplate.opsForHash().entries(countKey);
        result.setRequestCounts(counts.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(), 
                e -> ((Number) e.getValue()).longValue()
            )));
        
        // 错误计数
        String errorKey = ERROR_COUNT_KEY + ":" + hourKey;
        Map<Object, Object> errors = redisTemplate.opsForHash().entries(errorKey);
        result.setErrorCounts(errors.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(), 
                e -> ((Number) e.getValue()).longValue()
            )));
        
        // 慢请求数量
        String slowKey = SLOW_REQUEST_KEY + ":" + hourKey;
        Long slowCount = redisTemplate.opsForList().size(slowKey);
        result.setSlowRequestCount(slowCount != null ? slowCount : 0);
        
        return result;
    }

    /**
     * 获取最近N小时的请求趋势
     */
    public List<HourlyStats> getRequestTrend(int hours) {
        List<HourlyStats> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = hours - 1; i >= 0; i--) {
            LocalDateTime time = now.minusHours(i);
            String hourKey = time.format(HOUR_FORMATTER);
            
            HourlyStats stats = new HourlyStats();
            stats.setHour(time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")));
            
            // 统计总请求数
            String countKey = REQUEST_COUNT_KEY + ":" + hourKey;
            Map<Object, Object> counts = redisTemplate.opsForHash().entries(countKey);
            long total = counts.values().stream()
                .mapToLong(v -> ((Number) v).longValue())
                .sum();
            stats.setTotalRequests(total);
            
            // 统计总错误数
            String errorKey = ERROR_COUNT_KEY + ":" + hourKey;
            Map<Object, Object> errors = redisTemplate.opsForHash().entries(errorKey);
            long errorTotal = errors.values().stream()
                .mapToLong(v -> ((Number) v).longValue())
                .sum();
            stats.setTotalErrors(errorTotal);
            
            // 慢请求数
            String slowKey = SLOW_REQUEST_KEY + ":" + hourKey;
            Long slowCount = redisTemplate.opsForList().size(slowKey);
            stats.setSlowRequests(slowCount != null ? slowCount : 0);
            
            trend.add(stats);
        }
        
        return trend;
    }

    /**
     * 获取慢请求列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSlowRequests(String hourKey, int limit) {
        String slowKey = SLOW_REQUEST_KEY + ":" + hourKey;
        List<Object> rawList = redisTemplate.opsForList().range(slowKey, -limit, -1);
        
        if (rawList == null) return Collections.emptyList();
        
        return rawList.stream()
            .filter(obj -> obj instanceof Map)
            .map(obj -> (Map<String, Object>) obj)
            .sorted((a, b) -> {
                long da = ((Number) a.getOrDefault("duration", 0)).longValue();
                long db = ((Number) b.getOrDefault("duration", 0)).longValue();
                return Long.compare(db, da); // 按耗时降序
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ==================== 内部类 ====================

    @Data
    public static class ApiStatsResult {
        private Map<String, Long> requestCounts;
        private Map<String, Long> errorCounts;
        private long slowRequestCount;
    }

    @Data
    public static class HourlyStats {
        private String hour;
        private long totalRequests;
        private long totalErrors;
        private long slowRequests;
    }
}
