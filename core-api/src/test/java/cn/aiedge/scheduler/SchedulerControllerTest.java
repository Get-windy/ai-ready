package cn.aiedge.scheduler;

import cn.aiedge.scheduler.controller.JobSchedulerController;
import cn.aiedge.scheduler.controller.TaskMonitorController;
import cn.aiedge.scheduler.controller.TaskSchedulerController;
import cn.aiedge.scheduler.model.JobConfig;
import cn.aiedge.scheduler.model.JobLog;
import cn.aiedge.scheduler.monitor.TaskExecutionTracker;
import cn.aiedge.scheduler.retry.RetryPolicy;
import cn.aiedge.scheduler.retry.TaskRetryManager;
import cn.aiedge.scheduler.service.JobSchedulerService;
import cn.aiedge.scheduler.service.TaskSchedulerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.SchedulerException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 调度模块Controller层集成测试
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchedulerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobSchedulerService jobSchedulerService;

    @Mock
    private TaskSchedulerService schedulerService;

    @Mock
    private TaskExecutionTracker executionTracker;

    @Mock
    private TaskRetryManager retryManager;

    @InjectMocks
    private JobSchedulerController jobSchedulerController;

    @InjectMocks
    private TaskMonitorController monitorController;

    @InjectMocks
    private TaskSchedulerController taskSchedulerController;

    private ObjectMapper objectMapper;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(
            jobSchedulerController, 
            monitorController,
            taskSchedulerController
        ).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== JobSchedulerController 测试 ====================

    @Test
    @Order(1)
    @DisplayName("创建定时任务 - 成功")
    void testCreateJobSuccess() throws Exception {
        JobConfig config = createTestJobConfig(1L, "测试任务");
        String requestJson = objectMapper.writeValueAsString(config);

        when(jobSchedulerService.getById(anyLong())).thenReturn(config);

        mockMvc.perform(post("/api/scheduler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("更新定时任务 - 成功")
    void testUpdateJobSuccess() throws Exception {
        JobConfig config = createTestJobConfig(1L, "更新后的任务");
        String requestJson = objectMapper.writeValueAsString(config);

        when(jobSchedulerService.getById(anyLong())).thenReturn(config);

        mockMvc.perform(put("/api/scheduler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("删除定时任务 - 成功")
    void testDeleteJobSuccess() throws Exception {
        Long jobId = 1L;
        
        when(jobSchedulerService.deleteJob(jobId)).thenReturn(true);

        mockMvc.perform(delete("/api/scheduler/{jobId}", jobId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("获取任务详情 - 成功")
    void testGetJobInfoSuccess() throws Exception {
        Long jobId = 1L;
        JobConfig config = createTestJobConfig(jobId, "测试任务");

        when(jobSchedulerService.getById(jobId)).thenReturn(config);

        mockMvc.perform(get("/api/scheduler/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("测试任务"));
    }

    @Test
    @Order(5)
    @DisplayName("获取所有任务 - 成功")
    void testListAllJobsSuccess() throws Exception {
        List<JobConfig> jobs = Arrays.asList(
            createTestJobConfig(1L, "任务1"),
            createTestJobConfig(2L, "任务2")
        );

        when(jobSchedulerService.listAll()).thenReturn(jobs);

        mockMvc.perform(get("/api/scheduler/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(6)
    @DisplayName("分页查询任务 - 成功")
    void testPageJobsSuccess() throws Exception {
        Page<JobConfig> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(
            createTestJobConfig(1L, "任务1"),
            createTestJobConfig(2L, "任务2")
        ));
        mockPage.setTotal(2);

        when(jobSchedulerService.pageList(anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(mockPage);

        mockMvc.perform(get("/api/scheduler/page")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(2));
    }

    @Test
    @Order(7)
    @DisplayName("暂停任务 - 成功")
    void testPauseJobSuccess() throws Exception {
        Long jobId = 1L;
        
        doNothing().when(jobSchedulerService).pauseJob(jobId);

        mockMvc.perform(put("/api/scheduler/pause/{jobId}", jobId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("恢复任务 - 成功")
    void testResumeJobSuccess() throws Exception {
        Long jobId = 1L;
        
        doNothing().when(jobSchedulerService).resumeJob(jobId);

        mockMvc.perform(put("/api/scheduler/resume/{jobId}", jobId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("立即执行任务 - 成功")
    void testRunOnceSuccess() throws Exception {
        Long jobId = 1L;
        
        doNothing().when(jobSchedulerService).runOnce(jobId);

        mockMvc.perform(post("/api/scheduler/run/{jobId}", jobId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("分页查询日志 - 成功")
    void testPageLogListSuccess() throws Exception {
        Page<JobLog> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(
            createTestJobLog(1L),
            createTestJobLog(2L)
        ));
        mockPage.setTotal(2);

        when(jobSchedulerService.pageLogList(anyInt(), anyInt(), any(), any()))
            .thenReturn(mockPage);

        mockMvc.perform(get("/api/scheduler/log/page")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(2));
    }

    @Test
    @Order(11)
    @DisplayName("清理日志 - 成功")
    void testCleanLogsSuccess() throws Exception {
        when(jobSchedulerService.cleanLogs(anyInt())).thenReturn(10);

        mockMvc.perform(delete("/api/scheduler/log/clean")
                .param("days", "30"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(12)
    @DisplayName("获取统计信息 - 成功")
    void testGetStatsSuccess() throws Exception {
        List<JobConfig> jobs = Arrays.asList(
            createTestJobConfig(1L, "运行中任务", 1),
            createTestJobConfig(2L, "暂停任务", 0),
            createTestJobConfig(3L, "另一个运行中任务", 1)
        );

        when(jobSchedulerService.listAll()).thenReturn(jobs);

        mockMvc.perform(get("/api/scheduler/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.running").value(2))
                .andExpect(jsonPath("$.paused").value(1));
    }

    // ==================== TaskMonitorController 测试 ====================

    @Test
    @Order(20)
    @DisplayName("获取执行摘要 - 成功")
    void testGetExecutionSummarySuccess() throws Exception {
        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(100L);
        when(mockSummary.getSuccessRate()).thenReturn(95.0);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        mockMvc.perform(get("/api/scheduler/monitor/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(21)
    @DisplayName("获取全局统计 - 成功")
    void testGetGlobalStatisticsSuccess() throws Exception {
        TaskExecutionTracker.GlobalStatistics mockStats = 
            mock(TaskExecutionTracker.GlobalStatistics.class);
        when(mockStats.getTotalTasks()).thenReturn(10L);
        when(mockStats.getCurrentlyRunning()).thenReturn(3);

        when(executionTracker.getGlobalStatistics()).thenReturn(mockStats);

        mockMvc.perform(get("/api/scheduler/monitor/global-stats"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(22)
    @DisplayName("获取正在执行的任务 - 成功")
    void testGetRunningTasksSuccess() throws Exception {
        Map<Long, TaskExecutionTracker.ExecutionContext> mockContext = new HashMap<>();
        
        when(executionTracker.getRunningTasks()).thenReturn(mockContext);

        mockMvc.perform(get("/api/scheduler/monitor/running"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("获取调度器状态 - 成功")
    void testGetSchedulerStatusSuccess() throws Exception {
        when(schedulerService.isSchedulerRunning()).thenReturn(true);
        when(schedulerService.getAllTasks()).thenReturn(Arrays.asList(
            createTestScheduledTask(1L, "任务1", 1),
            createTestScheduledTask(2L, "任务2", 0)
        ));

        mockMvc.perform(get("/api/scheduler/monitor/scheduler-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true));
    }

    @Test
    @Order(24)
    @DisplayName("健康检查 - 正常")
    void testHealthCheckNormal() throws Exception {
        when(schedulerService.isSchedulerRunning()).thenReturn(true);

        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(100L);
        when(mockSummary.getSuccessRate()).thenReturn(98.0);
        when(mockSummary.getCurrentlyRunning()).thenReturn(2);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        mockMvc.perform(get("/api/scheduler/monitor/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(25)
    @DisplayName("健康检查 - 调度器停止")
    void testHealthCheckSchedulerDown() throws Exception {
        when(schedulerService.isSchedulerRunning()).thenReturn(false);

        TaskExecutionTracker.ExecutionSummary mockSummary = 
            mock(TaskExecutionTracker.ExecutionSummary.class);
        when(mockSummary.getTotalExecutions()).thenReturn(10L);
        when(mockSummary.getSuccessRate()).thenReturn(50.0);
        when(mockSummary.getCurrentlyRunning()).thenReturn(0);

        when(executionTracker.getSummary()).thenReturn(mockSummary);

        mockMvc.perform(get("/api/scheduler/monitor/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    // ==================== 重试管理测试 ====================

    @Test
    @Order(30)
    @DisplayName("获取重试策略 - 成功")
    void testGetRetryPolicySuccess() throws Exception {
        Long taskId = 1L;
        
        cn.aiedge.scheduler.entity.ScheduledTask task = createTestScheduledTask(taskId, "测试任务", 1);
        when(schedulerService.getTask(taskId)).thenReturn(task);
        
        RetryPolicy policy = new RetryPolicy();
        when(retryManager.getRetryPolicy(task)).thenReturn(policy);

        mockMvc.perform(get("/api/scheduler/monitor/retry/{taskId}/policy", taskId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(31)
    @DisplayName("设置重试策略 - 成功")
    void testSetRetryPolicySuccess() throws Exception {
        Long taskId = 1L;
        RetryPolicy policy = new RetryPolicy();
        String requestJson = objectMapper.writeValueAsString(policy);

        doNothing().when(retryManager).setRetryPolicy(eq(taskId), any(RetryPolicy.class));

        mockMvc.perform(put("/api/scheduler/monitor/retry/{taskId}/policy", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(32)
    @DisplayName("获取重试计数 - 成功")
    void testGetRetryCountSuccess() throws Exception {
        Long taskId = 1L;
        
        when(retryManager.getRetryCount(taskId)).thenReturn(3);

        mockMvc.perform(get("/api/scheduler/monitor/retry/{taskId}/count", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retryCount").value(3));
    }

    @Test
    @Order(33)
    @DisplayName("重置重试计数 - 成功")
    void testResetRetryCountSuccess() throws Exception {
        Long taskId = 1L;
        
        doNothing().when(retryManager).clearRetryCount(taskId);

        mockMvc.perform(post("/api/scheduler/monitor/retry/{taskId}/reset", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(34)
    @DisplayName("手动触发重试 - 成功")
    void testManualRetrySuccess() throws Exception {
        Long taskId = 1L;
        
        when(retryManager.manualRetry(taskId)).thenReturn(true);

        mockMvc.perform(post("/api/scheduler/monitor/retry/{taskId}/manual", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== 统计重置测试 ====================

    @Test
    @Order(40)
    @DisplayName("重置任务统计 - 成功")
    void testResetTaskStatsSuccess() throws Exception {
        Long taskId = 1L;
        
        doNothing().when(executionTracker).resetStatistics(taskId);

        mockMvc.perform(post("/api/scheduler/monitor/reset-stats/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(41)
    @DisplayName("重置所有统计 - 成功")
    void testResetAllStatsSuccess() throws Exception {
        doNothing().when(executionTracker).resetAllStatistics();

        mockMvc.perform(post("/api/scheduler/monitor/reset-all-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== 异常场景测试 ====================

    @Test
    @Order(50)
    @DisplayName("获取不存在的任务 - 返回null")
    void testGetNonExistentJob() throws Exception {
        Long jobId = 999L;
        
        when(jobSchedulerService.getById(jobId)).thenReturn(null);

        mockMvc.perform(get("/api/scheduler/{jobId}", jobId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(51)
    @DisplayName("获取不存在的任务统计 - 返回404")
    void testGetNonExistentTaskStats() throws Exception {
        Long taskId = 999L;
        
        when(executionTracker.getStatistics(taskId)).thenReturn(null);

        mockMvc.perform(get("/api/scheduler/monitor/task-stats/{taskId}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(52)
    @DisplayName("获取不存在的任务重试策略 - 返回404")
    void testGetNonExistentTaskRetryPolicy() throws Exception {
        Long taskId = 999L;
        
        when(schedulerService.getTask(taskId)).thenReturn(null);

        mockMvc.perform(get("/api/scheduler/monitor/retry/{taskId}/policy", taskId))
                .andExpect(status().isNotFound());
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
