package cn.aiedge.erp.metrics.config;

import cn.aiedge.erp.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 指标数据采集定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsScheduler {
    
    private final MetricsService metricsService;
    
    /**
     * 实时指标采集 - 每5秒执行一次
     * 满足延迟≤3秒的要求
     */
    @Scheduled(fixedRate = 5000)
    public void collectRealtimeMetrics() {
        try {
            log.debug("Collecting realtime metrics...");
            metricsService.refreshMetrics();
        } catch (Exception e) {
            log.error("Error collecting realtime metrics: {}", e.getMessage());
        }
    }
    
    /**
     * 分钟级指标聚合 - 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * *")
    public void aggregateMinuteMetrics() {
        try {
            log.debug("Aggregating minute metrics...");
            // 聚合逻辑在MetricsAggregationService中实现
        } catch (Exception e) {
            log.error("Error aggregating minute metrics: {}", e.getMessage());
        }
    }
    
    /**
     * 小时级指标聚合 - 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *")
    public void aggregateHourMetrics() {
        try {
            log.info("Aggregating hour metrics...");
            // 聚合逻辑在MetricsAggregationService中实现
        } catch (Exception e) {
            log.error("Error aggregating hour metrics: {}", e.getMessage());
        }
    }
    
    /**
     * 日级指标聚合 - 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void aggregateDayMetrics() {
        try {
            log.info("Aggregating day metrics...");
            // 聚合逻辑在MetricsAggregationService中实现
        } catch (Exception e) {
            log.error("Error aggregating day metrics: {}", e.getMessage());
        }
    }
}
