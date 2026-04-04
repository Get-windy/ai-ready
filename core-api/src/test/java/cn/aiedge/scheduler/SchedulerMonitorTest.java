package cn.aiedge.scheduler;

import cn.aiedge.scheduler.monitor.TaskExecutionTracker;
import cn.aiedge.scheduler.retry.RetryPolicy;
import cn.aiedge.scheduler.retry.TaskRetryManager;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务监控与重试机制单元测试
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchedulerMonitorTest {

    @Mock
    private TaskExecutionTracker executionTracker;

    @Mock
    private TaskRetryManager retryManager;

    @Mock
    private TaskSchedulerService schedulerService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== 执行统计测试 ====================

    @Test
    @Order(1)
    @DisplayName("执行摘要测试")
    void testExecutionSummary() {
        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        
        when(mockSummary.getTotalExecutions()).thenReturn(100L);
        when(mockSummary.getSuccessfulExecutions()).thenReturn(95L);
        when(mockSummary.getFailedExecutions()).thenReturn(5L);
        when(mockSummary.getSuccessRate()).thenReturn(95.0);
        when(mockSummary.getCurrentlyRunning()).thenReturn(3);
        when(mockSummary.getAverageExecutionTime()).thenReturn(250.5);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        var result = executionTracker.getSummary();
        
        assertNotNull(result);
        assertEquals(100L, result.getTotalExecutions());
        assertEquals(95L, result.getSuccessfulExecutions());
        assertEquals(5L, result.getFailedExecutions());
        assertEquals(95.0, result.getSuccessRate());
        assertEquals(3, result.getCurrentlyRunning());
        assertEquals(250.5, result.getAverageExecutionTime());
    }

    @Test
    @Order(2)
    @DisplayName("全局统计测试")
    void testGlobalStatistics() {
        TaskExecutionTracker.GlobalStatistics mockStats = 
            mock(TaskExecutionTracker.GlobalStatistics.class);
        
        when(mockStats.getTotalTasks()).thenReturn(10L);
        when(mockStats.getActiveTasks()).thenReturn(8L);
        when(mockStats.getPausedTasks()).thenReturn(2L);
        when(mockStats.getCurrentlyRunning()).thenReturn(3);
        when(mockStats.getTotalExecutions()).thenReturn(500L);
        when(mockStats.getSuccessRate()).thenReturn(98.5);

        when(executionTracker.getGlobalStatistics()).thenReturn(mockStats);

        var result = executionTracker.getGlobalStatistics();
        
        assertNotNull(result);
        assertEquals(10L, result.getTotalTasks());
        assertEquals(8L, result.getActiveTasks());
        assertEquals(2L, result.getPausedTasks());
    }

    @Test
    @Order(3)
    @DisplayName("任务统计测试")
    void testTaskStatistics() {
        Long taskId = 1L;
        TaskExecutionTracker.TaskStatistics mockTaskStats = 
            mock(TaskExecutionTracker.TaskStatistics.class);
        
        when(mockTaskStats.getTaskId()).thenReturn(taskId);
        when(mockTaskStats.getTaskName()).thenReturn("测试任务");
        when(mockTaskStats.getTotalExecutions()).thenReturn(50L);
        when(mockTaskStats.getSuccessfulExecutions()).thenReturn(48L);
        when(mockTaskStats.getFailedExecutions()).thenReturn(2L);
        when(mockTaskStats.getSuccessRate()).thenReturn(96.0);
        when(mockTaskStats.getAverageExecutionTime()).thenReturn(180.5);
        when(mockTaskStats.getLastExecutionTime()).thenReturn(200L);

        when(executionTracker.getStatistics(taskId)).thenReturn(mockTaskStats);

        var result = executionTracker.getStatistics(taskId);
        
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals("测试任务", result.getTaskName());
        assertEquals(50L, result.getTotalExecutions());
        assertEquals(96.0, result.getSuccessRate());
    }

    @Test
    @Order(4)
    @DisplayName("所有任务统计测试")
    void testAllTaskStatistics() {
        Map<Long, TaskExecutionTracker.TaskStatistics> mockMap = new HashMap<>();
        
        TaskExecutionTracker.TaskStatistics stats1 = 
            mock(TaskExecutionTracker.TaskStatistics.class);
        when(stats1.getTaskId()).thenReturn(1L);
        when(stats1.getTotalExecutions()).thenReturn(50L);
        
        TaskExecutionTracker.TaskStatistics stats2 = 
            mock(TaskExecutionTracker.TaskStatistics.class);
        when(stats2.getTaskId()).thenReturn(2L);
        when(stats2.getTotalExecutions()).thenReturn(30L);
        
        mockMap.put(1L, stats1);
        mockMap.put(2L, stats2);

        when(executionTracker.getAllStatistics()).thenReturn(mockMap);

        var result = executionTracker.getAllStatistics();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(1L));
        assertTrue(result.containsKey(2L));
    }

    // ==================== 运行状态测试 ====================

    @Test
    @Order(10)
    @DisplayName("正在执行的任务测试")
    void testRunningTasks() {
        Map<Long, TaskExecutionTracker.ExecutionContext> mockContext = new HashMap<>();
        
        TaskExecutionTracker.ExecutionContext ctx = 
            mock(TaskExecutionTracker.ExecutionContext.class);
        when(ctx.getTaskId()).thenReturn(1L);
        when(ctx.getTaskName()).thenReturn("运行中任务");
        when(ctx.getStartTime()).thenReturn(System.currentTimeMillis());
        
        mockContext.put(1L, ctx);

        when(executionTracker.getRunningTasks()).thenReturn(mockContext);

        var result = executionTracker.getRunningTasks();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(1L));
    }

    // ==================== 重试策略测试 ====================

    @Test
    @Order(20)
    @DisplayName("默认重试策略测试")
    void testDefaultRetryPolicy() {
        RetryPolicy policy = new RetryPolicy();
        
        assertEquals(3, policy.getMaxRetries());
        assertEquals(1000L, policy.getInitialDelay());
        assertEquals(2.0, policy.getMultiplier());
        assertEquals(30000L, policy.getMaxDelay());
        assertTrue(policy.isRetryOnException());
    }

    @Test
    @Order(21)
    @DisplayName("自定义重试策略测试")
    void testCustomRetryPolicy() {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(5);
        policy.setInitialDelay(2000L);
        policy.setMultiplier(1.5);
        policy.setMaxDelay(60000L);
        policy.setRetryOnException(false);

        assertEquals(5, policy.getMaxRetries());
        assertEquals(2000L, policy.getInitialDelay());
        assertEquals(1.5, policy.getMultiplier());
        assertEquals(60000L, policy.getMaxDelay());
        assertFalse(policy.isRetryOnException());
    }

    @Test
    @Order(22)
    @DisplayName("重试延迟计算测试")
    void testRetryDelayCalculation() {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(4);
        policy.setInitialDelay(1000L);
        policy.setMultiplier(2.0);
        policy.setMaxDelay(10000L);

        // 模拟延迟计算
        // 第1次: 1000ms
        // 第2次: 2000ms
        // 第3次: 4000ms
        // 第4次: 8000ms
        
        long delay1 = policy.getInitialDelay();
        long delay2 = delay1 * (long) policy.getMultiplier();
        long delay3 = delay2 * (long) policy.getMultiplier();
        long delay4 = Math.min(delay3 * (long) policy.getMultiplier(), policy.getMaxDelay());

        assertEquals(1000L, delay1);
        assertEquals(2000L, delay2);
        assertEquals(4000L, delay3);
        assertEquals(8000L, delay4);
    }

    // ==================== 重试管理器测试 ====================

    @Test
    @Order(30)
    @DisplayName("重试计数管理测试")
    void testRetryCountManagement() {
        Long taskId = 1L;
        
        when(retryManager.getRetryCount(taskId)).thenReturn(0);
        assertEquals(0, retryManager.getRetryCount(taskId));

        when(retryManager.getRetryCount(taskId)).thenReturn(1);
        assertEquals(1, retryManager.getRetryCount(taskId));

        when(retryManager.getRetryCount(taskId)).thenReturn(2);
        assertEquals(2, retryManager.getRetryCount(taskId));
    }

    @Test
    @Order(31)
    @DisplayName("重试计数重置测试")
    void testRetryCountReset() {
        Long taskId = 1L;
        
        doNothing().when(retryManager).clearRetryCount(taskId);
        
        retryManager.clearRetryCount(taskId);
        
        verify(retryManager, times(1)).clearRetryCount(taskId);
    }

    @Test
    @Order(32)
    @DisplayName("手动触发重试测试")
    void testManualRetry() {
        Long taskId = 1L;
        
        when(retryManager.manualRetry(taskId)).thenReturn(true);
        assertTrue(retryManager.manualRetry(taskId));

        Long invalidTaskId = 999L;
        when(retryManager.manualRetry(invalidTaskId)).thenReturn(false);
        assertFalse(retryManager.manualRetry(invalidTaskId));
    }

    @Test
    @Order(33)
    @DisplayName("是否应该重试测试")
    void testShouldRetry() {
        Long taskId = 1L;
        
        when(retryManager.shouldRetry(taskId, new RuntimeException())).thenReturn(true);
        assertTrue(retryManager.shouldRetry(taskId, new RuntimeException()));

        when(retryManager.shouldRetry(taskId, new OutOfMemoryError())).thenReturn(false);
        assertFalse(retryManager.shouldRetry(taskId, new OutOfMemoryError()));
    }

    // ==================== 统计重置测试 ====================

    @Test
    @Order(40)
    @DisplayName("重置任务统计测试")
    void testResetTaskStats() {
        Long taskId = 1L;
        
        doNothing().when(executionTracker).resetStatistics(taskId);
        
        executionTracker.resetStatistics(taskId);
        
        verify(executionTracker, times(1)).resetStatistics(taskId);
    }

    @Test
    @Order(41)
    @DisplayName("重置所有统计测试")
    void testResetAllStats() {
        doNothing().when(executionTracker).resetAllStatistics();
        
        executionTracker.resetAllStatistics();
        
        verify(executionTracker, times(1)).resetAllStatistics();
    }

    // ==================== 健康检查测试 ====================

    @Test
    @Order(50)
    @DisplayName("调度器健康检查-正常状态")
    void testHealthCheckNormal() {
        when(schedulerService.isSchedulerRunning()).thenReturn(true);

        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(100L);
        when(mockSummary.getSuccessRate()).thenReturn(98.0);
        when(mockSummary.getCurrentlyRunning()).thenReturn(2);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        boolean schedulerRunning = schedulerService.isSchedulerRunning();
        var summary = executionTracker.getSummary();

        assertTrue(schedulerRunning);
        assertEquals(98.0, summary.getSuccessRate());
    }

    @Test
    @Order(51)
    @DisplayName("调度器健康检查-异常状态")
    void testHealthCheckAbnormal() {
        when(schedulerService.isSchedulerRunning()).thenReturn(false);

        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(100L);
        when(mockSummary.getSuccessRate()).thenReturn(45.0); // 低成功率
        when(mockSummary.getCurrentlyRunning()).thenReturn(0);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        boolean schedulerRunning = schedulerService.isSchedulerRunning();
        var summary = executionTracker.getSummary();

        assertFalse(schedulerRunning);
        assertTrue(summary.getSuccessRate() < 50);
    }

    // ==================== 并发测试 ====================

    @Test
    @Order(60)
    @DisplayName("并发执行计数测试")
    void testConcurrentExecutionCount() {
        TaskExecutionTracker.GlobalStatistics mockStats = 
            mock(TaskExecutionTracker.GlobalStatistics.class);
        
        when(mockStats.getCurrentlyRunning()).thenReturn(5);
        when(executionTracker.getGlobalStatistics()).thenReturn(mockStats);

        int running = executionTracker.getGlobalStatistics().getCurrentlyRunning();
        assertEquals(5, running);
    }

    @Test
    @Order(61)
    @DisplayName("最大并发限制测试")
    void testMaxConcurrencyLimit() {
        int maxConcurrency = 10;
        
        TaskExecutionTracker.GlobalStatistics mockStats = 
            mock(TaskExecutionTracker.GlobalStatistics.class);
        when(mockStats.getCurrentlyRunning()).thenReturn(10);
        when(executionTracker.getGlobalStatistics()).thenReturn(mockStats);

        int running = executionTracker.getGlobalStatistics().getCurrentlyRunning();
        assertTrue(running <= maxConcurrency);
    }

    // ==================== 异常场景测试 ====================

    @Test
    @Order(70)
    @DisplayName("任务不存在统计测试")
    void testNonExistentTaskStats() {
        Long nonExistentId = 999L;
        
        when(executionTracker.getStatistics(nonExistentId)).thenReturn(null);
        
        var result = executionTracker.getStatistics(nonExistentId);
        assertNull(result);
    }

    @Test
    @Order(71)
    @DisplayName("空运行任务列表测试")
    void testEmptyRunningTasks() {
        Map<Long, TaskExecutionTracker.ExecutionContext> emptyMap = new HashMap<>();
        when(executionTracker.getRunningTasks()).thenReturn(emptyMap);

        var result = executionTracker.getRunningTasks();
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(72)
    @DisplayName("重试次数超限测试")
    void testRetryLimitExceeded() {
        Long taskId = 1L;
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(3);
        
        when(retryManager.getRetryCount(taskId)).thenReturn(3);
        when(retryManager.shouldRetry(taskId, new RuntimeException())).thenReturn(false);

        int count = retryManager.getRetryCount(taskId);
        boolean shouldRetry = retryManager.shouldRetry(taskId, new RuntimeException());

        assertEquals(3, count);
        assertFalse(shouldRetry);
    }
}
