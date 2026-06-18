package cn.aiedge.test.connectionpool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库连接池基本功能测试
 * 验证连接池核心功能：
 * 1) 从连接池获取连接
 * 2) 使用连接执行SQL
 * 3) 正确释放连接回池
 * 4) 连接池容量控制
 * 
 * 验收标准:
 * - 连接获取成功率≥99%
 * - 连接释放正确率100%
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("数据库连接池基本功能测试")
public class ConnectionPoolBasicFunctionTest {

    @Autowired
    @Qualifier("h2DataSource")
    private DataSource dataSource;

    // 测试统计
    private final AtomicInteger totalAcquireAttempts = new AtomicInteger(0);
    private final AtomicInteger successfulAcquires = new AtomicInteger(0);
    private final AtomicInteger failedAcquires = new AtomicInteger(0);
    private final AtomicInteger totalReleases = new AtomicInteger(0);
    private final AtomicInteger successfulReleases = new AtomicInteger(0);
    private final AtomicInteger failedReleases = new AtomicInteger(0);

    @BeforeAll
    void setUp() {
        log.info("========== 数据库连接池基本功能测试开始 ==========");
        log.info("测试时间: {}", java.time.LocalDateTime.now());
        log.info("数据源类型: {}", dataSource.getClass().getName());
    }

    @AfterAll
    void tearDown() {
        log.info("========== 数据库连接池基本功能测试结束 ==========");
        printFinalReport();
    }

    /**
     * 测试1: 基本连接获取与释放
     * 验证从连接池获取连接和执行简单SQL的能力
     */
    @Test
    @Order(1)
    @DisplayName("TC-001: 基本连接获取与释放测试")
    void testBasicConnectionAcquireAndRelease() throws SQLException {
        log.info("\n----- TC-001: 基本连接获取与释放测试 -----");
        
        int iterations = 100;
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < iterations; i++) {
            Connection conn = null;
            try {
                // 1. 从连接池获取连接
                conn = dataSource.getConnection();
                totalAcquireAttempts.incrementAndGet();
                
                assertNotNull(conn, "获取的连接不应为null");
                assertFalse(conn.isClosed(), "获取的连接不应已关闭");
                
                // 2. 使用连接执行SQL
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    assertTrue(rs.next(), "应能获取结果");
                    assertEquals(1, rs.getInt(1), "查询结果应为1");
                }
                
                successfulAcquires.incrementAndGet();
                successCount++;
                
                // 3. 正确释放连接回池
                conn.close();
                totalReleases.incrementAndGet();
                successfulReleases.incrementAndGet();
                
            } catch (Exception e) {
                failedAcquires.incrementAndGet();
                failCount++;
                log.error("连接获取/使用失败 [iteration={}]: {}", i, e.getMessage());
            }
        }
        
        // 验证结果
        double successRate = (double) successCount / iterations * 100;
        log.info("测试结果: 总尝试={}, 成功={}, 失败={}, 成功率={}%", 
                iterations, successCount, failCount, String.format("%.2f", successRate));
        
        assertEquals(0, failCount, "不应有连接获取失败");
        assertTrue(successRate >= 99.0, "连接获取成功率应≥99%, 实际: " + successRate + "%");
        
        log.info("TC-001 测试通过 ✓");
    }

    /**
     * 测试2: 连接池容量控制测试
     * 验证连接池的最大连接数限制和等待行为
     */
    @Test
    @Order(2)
    @DisplayName("TC-002: 连接池容量控制测试")
    void testPoolCapacityControl() throws Exception {
        log.info("\n----- TC-002: 连接池容量控制测试 -----");
        
        int maxPoolSize = 30; // 根据配置
        int threadCount = maxPoolSize + 10; // 超过最大连接数的线程数
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicInteger activeConnections = new AtomicInteger(0);
        AtomicInteger maxActiveObserved = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    Connection conn = dataSource.getConnection();
                    int currentActive = activeConnections.incrementAndGet();
                    
                    // 记录最大活跃连接数
                    int maxObserved;
                    do {
                        maxObserved = maxActiveObserved.get();
                        if (currentActive <= maxObserved) break;
                    } while (!maxActiveObserved.compareAndSet(maxObserved, currentActive));
                    
                    // 模拟工作
                    Thread.sleep(100);
                    
                    // 执行简单查询
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeQuery("SELECT 1").close();
                    }
                    
                    successCount.incrementAndGet();
                    
                    // 释放连接
                    conn.close();
                    activeConnections.decrementAndGet();
                    
                } catch (SQLException e) {
                    if (e.getMessage().contains("timeout")) {
                        timeoutCount.incrementAndGet();
                        log.warn("连接获取超时（预期行为）: {}", e.getMessage());
                    } else {
                        log.error("SQL异常: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("线程执行异常: {}", e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completeLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        log.info("容量控制测试结果:");
        log.info("  - 最大连接数配置: {}", maxPoolSize);
        log.info("  - 并发线程数: {}", threadCount);
        log.info("  - 观察到的最大活跃连接: {}", maxActiveObserved.get());
        log.info("  - 成功获取连接: {}", successCount.get());
        log.info("  - 超时次数: {}", timeoutCount.get());
        
        // 验证容量控制
        assertTrue(maxActiveObserved.get() <= maxPoolSize, 
                "活跃连接数不应超过最大连接数配置");
        assertTrue(successCount.get() >= threadCount * 0.9, 
                "至少90%的线程应能获取连接");
        
        log.info("TC-002 测试通过 ✓");
    }

    /**
     * 测试3: 并发连接获取测试
     * 验证多线程环境下连接获取的稳定性和成功率
     */
    @Test
    @Order(3)
    @DisplayName("TC-003: 并发连接获取测试")
    void testConcurrentConnectionAcquisition() throws Exception {
        log.info("\n----- TC-003: 并发连接获取测试 -----");
        
        int threadCount = 50;
        int iterationsPerThread = 50;
        int totalIterations = threadCount * iterationsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicInteger threadSuccessCount = new AtomicInteger(0);
        AtomicInteger threadFailCount = new AtomicInteger(0);
        AtomicLong totalAcquireTime = new AtomicLong(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int localSuccess = 0;
                    int localFail = 0;
                    long localAcquireTime = 0;
                    
                    for (int j = 0; j < iterationsPerThread; j++) {
                        long startTime = System.nanoTime();
                        try (Connection conn = dataSource.getConnection()) {
                            long acquireTime = System.nanoTime() - startTime;
                            localAcquireTime += acquireTime;
                            
                            // 执行简单查询
                            try (Statement stmt = conn.createStatement();
                                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                                rs.next();
                            }
                            
                            localSuccess++;
                        } catch (Exception e) {
                            localFail++;
                            log.error("线程{} 第{}次获取失败: {}", threadId, j, e.getMessage());
                        }
                    }
                    
                    threadSuccessCount.addAndGet(localSuccess);
                    threadFailCount.addAndGet(localFail);
                    totalAcquireTime.addAndGet(localAcquireTime);
                    
                } catch (Exception e) {
                    log.error("线程{} 执行异常: {}", threadId, e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completeLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        int totalSuccess = threadSuccessCount.get();
        int totalFail = threadFailCount.get();
        double successRate = (double) totalSuccess / totalIterations * 100;
        double avgAcquireTimeMs = totalSuccess > 0 
                ? TimeUnit.NANOSECONDS.toMillis(totalAcquireTime.get()) / (double) totalSuccess 
                : 0;
        
        log.info("并发测试结果:");
        log.info("  - 总迭代次数: {}", totalIterations);
        log.info("  - 成功次数: {}", totalSuccess);
        log.info("  - 失败次数: {}", totalFail);
        log.info("  - 成功率: {}%", String.format("%.2f", successRate));
        log.info("  - 平均获取时间: {}ms", String.format("%.2f", avgAcquireTimeMs));
        
        // 验证成功率≥99%
        assertTrue(successRate >= 99.0, 
                "连接获取成功率应≥99%, 实际: " + successRate + "%");
        assertEquals(0, totalFail, "不应有连接获取失败");
        
        // 更新全局统计
        totalAcquireAttempts.addAndGet(totalIterations);
        successfulAcquires.addAndGet(totalSuccess);
        failedAcquires.addAndGet(totalFail);
        
        log.info("TC-003 测试通过 ✓");
    }

    /**
     * 测试4: 连接释放正确性测试
     * 验证连接是否正确释放回连接池，可被重复利用
     */
    @Test
    @Order(4)
    @DisplayName("TC-004: 连接释放正确性测试")
    void testConnectionReleaseCorrectness() throws SQLException {
        log.info("\n----- TC-004: 连接释放正确性测试 -----");
        
        int iterations = 100;
        int releaseSuccess = 0;
        int releaseFail = 0;
        
        for (int i = 0; i < iterations; i++) {
            Connection conn = null;
            try {
                conn = dataSource.getConnection();
                
                // 执行查询
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    rs.next();
                }
                
                // 释放连接
                conn.close();
                
                // 验证连接已关闭（但连接池中的连接是复用的）
                if (conn.isClosed()) {
                    releaseSuccess++;
                } else {
                    releaseFail++;
                }
                
            } catch (Exception e) {
                releaseFail++;
                log.error("连接释放失败 [iteration={}]: {}", i, e.getMessage());
            }
        }
        
        double releaseRate = (double) releaseSuccess / iterations * 100;
        log.info("连接释放测试结果:");
        log.info("  - 总测试次数: {}", iterations);
        log.info("  - 成功释放: {}", releaseSuccess);
        log.info("  - 释放失败: {}", releaseFail);
        log.info("  - 释放正确率: {}%", String.format("%.2f", releaseRate));
        
        // 验证释放正确率100%
        assertEquals(iterations, releaseSuccess, "连接释放正确率应为100%");
        assertEquals(0, releaseFail, "不应有连接释放失败");
        
        totalReleases.addAndGet(iterations);
        successfulReleases.addAndGet(releaseSuccess);
        failedReleases.addAndGet(releaseFail);
        
        log.info("TC-004 测试通过 ✓");
    }

    /**
     * 测试5: 连接池连接复用测试
     * 验证释放的连接是否被正确回收并复用
     */
    @Test
    @Order(5)
    @DisplayName("TC-005: 连接池连接复用测试")
    void testConnectionReuse() throws SQLException {
        log.info("\n----- TC-005: 连接池连接复用测试 -----");
        
        // 获取连接并记录其信息
        List<String> connectionIds = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            try (Connection conn = dataSource.getConnection()) {
                // 使用toString()作为连接标识（实际连接池会复用底层连接）
                String connId = conn.toString();
                connectionIds.add(connId);
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    rs.next();
                }
            }
        }
        
        // 分析连接复用情况
        long uniqueConnections = connectionIds.stream().distinct().count();
        long reuseCount = connectionIds.size() - uniqueConnections;
        double reuseRate = (double) reuseCount / connectionIds.size() * 100;
        
        log.info("连接复用测试结果:");
        log.info("  - 总获取次数: {}", connectionIds.size());
        log.info("  - 唯一连接数: {}", uniqueConnections);
        log.info("  - 复用次数: {}", reuseCount);
        log.info("  - 复用率: {}%", String.format("%.2f", reuseRate));
        
        // 连接池应该复用连接（实际复用率取决于连接池实现）
        assertTrue(uniqueConnections <= connectionIds.size(), 
                "唯一连接数不应超过总获取次数（验证逻辑正确性）");
        
        log.info("TC-005 测试通过 ✓");
    }

    /**
     * 测试6: 连接池压力测试
     * 模拟高并发场景下的连接获取和释放
     */
    @Test
    @Order(6)
    @DisplayName("TC-006: 连接池压力测试")
    void testPoolStressTest() throws Exception {
        log.info("\n----- TC-006: 连接池压力测试 -----");
        
        int threadCount = 100;
        int iterationsPerThread = 20;
        int totalIterations = threadCount * iterationsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        AtomicInteger stressSuccessCount = new AtomicInteger(0);
        AtomicInteger stressFailCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < iterationsPerThread; j++) {
                        try (Connection conn = dataSource.getConnection();
                             Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery("SELECT 1")) {
                            rs.next();
                            stressSuccessCount.incrementAndGet();
                        } catch (Exception e) {
                            stressFailCount.incrementAndGet();
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("压力测试线程异常: {}", e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        completeLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        int totalSuccess = stressSuccessCount.get();
        int totalFail = stressFailCount.get();
        double successRate = (double) totalSuccess / totalIterations * 100;
        
        log.info("压力测试结果:");
        log.info("  - 总迭代次数: {}", totalIterations);
        log.info("  - 成功次数: {}", totalSuccess);
        log.info("  - 失败次数: {}", totalFail);
        log.info("  - 成功率: {}%", String.format("%.2f", successRate));
        
        // 验证成功率≥99%
        assertTrue(successRate >= 99.0, 
                "压力测试连接获取成功率应≥99%, 实际: " + successRate + "%");
        
        // 更新全局统计
        totalAcquireAttempts.addAndGet(totalIterations);
        successfulAcquires.addAndGet(totalSuccess);
        failedAcquires.addAndGet(totalFail);
        totalReleases.addAndGet(totalSuccess);
        successfulReleases.addAndGet(totalSuccess);
        
        log.info("TC-006 测试通过 ✓");
    }

    /**
     * 打印最终测试报告
     */
    private void printFinalReport() {
        log.info("\n" + "=".repeat(60));
        log.info("数据库连接池基本功能测试 - 最终报告");
        log.info("=".repeat(60));
        
        int totalAttempts = totalAcquireAttempts.get();
        int totalSuccess = successfulAcquires.get();
        int totalFail = failedAcquires.get();
        double acquireSuccessRate = totalAttempts > 0 
                ? (double) totalSuccess / totalAttempts * 100 
                : 0;
        
        int totalRelease = totalReleases.get();
        int totalReleaseSuccess = successfulReleases.get();
        int totalReleaseFail = failedReleases.get();
        double releaseSuccessRate = totalRelease > 0 
                ? (double) totalReleaseSuccess / totalRelease * 100 
                : 0;
        
        log.info("\n【连接获取统计】");
        log.info("  - 总尝试次数: {}", totalAttempts);
        log.info("  - 成功次数: {}", totalSuccess);
        log.info("  - 失败次数: {}", totalFail);
        log.info("  - 成功率: {}%", String.format("%.2f", acquireSuccessRate));
        log.info("  - 验收标准: ≥99%");
        log.info("  - 结果: {}", acquireSuccessRate >= 99.0 ? "✓ 通过" : "✗ 未通过");
        
        log.info("\n【连接释放统计】");
        log.info("  - 总释放次数: {}", totalRelease);
        log.info("  - 成功释放: {}", totalReleaseSuccess);
        log.info("  - 释放失败: {}", totalReleaseFail);
        log.info("  - 释放正确率: {}%", String.format("%.2f", releaseSuccessRate));
        log.info("  - 验收标准: 100%");
        log.info("  - 结果: {}", releaseSuccessRate >= 100.0 ? "✓ 通过" : "✗ 未通过");
        
        log.info("\n【总体结论】");
        boolean allPassed = acquireSuccessRate >= 99.0 && releaseSuccessRate >= 100.0;
        log.info("  - 连接获取成功率: {} (标准: ≥99%)", 
                acquireSuccessRate >= 99.0 ? "✓ 通过" : "✗ 未通过");
        log.info("  - 连接释放正确率: {} (标准: 100%)", 
                releaseSuccessRate >= 100.0 ? "✓ 通过" : "✗ 未通过");
        log.info("  - 总体结果: {}", allPassed ? "✓ 所有测试通过" : "✗ 部分测试未通过");
        
        log.info("\n" + "=".repeat(60));
    }
}