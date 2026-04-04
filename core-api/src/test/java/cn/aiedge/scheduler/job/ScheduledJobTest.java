package cn.aiedge.scheduler.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScheduledJob 接口测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class ScheduledJobTest {

    private SampleJob sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = new SampleJob();
    }

    @Test
    @DisplayName("执行示例任务 - 无参数")
    void testExecute_NoParams() throws Exception {
        // When
        String result = sampleJob.execute(Map.of());

        // Then
        assertNotNull(result);
        assertTrue(result.contains("示例任务执行成功"));
        assertTrue(result.contains("当前时间"));
    }

    @Test
    @DisplayName("执行示例任务 - 带参数")
    void testExecute_WithParams() throws Exception {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", 123);

        // When
        String result = sampleJob.execute(params);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("示例任务执行成功"));
        assertTrue(result.contains("key1"));
    }

    @Test
    @DisplayName("获取任务名称")
    void testGetTaskName() {
        assertEquals("示例任务", sampleJob.getTaskName());
    }

    @Test
    @DisplayName("获取任务描述")
    void testGetDescription() {
        assertNotNull(sampleJob.getDescription());
        assertTrue(sampleJob.getDescription().contains("示例"));
    }

    @Test
    @DisplayName("默认钩子方法 - 不抛出异常")
    void testDefaultHookMethods() {
        // Given
        Map<String, Object> params = Map.of();

        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> sampleJob.beforeExecute(params));
        assertDoesNotThrow(() -> sampleJob.afterExecute(params, "result", null));
        assertDoesNotThrow(() -> sampleJob.afterExecute(params, null, new Exception("test")));
    }
}
