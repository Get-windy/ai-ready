package cn.aiedge.erp.monitor.config;

import cn.aiedge.erp.monitor.service.MetricCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;

/**
 * 指标采集定时任务配置
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MetricCollectorConfig {

    private final MetricCollectorService metricCollectorService;

    /**
     * 每分钟采集一次实时指标
     */
    @Scheduled(fixedRate = 60000)
    public void collectRealTimeMetrics() {
        log.debug("Starting scheduled metric collection...");
        try {
            Long tenantId = 1L; // 默认租户ID
            List<String> metricTypes = Arrays.asList("order", "inventory", "user", "sales");
            int count = metricCollectorService.batchCollectAndSave(tenantId, metricTypes);
            log.debug("Collected and saved {} metrics", count);
        } catch (Exception e) {
            log.error("Failed to collect metrics", e);
        }
    }

    /**
     * 每5分钟采集一次详细指标
     */
    @Scheduled(fixedRate = 300000)
    public void collectDetailedMetrics() {
        log.debug("Starting detailed metric collection...");
        try {
            Long tenantId = 1L;
            List<String> metricTypes = Arrays.asList("order", "inventory", "user", "sales", "purchase", "finance");
            int count = metricCollectorService.batchCollectAndSave(tenantId, metricTypes);
            log.debug("Collected and saved {} detailed metrics", count);
        } catch (Exception e) {
            log.error("Failed to collect detailed metrics", e);
        }
    }
}
