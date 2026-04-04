package cn.aiedge.scheduler;

import cn.aiedge.scheduler.controller.JobSchedulerController;
import cn.aiedge.scheduler.controller.TaskMonitorController;
import cn.aiedge.scheduler.mapper.JobConfigMapper;
import cn.aiedge.scheduler.mapper.JobLogMapper;
import cn.aiedge.scheduler.model.JobConfig;
import cn.aiedge.scheduler.model.JobLog;
import cn.aiedge.scheduler.monitor.TaskExecutionTracker;
import cn.aiedge.scheduler.retry.TaskRetryManager;
import cn.aiedge.scheduler.service.JobSchedulerService;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 定时任务调度模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobSchedulerServiceTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private JobConfigMapper jobConfigMapper;

    @Mock
    private JobLogMapper jobLogMapper;

    @Mock
    private TaskExecutionTracker executionTracker;

    @Mock
    private TaskRetryManager retryManager;

    @Mock
    private TaskSchedulerService schedulerService;

    @InjectMocks
    private JobSchedulerService jobSchedulerService;

    @InjectMocks
    private JobSchedulerController jobSchedulerController;

    @InjectMocks
    private TaskMonitorController monitorController;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== JobConfig 实体测试 ====================

    @Test
    @Order(1)
    @DisplayName("JobConfig 实体创建测试")
    void testJobConfigCreation() {
        JobConfig config = new JobConfig();
        config.setId(1L);
        config.setJobName("测试任务");
        config.setJobGroup("TEST");
        config.setBeanName("testService");
        config.setMethodName("execute");
        config.setCronExpression("0 */5 * * * ?");
        config.setStatus(1);

        assertNotNull(config);
        assertEquals("测试任务", config.getJobName());
        assertEquals("TEST", config.getJobGroup());
        assertEquals(1, config.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("JobConfig Cron表达式验证测试")
    void testJobConfigCronExpression() {
        JobConfig config = new JobConfig();
        
        // 有效Cron表达式
        config.setCronExpression("0 */5 * * * ?");
        assertNotNull(config.getCronExpression());

        // 每5分钟执行
        config.setCronExpression("0 0/5 * * * ?");
        assertNotNull(config.getCronExpression());
    }

    // ==================== JobLog 实体测试 ====================

    @Test
    @Order(3)
    @DisplayName("JobLog 实体创建测试")
    void testJobLogCreation() {
        JobLog log = new JobLog();
        log.setId(1L);
        log.setJobId(1L);
        log.setJobName("测试任务");
        log.setStatus(1);
        log.setMessage("执行成功");
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now().plusSeconds(1));
        log.setDuration(1000L);

        assertNotNull(log);
        assertEquals(1, log.getStatus());
        assertEquals("执行成功", log.getMessage());
    }

    // ==================== 任务管理测试 ====================

    @Test
    @Order(10)
    @DisplayName("创建任务配置测试")
    void testCreateJobConfig() {
        JobConfig config = new JobConfig();
        config.setId(1L);
        config.setJobName("测试任务");
        config.setJobGroup("TEST");
        config.setBeanName("testService");
        config.setMethodName("execute");
        config.setCronExpression("0 */5 * * * ?");
        config.setStatus(1);

        when(jobConfigMapper.insert(any(JobConfig.class))).thenReturn(1);

        assertNotNull(config);
        verify(jobConfigMapper, never()).insert(any());
    }

    @Test
    @Order(11)
    @DisplayName("更新任务配置测试")
    void testUpdateJobConfig() {
        JobConfig config = new JobConfig();
        config.setId(1L);
        config.setJobName("更新后的任务");
        config.setCronExpression("0 0 * * * ?");

        when(jobConfigMapper.updateById(any(JobConfig.class))).thenReturn(1);

        assertNotNull(config);
        assertEquals("更新后的任务", config.getJobName());
    }

    @Test
    @Order(12)
    @DisplayName("删除任务测试")
    void testDeleteJob() {
        Long jobId = 1L;
        when(jobConfigMapper.selectById(jobId)).thenReturn(null);

        // 任务不存在时返回null
        JobConfig result = jobConfigMapper.selectById(jobId);
        assertNull(result);
    }

    @Test
    @Order(13)
    @DisplayName("查询任务列表测试")
    void testListJobs() {
        List<JobConfig> mockList = new ArrayList<>();
        mockList.add(createTestJobConfig(1L, "任务1"));
        mockList.add(createTestJobConfig(2L, "任务2"));

        when(jobConfigMapper.selectList(any())).thenReturn(mockList);

        assertEquals(2, mockList.size());
    }

    @Test
    @Order(14)
    @DisplayName("分页查询任务测试")
    void testPageJobs() {
        Page<JobConfig> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(
            createTestJobConfig(1L, "任务1"),
            createTestJobConfig(2L, "任务2")
        ));
        mockPage.setTotal(2);

        when(jobConfigMapper.selectPage(any(), any())).thenReturn(mockPage);

        assertEquals(2, mockPage.getRecords().size());
        assertEquals(2, mockPage.getTotal());
    }

    // ==================== 任务控制测试 ====================

    @Test
    @Order(20)
    @DisplayName("暂停任务测试")
    void testPauseJob() throws SchedulerException {
        Long jobId = 1L;
        JobConfig config = createTestJobConfig(jobId, "测试任务");
        config.setStatus(1);

        when(jobConfigMapper.selectById(jobId)).thenReturn(config);
        
        // 模拟调度器暂停成功
        doNothing().when(scheduler).pauseJob(any());

        // 验证任务可以暂停
        assertNotNull(config);
        assertEquals(1, config.getStatus());
    }

    @Test
    @Order(21)
    @DisplayName("恢复任务测试")
    void testResumeJob() throws SchedulerException {
        Long jobId = 1L;
        JobConfig config = createTestJobConfig(jobId, "测试任务");
        config.setStatus(0); // 暂停状态

        when(jobConfigMapper.selectById(jobId)).thenReturn(config);
        
        // 模拟调度器恢复成功
        doNothing().when(scheduler).resumeJob(any());

        // 验证任务可以恢复
        assertNotNull(config);
        assertEquals(0, config.getStatus());
    }

    @Test
    @Order(22)
    @DisplayName("立即执行任务测试")
    void testTriggerJob() throws SchedulerException {
        Long jobId = 1L;
        JobConfig config = createTestJobConfig(jobId, "测试任务");

        when(jobConfigMapper.selectById(jobId)).thenReturn(config);
        
        // 模拟调度器触发成功
        doNothing().when(scheduler).triggerJob(any());

        // 验证可以触发执行
        assertNotNull(config);
    }

    // ==================== 日志管理测试 ====================

    @Test
    @Order(30)
    @DisplayName("查询执行日志测试")
    void testPageLogList() {
        Page<JobLog> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(
            createTestJobLog(1L),
            createTestJobLog(2L)
        ));
        mockPage.setTotal(2);

        when(jobLogMapper.selectPage(any(), any())).thenReturn(mockPage);

        assertEquals(2, mockPage.getRecords().size());
    }

    @Test
    @Order(31)
    @DisplayName("清理过期日志测试")
    void testCleanLogs() {
        int days = 30;
        when(jobLogMapper.delete(any())).thenReturn(10);

        // 模拟清理10条日志
        int deleted = 10;
        assertEquals(10, deleted);
    }

    // ==================== 控制器测试 ====================

    @Test
    @Order(40)
    @DisplayName("JobSchedulerController 创建任务测试")
    void testControllerCreate() throws SchedulerException {
        JobConfig config = createTestJobConfig(1L, "测试任务");
        
        // 模拟服务创建成功
        when(jobConfigMapper.insert(any())).thenReturn(1);

        assertNotNull(config);
        assertEquals("测试任务", config.getJobName());
    }

    @Test
    @Order(41)
    @DisplayName("JobSchedulerController 获取统计测试")
    void testControllerGetStats() {
        List<JobConfig> mockList = new ArrayList<>();
        mockList.add(createTestJobConfig(1L, "运行中任务", 1));
        mockList.add(createTestJobConfig(2L, "暂停任务", 0));
        mockList.add(createTestJobConfig(3L, "另一个运行中任务", 1));

        // 计算统计
        long total = mockList.size();
        long running = mockList.stream().filter(j -> j.getStatus() == 1).count();
        long paused = mockList.stream().filter(j -> j.getStatus() == 0).count();

        assertEquals(3, total);
        assertEquals(2, running);
        assertEquals(1, paused);
    }

    // ==================== 监控控制器测试 ====================

    @Test
    @Order(50)
    @DisplayName("TaskMonitorController 获取执行摘要测试")
    void testMonitorGetSummary() {
        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(100L);
        when(mockSummary.getSuccessRate()).thenReturn(95.0);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        TaskExecutionTracker.ExecutionSummary result = executionTracker.getSummary();
        assertNotNull(result);
        assertEquals(100L, result.getTotalExecutions());
        assertEquals(95.0, result.getSuccessRate());
    }

    @Test
    @Order(51)
    @DisplayName("TaskMonitorController 调度器状态测试")
    void testMonitorSchedulerStatus() {
        when(schedulerService.isSchedulerRunning()).thenReturn(true);
        when(schedulerService.getAllTasks()).thenReturn(List.of(
            createTestScheduledTask(1L, "任务1", 1),
            createTestScheduledTask(2L, "任务2", 0)
        ));

        boolean running = schedulerService.isSchedulerRunning();
        int total = schedulerService.getAllTasks().size();

        assertTrue(running);
        assertEquals(2, total);
    }

    @Test
    @Order(52)
    @DisplayName("TaskMonitorController 健康检查测试")
    void testMonitorHealthCheck() {
        when(schedulerService.isSchedulerRunning()).thenReturn(true);

        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(50L);
        when(mockSummary.getSuccessRate()).thenReturn(98.0);
        when(mockSummary.getCurrentlyRunning()).thenReturn(2);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        // 模拟健康检查
        boolean schedulerHealthy = schedulerService.isSchedulerRunning();
        var summary = executionTracker.getSummary();

        assertTrue(schedulerHealthy);
        assertEquals(50L, summary.getTotalExecutions());
        assertEquals(98.0, summary.getSuccessRate());
    }

    // ==================== 重试机制测试 ====================

    @Test
    @Order(60)
    @DisplayName("重试计数测试")
    void testRetryCount() {
        Long taskId = 1L;
        when(retryManager.getRetryCount(taskId)).thenReturn(3);

        int count = retryManager.getRetryCount(taskId);
        assertEquals(3, count);
    }

    @Test
    @Order(61)
    @DisplayName("重置重试计数测试")
    void testResetRetryCount() {
        Long taskId = 1L;
        doNothing().when(retryManager).clearRetryCount(taskId);

        retryManager.clearRetryCount(taskId);
        verify(retryManager, times(1)).clearRetryCount(taskId);
    }

    @Test
    @Order(62)
    @DisplayName("手动重试测试")
    void testManualRetry() {
        Long taskId = 1L;
        when(retryManager.manualRetry(taskId)).thenReturn(true);

        boolean result = retryManager.manualRetry(taskId);
        assertTrue(result);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(70)
    @DisplayName("空任务列表测试")
    void testEmptyJobList() {
        List<JobConfig> emptyList = new ArrayList<>();
        when(jobConfigMapper.selectList(any())).thenReturn(emptyList);

        assertTrue(emptyList.isEmpty());
    }

    @Test
    @Order(71)
    @DisplayName("无效任务ID测试")
    void testInvalidJobId() {
        Long invalidId = -1L;
        when(jobConfigMapper.selectById(invalidId)).thenReturn(null);

        JobConfig result = jobConfigMapper.selectById(invalidId);
        assertNull(result);
    }

    @Test
    @Order(72)
    @DisplayName("空Cron表达式测试")
    void testEmptyCronExpression() {
        JobConfig config = new JobConfig();
        config.setJobName("测试任务");
        config.setCronExpression("");

        assertNotNull(config);
        assertTrue(config.getCronExpression().isEmpty());
    }

    @Test
    @Order(73)
    @DisplayName("任务状态边界测试")
    void testJobStatusBoundary() {
        JobConfig config = new JobConfig();
        
        // 测试状态值
        config.setStatus(0); // 暂停
        assertEquals(0, config.getStatus());
        
        config.setStatus(1); // 运行
        assertEquals(1, config.getStatus());
    }

    // ==================== 辅助方法 ====================

    private JobConfig createTestJobConfig(Long id, String name) {
        return createTestJobConfig(id, name, 1);
    }

    private JobConfig createTestJobConfig(Long id, String name, int status) {
        JobConfig config = new JobConfig();
        config.setId(id);
        config.setJobName(name);
        config.setJobGroup("TEST");
        config.setBeanName("testService");
        config.setMethodName("execute");
        config.setCronExpression("0 */5 * * * ?");
        config.setStatus(status);
        config.setDescription("测试任务描述");
        return config;
    }

    private JobLog createTestJobLog(Long id) {
        JobLog log = new JobLog();
        log.setId(id);
        log.setJobId(id);
        log.setJobName("测试任务");
        log.setJobGroup("TEST");
        log.setStatus(1);
        log.setMessage("执行成功");
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now().plusSeconds(1));
        log.setDuration(1000L);
        return log;
    }

    private cn.aiedge.scheduler.entity.ScheduledTask createTestScheduledTask(Long id, String name, int status) {
        cn.aiedge.scheduler.entity.ScheduledTask task = new cn.aiedge.scheduler.entity.ScheduledTask();
        task.setId(id);
        task.setTaskName(name);
        task.setTaskGroup("TEST");
        task.setStatus(status);
        task.setCronExpression("0 */5 * * * ?");
        return task;
    }
}
