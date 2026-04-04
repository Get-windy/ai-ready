package cn.aiedge.notification.controller;

import cn.aiedge.notification.cache.NotificationTemplateCache;
import cn.aiedge.notification.channel.NotificationChannel;
import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.limiter.NotificationRateLimiter;
import cn.aiedge.notification.service.NotificationService;
import cn.aiedge.notification.service.impl.EnhancedNotificationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 通知统计与健康检查控制器
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/notification-stats")
@RequiredArgsConstructor
@Tag(name = "通知统计", description = "统计数据与健康检查接口")
public class NotificationStatsController {

    private final NotificationService notificationService;
    private final List<NotificationChannel> channels;
    private final NotificationRateLimiter rateLimiter;
    private final NotificationTemplateCache templateCache;
    private final EnhancedNotificationServiceImpl.NotificationRecordRepository recordRepository;

    /**
     * 获取各渠道状态
     */
    @GetMapping("/channels")
    @Operation(summary = "获取渠道状态")
    public ResponseEntity<List<ChannelStatus>> getChannelStatus() {
        List<ChannelStatus> statuses = new ArrayList<>();
        
        for (NotificationChannel channel : channels) {
            int availableTokens = rateLimiter.getAvailableTokens(channel.getChannelType());
            
            statuses.add(new ChannelStatus(
                channel.getChannelType(),
                channel.isAvailable(),
                availableTokens,
                channel.getClass().getSimpleName()
            ));
        }
        
        return ResponseEntity.ok(statuses);
    }

    /**
     * 获取发送统计
     */
    @GetMapping("/summary")
    @Operation(summary = "获取发送统计摘要")
    public ResponseEntity<SendSummary> getSendSummary() {
        // 统计各状态数量
        int pending = recordRepository.findByStatus(NotificationRecord.STATUS_PENDING).size();
        int sending = recordRepository.findByStatus(NotificationRecord.STATUS_SENDING).size();
        int success = recordRepository.findByStatus(NotificationRecord.STATUS_SUCCESS).size();
        int failed = recordRepository.findByStatus(NotificationRecord.STATUS_FAILED).size();
        
        int total = pending + sending + success + failed;
        double successRate = total > 0 ? (double) success / total * 100 : 0;
        
        return ResponseEntity.ok(new SendSummary(
            total, pending, sending, success, failed, 
            Math.round(successRate * 100.0) / 100.0
        ));
    }

    /**
     * 获取每日发送统计
     */
    @GetMapping("/daily")
    @Operation(summary = "获取每日发送统计")
    public ResponseEntity<List<DailyStats>> getDailyStats(
            @RequestParam(defaultValue = "7") int days) {
        
        List<DailyStats> stats = new ArrayList<>();
        LocalDateTime endTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        // 模拟数据（实际应从数据库查询）
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime dayStart = endTime.minusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            
            // 实际应该用SQL查询按日期分组统计
            // 这里简化处理
            stats.add(new DailyStats(
                dayStart.toLocalDate(),
                0, 0, 0, 0  // 实际数据需要从数据库查询
            ));
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取模板使用统计
     */
    @GetMapping("/templates")
    @Operation(summary = "获取模板使用统计")
    public ResponseEntity<List<TemplateUsageStats>> getTemplateUsageStats() {
        List<NotificationTemplateCache.CacheStats> cacheStats = 
            Collections.singletonList(templateCache.getStats());
        
        return ResponseEntity.ok(cacheStats.stream()
            .map(s -> new TemplateUsageStats(
                s.total(), s.enabled(), s.expired()
            ))
            .toList());
    }

    /**
     * 获取限流状态
     */
    @GetMapping("/rate-limit")
    @Operation(summary = "获取限流状态")
    public ResponseEntity<Map<String, Integer>> getRateLimitStatus() {
        Map<String, Integer> status = new HashMap<>();
        
        for (NotificationChannel channel : channels) {
            status.put(channel.getChannelType(), 
                rateLimiter.getAvailableTokens(channel.getChannelType()));
        }
        
        return ResponseEntity.ok(status);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<HealthCheckResult> healthCheck() {
        List<String> issues = new ArrayList<>();
        boolean healthy = true;
        
        // 检查渠道可用性
        for (NotificationChannel channel : channels) {
            if (!channel.isAvailable()) {
                issues.add("渠道不可用: " + channel.getChannelType());
                healthy = false;
            }
        }
        
        // 检查是否有过多失败记录
        int failed = recordRepository.findByStatus(NotificationRecord.STATUS_FAILED).size();
        if (failed > 100) {
            issues.add("大量发送失败记录: " + failed);
        }
        
        return ResponseEntity.ok(new HealthCheckResult(
            healthy,
            channels.size(),
            templateCache.getStats().total(),
            issues
        ));
    }

    /**
     * 处理失败记录重试
     */
    @PostMapping("/retry-failed")
    @Operation(summary = "重试所有失败记录")
    public ResponseEntity<RetryResult> retryFailed() {
        List<NotificationRecord> failedRecords = 
            recordRepository.findByStatus(NotificationRecord.STATUS_FAILED);
        
        int retried = 0;
        int skipped = 0;
        
        for (NotificationRecord record : failedRecords) {
            if (notificationService.retry(record.getId())) {
                retried++;
            } else {
                skipped++;
            }
        }
        
        return ResponseEntity.ok(new RetryResult(failedRecords.size(), retried, skipped));
    }

    // ==================== DTO ====================

    public record ChannelStatus(
        String channelType,
        boolean available,
        int availableTokens,
        String implementation
    ) {}

    public record SendSummary(
        int total,
        int pending,
        int sending,
        int success,
        int failed,
        double successRate
    ) {}

    public record DailyStats(
        LocalDate date,
        int total,
        int success,
        int failed,
        int siteMessages
    ) {}

    public record TemplateUsageStats(
        int totalTemplates,
        int enabledTemplates,
        int cacheExpired
    ) {}

    public record HealthCheckResult(
        boolean healthy,
        int channelCount,
        int templateCount,
        List<String> issues
    ) {}

    public record RetryResult(
        int totalFailed,
        int retried,
        int skipped
    ) {}
}
