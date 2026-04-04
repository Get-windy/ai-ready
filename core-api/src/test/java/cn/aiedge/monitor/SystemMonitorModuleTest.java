package cn.aiedge.monitor;

import cn.aiedge.monitor.model.AlertRule;
import cn.aiedge.monitor.model.SystemMetrics;
import cn.aiedge.monitor.service.SystemMonitorService;
import cn.aiedge.monitor.service.impl.SystemMonitorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统监控模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("系统监控模块单元测试")
public class SystemMonitorModuleTest {

    private SystemMonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new SystemMonitorServiceImpl();
    }

    // ==================== 指标采集测试 ====================

    @Test
    @DisplayName("采集系统指标 - 成功")
    void testGetCurrentMetrics() {
        SystemMetrics metrics = monitorService.getCurrentMetrics();

        assertNotNull(metrics);
        assertNotNull(metrics.getCollectTime());
        assertNotNull(metrics.getCpuCores());
        assertTrue(metrics.getCpuCores() > 0);
    }

    @Test
    @DisplayName("采集内存指标 - 成功")
    void testMemoryMetrics() {
        SystemMetrics metrics = monitorService.getCurrentMetrics();

        assertNotNull(metrics.getTotalMemory());
        assertNotNull(metrics.getUsedMemory());
        assertNotNull(metrics.getMemoryUsage());

        assertTrue(metrics.getTotalMemory() > 0);
        assertTrue(metrics.getUsedMemory() >= 0);
        assertTrue(metrics.getMemoryUsage() >= 0 && metrics.getMemoryUsage() <= 100);
    }

    @Test
    @DisplayName("采集磁盘指标 - 成功")
    void testDiskMetrics() {
        SystemMetrics metrics = monitorService.getCurrentMetrics();

        assertNotNull(metrics.getTotalDisk());
        assertNotNull(metrics.getUsedDisk());
        assertNotNull(metrics.getDiskUsage());

        assertTrue(metrics.getTotalDisk() >= 0);
        assertTrue(metrics.getDiskUsage() >= 0 && metrics.getDiskUsage() <= 100);
    }

    @Test
    @DisplayName("采集JVM指标 - 成功")
    void testJvmMetrics() {
        SystemMetrics metrics = monitorService.getCurrentMetrics();

        assertNotNull(metrics.getJvmUptime());
        assertNotNull(metrics.getThreadCount());

        assertTrue(metrics.getJvmUptime() > 0);
        assertTrue(metrics.getThreadCount() > 0);
    }

    @Test
    @DisplayName("采集线程指标 - 成功")
    void testThreadMetrics() {
        SystemMetrics metrics = monitorService.getCurrentMetrics();

        assertNotNull(metrics.getThreadCount());
        assertNotNull(metrics.getPeakThreadCount());
        assertNotNull(metrics.getDaemonThreadCount());

        assertTrue(metrics.getThreadCount() >= metrics.getDaemonThreadCount());
    }

    // ==================== 系统概览测试 ====================

    @Test
    @DisplayName("获取系统概览 - 成功")
    void testGetSystemOverview() {
        Map<String, Object> overview = monitorService.getSystemOverview();

        assertNotNull(overview);
        assertTrue(overview.containsKey("cpuUsage"));
        assertTrue(overview.containsKey("memoryUsage"));
        assertTrue(overview.containsKey("diskUsage"));
        assertTrue(overview.containsKey("status"));
    }

    @Test
    @DisplayName("检查系统健康状态 - 成功")
    void testCheckHealth() {
        Map<String, Object> health = monitorService.checkHealth();

        assertNotNull(health);
        assertTrue(health.containsKey("status"));
        assertTrue(health.containsKey("cpu"));
        assertTrue(health.containsKey("memory"));
        assertTrue(health.containsKey("disk"));
    }

    // ==================== JVM信息测试 ====================

    @Test
    @DisplayName("获取JVM信息 - 成功")
    void testGetJvmInfo() {
        Map<String, Object> jvmInfo = monitorService.getJvmInfo();

        assertNotNull(jvmInfo);
        assertNotNull(jvmInfo.get("javaVersion"));
        assertNotNull(jvmInfo.get("javaVendor"));
    }

    @Test
    @DisplayName("获取线程信息 - 成功")
    void testGetThreadInfo() {
        Map<String, Object> threadInfo = monitorService.getThreadInfo();

        assertNotNull(threadInfo);
        assertNotNull(threadInfo.get("threadCount"));
        assertNotNull(threadInfo.get("peakThreadCount"));
        assertNotNull(threadInfo.get("deadlockedThreads"));
    }

    @Test
    @DisplayName("获取内存信息 - 成功")
    void testGetMemoryInfo() {
        Map<String, Object> memoryInfo = monitorService.getMemoryInfo();

        assertNotNull(memoryInfo);
        assertNotNull(memoryInfo.get("heap"));
        assertNotNull(memoryInfo.get("nonHeap"));

        @SuppressWarnings("unchecked")
        Map<String, Long> heap = (Map<String, Long>) memoryInfo.get("heap");
        assertTrue(heap.containsKey("used"));
        assertTrue(heap.containsKey("max"));
    }

    // ==================== 历史指标测试 ====================

    @Test
    @DisplayName("获取历史指标 - 成功")
    void testGetHistoryMetrics() {
        // 先采集一些指标
        for (int i = 0; i < 5; i++) {
            monitorService.getCurrentMetrics();
        }

        var history = monitorService.getHistoryMetrics(1);
        assertNotNull(history);
    }

    // ==================== 告警规则测试 ====================

    @Test
    @DisplayName("告警规则 - 大于条件触发")
    void testAlertRuleGreaterThan() {
        AlertRule rule = new AlertRule();
        rule.setOperator(">");
        rule.setThreshold(80.0);

        assertTrue(rule.isTriggered(90.0));
        assertFalse(rule.isTriggered(70.0));
        assertFalse(rule.isTriggered(80.0));
    }

    @Test
    @DisplayName("告警规则 - 小于条件触发")
    void testAlertRuleLessThan() {
        AlertRule rule = new AlertRule();
        rule.setOperator("<");
        rule.setThreshold(20.0);

        assertTrue(rule.isTriggered(10.0));
        assertFalse(rule.isTriggered(30.0));
        assertFalse(rule.isTriggered(20.0));
    }

    @Test
    @DisplayName("告警规则 - 等于条件触发")
    void testAlertRuleEquals() {
        AlertRule rule = new AlertRule();
        rule.setOperator("==");
        rule.setThreshold(50.0);

        assertTrue(rule.isTriggered(50.0));
        assertFalse(rule.isTriggered(51.0));
    }

    @Test
    @DisplayName("告警规则 - 大于等于条件触发")
    void testAlertRuleGreaterThanOrEqual() {
        AlertRule rule = new AlertRule();
        rule.setOperator(">=");
        rule.setThreshold(80.0);

        assertTrue(rule.isTriggered(80.0));
        assertTrue(rule.isTriggered(90.0));
        assertFalse(rule.isTriggered(70.0));
    }

    @Test
    @DisplayName("告警规则 - 小于等于条件触发")
    void testAlertRuleLessThanOrEqual() {
        AlertRule rule = new AlertRule();
        rule.setOperator("<=");
        rule.setThreshold(20.0);

        assertTrue(rule.isTriggered(20.0));
        assertTrue(rule.isTriggered(10.0));
        assertFalse(rule.isTriggered(30.0));
    }

    // ==================== GC测试 ====================

    @Test
    @DisplayName("执行GC - 成功")
    void testPerformGc() {
        // 不应该抛出异常
        assertDoesNotThrow(() -> monitorService.performGc());
    }
}
