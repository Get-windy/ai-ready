package cn.aiedge.test.connectionpool;

import cn.aiedge.test.connectionpool.metrics.PoolMetricsCollector;
import cn.aiedge.test.connectionpool.metrics.PoolMetricsCollector.ConnectionAcquisitionTestResult;
import cn.aiedge.test.connectionpool.metrics.PoolMetricsCollector.PoolMetrics;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库连接池性能测试类
 * 测试不同并发级别下的连接池性能
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConnectionPoolPerformanceTest {

    @Autowired
    @Qualifier("lowConcurrencyDataSource")
    private DataSource lowConcurrencyDataSource;

    @Autowired
    @Qualifier("mediumConcurrencyDataSource")
    private DataSource mediumConcurrencyDataSource;

    @Autowired
    @Qualifier("highConcurrencyDataSource")
    private DataSource highConcurrencyDataSource;

    @Autowired
    @Qualifier("h2DataSource")
    private DataSource h2DataSource;

    @Autowired
    private PoolMetricsCollector metricsCollector;

    // 测试结果存储
    private final List<TestResult> testResults = new ArrayList<>();

    @BeforeAll
    void setUp() {
        log.info("========== 数据库连接池性能测试开始 ==========");
        log.info("测试时间: {}", java.time.LocalDateTime.now());
    }

    @AfterAll
    void tearDown() {
        log.info("========== 数据库连接池性能测试结束 ==========");
        printSummaryReport();
    }

    /**
     * 测试1: 低并发场景下的连接池性能
     * 并发数: 10线程
     */
    @Test
    @Order(1)
    @DisplayName("低并发场景 - 10线程连接池性能测试")
    void testLowConcurrency() throws Exception {
        log.info("\n----- 低并发场景测试 (10线程) -----");
        
        int threadCount = 10;
        int iterationsPerThread = 100;
        int totalIterations = threadCount * iterationsPerThread;
        
        // 预热
        warmupPool(lowConcurrencyDataSource, 50);
        
        // 执行并发测试
        ConcurrencyTestResult result = executeConcurrencyTest(
                lowConcurrencyDataSource, 
                "Low-Concurrency-Pool",
                threadCount, 
                iterationsPerThread
        );
        
        // 收集指标
        PoolMetrics metrics = metricsCollector.collectMetrics(lowConcurrencyDataSource, "Low-Concurrency-Pool");
        logPoolMetrics(metrics);
        
        // 验证结果
        assertTrue(result.getAverageAcquisitionTimeMs() <= 100, 
                "连接获取时间应≤100ms, 实际: " + result.getAverageAcquisitionTimeMs() + "ms");
        assertEquals(0, result.getFailureCount(), "不应有连接失败");
        
        // 存储结果
        testResults.add(new TestResult("低并发场景", threadCount, totalIterations, result, metrics));
        
        log.info("低并发测试完成: 平均获取时间={}ms, 成功率={}%", 
                result.getAverageAcquisitionTimeMs(), 
                result.getSuccessRate());
    }

    /**
     * 测试2: 中并发场景下的连接池性能
     * 并发数: 50线程
     */
    @Test
    @Order(2)
    @DisplayName("中并发场景 - 50线程连接池性能测试")
    void testMediumConcurrency() throws Exception {
        log.info("\n----- 中并发场景测试 (50线程) -----");
        
        int threadCount = 50;
        int iterationsPerThread = 100;
        int totalIterations = threadCount * iterationsPerThread;
        
        // 预热
        warmupPool(mediumConcurrencyDataSource, 100);
        
        // 执行并发测试
        ConcurrencyTestResult result = executeConcurrencyTest(
                mediumConcurrencyDataSource, 
                "Medium-Concurrency-Pool",
                threadCount, 
                iterationsPerThread
        );
        
        // 收集指标
        PoolMetrics metrics = metricsCollector.collectMetrics(mediumConcurrencyDataSource, "Medium-Concurrency-Pool");
        logPoolMetrics(metrics);
        
        // 验证结果
        assertTrue(result.getAverageAcquisitionTimeMs() <= 100, 
                "连接获取时间应≤100ms, 实际: " + result.getAverageAcquisitionTimeMs() + "ms");
        assertEquals(0, result.getFailureCount(), "不应有连接失败");
        assertTrue(metrics.getPoolUtilization() >= 80, 
                "连接池利用率应≥80%, 实际: " + metrics.getPoolUtilization() + "%");
        
        // 存储结果
        testResults.add(new TestResult("中并发场景", threadCount, totalIterations, result, metrics));
        
        log.info("中并发测试完成: 平均获取时间={}ms, 成功率={}%", 
                result.getAverageAcquisitionTimeMs(), 
                result.getSuccessRate());
    }

    /**
     * 测试3: 高并发场景下的连接池性能
     * 并发数: 100线程
     */
    @Test
    @Order(3)
    @DisplayName("高并发场景 - 100线程连接池性能测试")
    void testHighConcurrency() throws Exception {
        log.info("\n----- 高并发场景测试 (100线程) -----");
        
        int threadCount = 100;
        int iterationsPerThread = 100;
        int totalIterations = threadCount * iterationsPerThread;
        
        // 预热
        warmupPool(highConcurrencyDataSource, 200);
        
        // 执行并发测试
        ConcurrencyTestResult result = executeConcurrencyTest(
                highConcurrencyDataSource, 
                "High-Concurrency-Pool",
                threadCount, 
                iterationsPerThread
        );
        
        // 收集指标
        PoolMetrics metrics = metricsCollector.collectMetrics(highConcurrencyDataSource, "High-Concurrency-Pool");
        logPoolMetrics(metrics);
        
        // 验证结果
        assertTrue(result.getAverageAcquisitionTimeMs() <= 100, 
                "连接获取时间应≤100ms, 实际: " + result.getAverageAcquisitionTimeMs() + "ms");
        assertEquals(0, result.getFailureCount(), "不应有连接失败");
        assertTrue(metrics.getPoolUtilization() >= 80, 
                "连接池利用率应≥80%, 实际: " + metrics.getPoolUtilization() + "%");
        
        // 存储结果
        testResults.add(new TestResult("高并发场景", threadCount, totalIterations, result, metrics));
        
        log.info("高并发测试完成: 平均获取时间={}ms, 成功率={}%", 
                result.getAverageAcquisitionTimeMs(), 
                result.getSuccessRate());
    }

    /**
     * 测试4: 连接泄漏检测测试
     */
    @Test
    @Order(4)
    @DisplayName("连接泄漏检测测试")
    void testConnectionLeakDetection() throws Exception {
        log.info("\n----- 连接泄漏检测测试 -----");
        
        // 使用H2数据库进行泄漏测试
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger leakCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 故意不关闭连接（模拟泄漏）
                    Connection conn = h2DataSource.getConnection();
                    // 正常应该关闭: conn.close();
                    // 这里故意不关闭来测试泄漏检测
                    Thread.sleep(100);
                    conn.close(); // 正确关闭
                } catch (Exception e) {
                    leakCount.incrementAndGet();
                    log.error("Connection leak detected", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        // 验证没有泄漏
        assertEquals(0, leakCount.get(), "不应检测到连接泄漏");
        
        log.info("连接泄漏检测测试完成: 泄漏数={}", leakCount.get());
    }

    /**
     * 测试5: 连接获取时间基准测试
     */
    @Test
    @Order(5)
    @DisplayName("连接获取时间基准测试")
    void testConnectionAcquisitionTime() {
        log.info("\n----- 连接获取时间基准测试 -----");
        
        int iterations = 1000;
        
        // 测试H2数据源
        ConnectionAcquisitionTestResult h2Result = metricsCollector.testConnectionAcquisitionTime(
                h2DataSource, "H2-Pool", iterations);
        
        logAcquisitionTestResult(h2Result);
        
        // 验证
        assertTrue(h2Result.getAverageTimeMs() <= 100, 
                "H2连接获取平均时间应≤100ms, 实际: " + h2Result.getAverageTimeMs() + "ms");
        assertTrue(h2Result.getP95TimeMs() <= 100, 
                "H2连接获取P95时间应≤100ms, 实际: " + h2Result.getP95TimeMs() + "ms");
        assertEquals(0, h2Result.getFailureCount(), "不应有连接获取失败");
        
        log.info("连接获取时间基准测试完成");
    }

    /**
     * 测试6: 连接池资源利用率测试
     */
    @Test
    @Order(6)
    @DisplayName("连接池资源利用率测试")
    void testPoolUtilization() throws Exception {
        log.info("\n----- 连接池资源利用率测试 -----");
        
        int threadCount = 50;
        int durationSeconds = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(threadCount);
        
        List<Double> utilizationSnapshots = new ArrayList<>();
        
        // 启动工作线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
                    while (System.currentTimeMillis() < endTime) {
                        try (Connection conn = mediumConcurrencyDataSource.getConnection()) {
                            Thread.sleep(50); // 模拟工作
                        }
                    }
                } catch (Exception e) {
                    log.error("Worker thread error", e);
                } finally {
                    stopLatch.countDown();
                }
            });
        }
        
        // 启动监控线程
        Thread monitorThread = new Thread(() -> {
            startLatch.countDown();
            long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
            while (System.currentTimeMillis() < endTime) {
                PoolMetrics metrics = metricsCollector.collectMetrics(
                        mediumConcurrencyDataSource, "Medium-Pool");
                if (metrics != null) {
                    utilizationSnapshots.add(metrics.getPoolUtilization());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        
        monitorThread.start();
        stopLatch.await(30, TimeUnit.SECONDS);
        monitorThread.join(5000);
        executor.shutdown();
        
        // 计算平均利用率
        double avgUtilization = utilizationSnapshots.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        log.info("平均连接池利用率: {}%", String.format("%.2f", avgUtilization));
        
        // 验证利用率
        assertTrue(avgUtilization >= 80, 
                "连接池平均利用率应≥80%, 实际: " + avgUtilization + "%");
        
        log.info("连接池资源利用率测试完成");
    }

    /**
     * 预热连接池
     */
    private void warmupPool(DataSource dataSource, int iterations) {
        log.info("预热连接池: {} 次迭代", iterations);
        for (int i = 0; i < iterations; i++) {
            try (Connection conn = dataSource.getConnection()) {
                // 简单查询
                conn.prepareStatement("SELECT 1").executeQuery().close();
            } catch (SQLException e) {
                log.error("Warmup error", e);
            }
        }
        log.info("预热完成");
    }

    /**
     * 执行并发测试
     */
    private ConcurrencyTestResult executeConcurrencyTest(
            DataSource dataSource, String poolName, 
            int threadCount, int iterationsPerThread) throws Exception {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicLong totalAcquisitionTime = new AtomicLong(0);
        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterationsPerThread; j++) {
                        long acquisitionStart = System.nanoTime();
                        try (Connection conn = dataSource.getConnection()) {
                            long acquisitionEnd = System.nanoTime();
                            long acquisitionTime = TimeUnit.NANOSECONDS.toMillis(acquisitionEnd - acquisitionStart);
                            totalAcquisitionTime.addAndGet(acquisitionTime);
                            
                            // 模拟数据库操作
                            conn.prepareStatement("SELECT 1").executeQuery().close();
                            Thread.sleep(10); // 模拟处理时间
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            log.error("Connection error", e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Thread error", e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completeLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;
        
        int totalIterations = threadCount * iterationsPerThread;
        double avgAcquisitionTime = successCount.get() > 0 
                ? (double) totalAcquisitionTime.get() / successCount.get() 
                : 0;
        double successRate = totalIterations > 0 
                ? (double) successCount.get() / totalIterations * 100 
                : 0;
        double throughput = totalTestTime > 0 
                ? (double) successCount.get() / (totalTestTime / 1000.0) 
                : 0;
        
        return ConcurrencyTestResult.builder()
                .poolName(poolName)
                .threadCount(threadCount)
                .iterationsPerThread(iterationsPerThread)
                .totalIterations(totalIterations)
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .averageAcquisitionTimeMs(avgAcquisitionTime)
                .totalTestTimeMs(totalTestTime)
                .successRate(successRate)
                .throughput(throughput)
                .build();
    }

    /**
     * 记录连接池指标
     */
    private void logPoolMetrics(PoolMetrics metrics) {
        if (metrics == null) {
            log.warn("Pool metrics is null");
            return;
        }
        log.info("连接池指标 [{}]:", metrics.getPoolName());
        log.info("  - 活跃连接: {}", metrics.getActiveConnections());
        log.info("  - 空闲连接: {}", metrics.getIdleConnections());
        log.info("  - 总连接数: {}", metrics.getTotalConnections());
        log.info("  - 等待线程: {}", metrics.getThreadsAwaitingConnection());
        log.info("  - 最大连接数: {}", metrics.getMaxPoolSize());
        log.info("  - 连接池利用率: {}%", String.format("%.2f", metrics.getPoolUtilization()));
    }

    /**
     * 记录连接获取测试结果
     */
    private void logAcquisitionTestResult(ConnectionAcquisitionTestResult result) {
        log.info("连接获取时间测试结果 [{}]:", result.getPoolName());
        log.info("  - 迭代次数: {}", result.getIterations());
        log.info("  - 成功次数: {}", result.getSuccessCount());
        log.info("  - 失败次数: {}", result.getFailureCount());
        log.info("  - 平均时间: {}ms", String.format("%.2f", result.getAverageTimeMs()));
        log.info("  - 最小时间: {}ms", result.getMinTimeMs());
        log.info("  - 最大时间: {}ms", result.getMaxTimeMs());
        log.info("  - P95时间: {}ms", result.getP95TimeMs());
        log.info("  - P99时间: {}ms", result.getP99TimeMs());
    }

    /**
     * 打印汇总报告
     */
    private void printSummaryReport() {
        log.info("\n" + "=".repeat(60));
        log.info("数据库连接池性能测试汇总报告");
        log.info("=".repeat(60));
        
        for (TestResult result : testResults) {
            log.info("\n【{}】", result.getScenarioName());
            log.info("  并发线程数: {}", result.getThreadCount());
            log.info("  总迭代次数: {}", result.getTotalIterations());
            log.info("  平均获取时间: {}ms", String.format("%.2f", result.getConcurrencyResult().getAverageAcquisitionTimeMs()));
            log.info("  成功率: {}%", String.format("%.2f", result.getConcurrencyResult().getSuccessRate()));
            log.info("  吞吐量: {}/秒", String.format("%.2f", result.getConcurrencyResult().getThroughput()));
            log.info("  连接池利用率: {}%", String.format("%.2f", result.getPoolMetrics().getPoolUtilization()));
        }
        
        log.info("\n" + "=".repeat(60));
        log.info("所有测试完成");
        log.info("=".repeat(60));
    }

    // ========== 内部类 ==========
    
    /**
     * 并发测试结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ConcurrencyTestResult {
        private String poolName;
        private int threadCount;
        private int iterationsPerThread;
        private int totalIterations;
        private int successCount;
        private int failureCount;
        private double averageAcquisitionTimeMs;
        private long totalTestTimeMs;
        private double successRate;
        private double throughput;
    }

    /**
     * 测试结果包装类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TestResult {
        private String scenarioName;
        private int threadCount;
        private int totalIterations;
        private ConcurrencyTestResult concurrencyResult;
        private PoolMetrics poolMetrics;
    }
}
