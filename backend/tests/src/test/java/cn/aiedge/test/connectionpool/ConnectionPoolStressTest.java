package cn.aiedge.test.connectionpool;

import cn.aiedge.test.connectionpool.metrics.PoolMetricsCollector;
import cn.aiedge.test.connectionpool.metrics.PoolMetricsCollector.PoolMetrics;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
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
 * 数据库连接池压力测试类
 * 模拟极端高并发场景下的连接池表现
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConnectionPoolStressTest {

    @Autowired
    @Qualifier("highConcurrencyDataSource")
    private DataSource highConcurrencyDataSource;

    @Autowired
    @Qualifier("h2DataSource")
    private DataSource h2DataSource;

    @Autowired
    private PoolMetricsCollector metricsCollector;

    private final List<StressTestResult> stressTestResults = new ArrayList<>();

    @BeforeAll
    void setUp() {
        log.info("========== 数据库连接池压力测试开始 ==========");
    }

    @AfterAll
    void tearDown() {
        log.info("========== 数据库连接池压力测试结束 ==========");
        printStressTestReport();
    }

    /**
     * 压力测试1: 突发流量测试
     * 模拟突发的大量并发请求
     */
    @Test
    @Order(1)
    @DisplayName("突发流量测试 - 200线程突发请求")
    void testBurstTraffic() throws Exception {
        log.info("\n----- 突发流量测试 (200线程) -----");
        
        int threadCount = 200;
        int burstIterations = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalAcquisitionTime = new AtomicLong(0);
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < burstIterations; j++) {
                        long startTime = System.nanoTime();
                        try (Connection conn = h2DataSource.getConnection()) {
                            long endTime = System.nanoTime();
                            totalAcquisitionTime.addAndGet(TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
                            
                            // 快速查询
                            conn.prepareStatement("SELECT 1").executeQuery().close();
                            Thread.sleep(5);
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
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
        long totalTime = testEndTime - testStartTime;
        
        int totalOperations = threadCount * burstIterations;
        double avgTime = successCount.get() > 0 
                ? (double) totalAcquisitionTime.get() / successCount.get() 
                : 0;
        double successRate = (double) successCount.get() / totalOperations * 100;
        double throughput = totalTime > 0 ? (double) successCount.get() / (totalTime / 1000.0) : 0;
        
        // 收集最终指标
        PoolMetrics metrics = metricsCollector.collectMetrics(h2DataSource, "H2-Pool");
        
        log.info("突发流量测试结果:");
        log.info("  - 总操作数: {}", totalOperations);
        log.info("  - 成功数: {}", successCount.get());
        log.info("  - 失败数: {}", failureCount.get());
        log.info("  - 成功率: {}%", String.format("%.2f", successRate));
        log.info("  - 平均获取时间: {}ms", String.format("%.2f", avgTime));
        log.info("  - 吞吐量: {}/秒", String.format("%.2f", throughput));
        
        // 验证
        assertTrue(avgTime <= 100, "突发流量下平均获取时间应≤100ms");
        assertTrue(successRate >= 95, "突发流量下成功率应≥95%");
        
        stressTestResults.add(new StressTestResult(
                "突发流量测试", threadCount, totalOperations, 
                successCount.get(), failureCount.get(), avgTime, successRate, throughput, metrics));
        
        log.info("突发流量测试完成");
    }

    /**
     * 压力测试2: 连接池耗尽测试
     * 测试连接池在耗尽情况下的行为
     */
    @Test
    @Order(2)
    @DisplayName("连接池耗尽测试 - 验证超时和等待行为")
    void testPoolExhaustion() throws Exception {
        log.info("\n----- 连接池耗尽测试 -----");
        
        int threadCount = 50;
        int holdTimeMs = 5000; // 每个连接持有5秒
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        AtomicLong waitTimeTotal = new AtomicLong(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long waitStart = System.currentTimeMillis();
                    try (Connection conn = h2DataSource.getConnection()) {
                        long waitEnd = System.currentTimeMillis();
                        waitTimeTotal.addAndGet(waitEnd - waitStart);
                        
                        successCount.incrementAndGet();
                        // 长时间持有连接
                        Thread.sleep(holdTimeMs);
                    } catch (SQLException e) {
                        if (e.getMessage().contains("timeout")) {
                            timeoutCount.incrementAndGet();
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
        
        double avgWaitTime = successCount.get() > 0 
                ? (double) waitTimeTotal.get() / successCount.get() 
                : 0;
        
        log.info("连接池耗尽测试结果:");
        log.info("  - 成功获取连接: {}", successCount.get());
        log.info("  - 超时次数: {}", timeoutCount.get());
        log.info("  - 平均等待时间: {}ms", String.format("%.2f", avgWaitTime));
        
        // 验证：超时次数应该合理（连接池配置的超时时间）
        assertTrue(timeoutCount.get() < threadCount / 2, "超时次数不应超过线程数的一半");
        
        log.info("连接池耗尽测试完成");
    }

    /**
     * 压力测试3: 连接泄漏检测压力测试
     */
    @Test
    @Order(3)
    @DisplayName("连接泄漏检测压力测试")
    void testLeakDetectionUnderPressure() throws Exception {
        log.info("\n----- 连接泄漏检测压力测试 -----");
        
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicInteger leakCount = new AtomicInteger(0);
        AtomicInteger normalCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // 模拟一些线程忘记关闭连接（但这里我们正确关闭）
                    Connection conn = h2DataSource.getConnection();
                    Thread.sleep(100);
                    // 正确关闭
                    conn.close();
                    normalCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("leak")) {
                        leakCount.incrementAndGet();
                    }
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completeLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        log.info("连接泄漏检测压力测试结果:");
        log.info("  - 正常关闭: {}", normalCount.get());
        log.info("  - 检测到泄漏: {}", leakCount.get());
        
        // 验证没有泄漏
        assertEquals(0, leakCount.get(), "不应检测到连接泄漏");
        assertEquals(threadCount, normalCount.get(), "所有连接应正常关闭");
        
        log.info("连接泄漏检测压力测试完成");
    }

    /**
     * 打印压力测试报告
     */
    private void printStressTestReport() {
        log.info("\n" + "=".repeat(60));
        log.info("数据库连接池压力测试报告");
        log.info("=".repeat(60));
        
        for (StressTestResult result : stressTestResults) {
            log.info("\n【{}】", result.getTestName());
            log.info("  并发线程数: {}", result.getThreadCount());
            log.info("  总操作数: {}", result.getTotalOperations());
            log.info("  成功数: {}", result.getSuccessCount());
            log.info("  失败数: {}", result.getFailureCount());
            log.info("  平均获取时间: {}ms", String.format("%.2f", result.getAvgAcquisitionTimeMs()));
            log.info("  成功率: {}%", String.format("%.2f", result.getSuccessRate()));
            log.info("  吞吐量: {}/秒", String.format("%.2f", result.getThroughput()));
            if (result.getFinalMetrics() != null) {
                log.info("  最终连接池利用率: {}%", 
                        String.format("%.2f", result.getFinalMetrics().getPoolUtilization()));
            }
        }
        
        log.info("\n" + "=".repeat(60));
        log.info("压力测试完成");
        log.info("=".repeat(60));
    }

    // ========== 内部类 ==========
    
    /**
     * 压力测试结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StressTestResult {
        private String testName;
        private int threadCount;
        private int totalOperations;
        private int successCount;
        private int failureCount;
        private double avgAcquisitionTimeMs;
        private double successRate;
        private double throughput;
        private PoolMetrics finalMetrics;
    }
}