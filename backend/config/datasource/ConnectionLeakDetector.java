package com.qizhilian.datasource.monitor;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接池泄漏检测器
 * Sprint 27+1: 数据库连接池监控与优化
 */
@Slf4j
@Component
public class ConnectionLeakDetector implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    private final ConcurrentHashMap<String, LeakInfo> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalLeakCount = new AtomicInteger(0);
    private final AtomicInteger totalWarningCount = new AtomicInteger(0);

    // 泄漏阈值（毫秒）
    private static final long LEAK_THRESHOLD_MS = 5000;
    // 严重泄漏阈值
    private static final long CRITICAL_LEAK_MS = 30000;

    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void detectLeaks() {
        if (!(dataSource instanceof HikariDataSource)) {
            return;
        }

        HikariDataSource hikari = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikari.getHikariPoolMXBean();
        
        if (poolMXBean == null) {
            return;
        }

        int active = poolMXBean.getActiveConnections();
        int idle = poolMXBean.getIdleConnections();
        int waiting = poolMXBean.getThreadsAwaitingConnection();
        int total = poolMXBean.getTotalConnections();

        // 记录连接池状态
        log.debug("连接池状态 - 活跃: {}, 空闲: {}, 等待: {}, 总计: {}", 
                  active, idle, waiting, total);

        // 检测等待线程过多（潜在泄漏）
        if (waiting > 5) {
            log.warn("连接池告警: {} 个线程等待连接，可能存在连接泄漏", waiting);
            totalWarningCount.incrementAndGet();
        }

        // 检测连接使用率过高
        double usageRate = total > 0 ? (double) active / total : 0;
        if (usageRate > 0.9) {
            log.warn("连接池告警: 连接使用率 {:.1%}，接近上限", usageRate);
        }
    }

    @Override
    public Health health() {
        if (!(dataSource instanceof HikariDataSource)) {
            return Health.up().build();
        }

        HikariDataSource hikari = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikari.getHikariPoolMXBean();
        
        if (poolMXBean == null) {
            return Health.down().withDetail("error", "无法获取连接池MXBean").build();
        }

        int active = poolMXBean.getActiveConnections();
        int idle = poolMXBean.getIdleConnections();
        int waiting = poolMXBean.getThreadsAwaitingConnection();
        int total = poolMXBean.getTotalConnections();
        double usageRate = total > 0 ? (double) active / total : 0;

        Health.Builder builder = usageRate > 0.95 ? Health.down() : 
                                  usageRate > 0.8 ? Health.status("WARNING") : Health.up();

        return builder
                .withDetail("pool.name", hikari.getPoolName())
                .withDetail("active.connections", active)
                .withDetail("idle.connections", idle)
                .withDetail("total.connections", total)
                .withDetail("waiting.threads", waiting)
                .withDetail("usage.rate", String.format("%.2f", usageRate))
                .withDetail("total.leaks.detected", totalLeakCount.get())
                .withDetail("total.warnings", totalWarningCount.get())
                .build();
    }

    /**
     * 连接泄漏信息
     */
    private static class LeakInfo {
        private final String connectionId;
        private final Instant acquiredAt;
        private final StackTraceElement[] stackTrace;

        public LeakInfo(String connectionId, StackTraceElement[] stackTrace) {
            this.connectionId = connectionId;
            this.acquiredAt = Instant.now();
            this.stackTrace = stackTrace;
        }
    }
}