package cn.aiedge.test.connectionpool.metrics;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 连接池指标收集器
 * 用于收集和监控连接池的各项性能指标
 */
@Slf4j
@Component
public class PoolMetricsCollector {

    /**
     * 收集连接池指标
     */
    public PoolMetrics collectMetrics(DataSource dataSource, String poolName) {
        if (!(dataSource instanceof HikariDataSource)) {
            log.warn("DataSource is not HikariDataSource, cannot collect metrics");
            return null;
        }

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean mxBean = hikariDataSource.getHikariPoolMXBean();

        if (mxBean == null) {
            log.warn("HikariPoolMXBean is null, pool may not be initialized");
            return null;
        }

        return PoolMetrics.builder()
                .poolName(poolName)
                .activeConnections(mxBean.getActiveConnections())
                .idleConnections(mxBean.getIdleConnections())
                .totalConnections(mxBean.getTotalConnections())
                .threadsAwaitingConnection(mxBean.getThreadsAwaitingConnection())
                .maxPoolSize(hikariDataSource.getMaximumPoolSize())
                .minimumIdle(hikariDataSource.getMinimumIdle())
                .connectionTimeout(hikariDataSource.getConnectionTimeout())
                .idleTimeout(hikariDataSource.getIdleTimeout())
                .maxLifetime(hikariDataSource.getMaxLifetime())
                .leakDetectionThreshold(hikariDataSource.getLeakDetectionThreshold())
                .poolUtilization(calculatePoolUtilization(mxBean))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 计算连接池利用率
     */
    private double calculatePoolUtilization(HikariPoolMXBean mxBean) {
        int total = mxBean.getTotalConnections();
        int active = mxBean.getActiveConnections();
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) active / total * 100.0;
    }

    /**
     * 测试连接获取时间
     */
    public ConnectionAcquisitionTestResult testConnectionAcquisitionTime(
            DataSource dataSource, String poolName, int iterations) {
        
        long[] acquisitionTimes = new long[iterations];
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                var connection = dataSource.getConnection();
                long endTime = System.nanoTime();
                acquisitionTimes[i] = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                connection.close();
                successCount++;
            } catch (SQLException e) {
                acquisitionTimes[i] = -1;
                failureCount++;
                log.error("Failed to acquire connection", e);
            }
        }
        
        // 计算统计数据
        long[] validTimes = java.util.Arrays.stream(acquisitionTimes)
                .filter(t -> t >= 0)
                .toArray();
        
        if (validTimes.length == 0) {
            return ConnectionAcquisitionTestResult.builder()
                    .poolName(poolName)
                    .iterations(iterations)
                    .successCount(0)
                    .failureCount(iterations)
                    .averageTimeMs(0)
                    .minTimeMs(0)
                    .maxTimeMs(0)
                    .p95TimeMs(0)
                    .p99TimeMs(0)
                    .build();
        }
        
        java.util.Arrays.sort(validTimes);
        
        long sum = java.util.Arrays.stream(validTimes).sum();
        double average = (double) sum / validTimes.length;
        long min = validTimes[0];
        long max = validTimes[validTimes.length - 1];
        
        int p95Index = (int) Math.ceil(validTimes.length * 0.95) - 1;
        int p99Index = (int) Math.ceil(validTimes.length * 0.99) - 1;
        
        long p95 = validTimes[Math.max(0, p95Index)];
        long p99 = validTimes[Math.max(0, p99Index)];
        
        return ConnectionAcquisitionTestResult.builder()
                .poolName(poolName)
                .iterations(iterations)
                .successCount(successCount)
                .failureCount(failureCount)
                .averageTimeMs(average)
                .minTimeMs(min)
                .maxTimeMs(max)
                .p95TimeMs(p95)
                .p99TimeMs(p99)
                .acquisitionTimes(validTimes)
                .build();
    }

    /**
     * 连接池指标数据类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoolMetrics {
        private String poolName;
        private int activeConnections;
        private int idleConnections;
        private int totalConnections;
        private int threadsAwaitingConnection;
        private int maxPoolSize;
        private int minimumIdle;
        private long connectionTimeout;
        private long idleTimeout;
        private long maxLifetime;
        private long leakDetectionThreshold;
        private double poolUtilization;
        private long timestamp;
    }

    /**
     * 连接获取时间测试结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionAcquisitionTestResult {
        private String poolName;
        private int iterations;
        private int successCount;
        private int failureCount;
        private double averageTimeMs;
        private long minTimeMs;
        private long maxTimeMs;
        private long p95TimeMs;
        private long p99TimeMs;
        private long[] acquisitionTimes;
    }
}
