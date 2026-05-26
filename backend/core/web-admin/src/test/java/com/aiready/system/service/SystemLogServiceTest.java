package com.aiready.system.service;

import com.aiready.system.entity.SystemLog;
import com.aiready.system.mapper.SystemLogMapper;
import com.aiready.system.service.impl.SystemLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 系统日志服务测试类
 */
@DisplayName("系统日志服务测试")
class SystemLogServiceTest {

    @Mock
    private SystemLogMapper systemLogMapper;

    @InjectMocks
    private SystemLogServiceImpl systemLogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("测试根据类型查询日志")
    void testListByType() {
        SystemLog log1 = new SystemLog();
        log1.setId(1L);
        log1.setLogType(1);
        log1.setTitle("用户登录");

        SystemLog log2 = new SystemLog();
        log2.setId(2L);
        log2.setLogType(1);
        log2.setTitle("新增部门");

        when(systemLogMapper.selectList(any())).thenReturn(Arrays.asList(log1, log2));

        List<SystemLog> result = systemLogService.listByType(1);

        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(log -> assertEquals(1, log.getLogType()));
    }

    @Test
    @DisplayName("测试根据用户ID查询日志")
    void testListByUserId() {
        SystemLog log = new SystemLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setUsername("admin");

        when(systemLogMapper.selectList(any())).thenReturn(Arrays.asList(log));

        List<SystemLog> result = systemLogService.listByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    @DisplayName("测试记录操作日志")
    void testRecordOperationLog() {
        SystemLog log = new SystemLog();
        log.setTitle("新增用户");
        log.setOperationType("INSERT");
        log.setModuleName("用户管理");

        when(systemLogMapper.insert(any(SystemLog.class))).thenReturn(1);

        boolean result = systemLogService.recordOperationLog(log);

        assertTrue(result);
        assertEquals(1, log.getLogType());
        assertNotNull(log.getCreateTime());
    }

    @Test
    @DisplayName("测试记录登录日志")
    void testRecordLoginLog() {
        when(systemLogMapper.insert(any(SystemLog.class))).thenReturn(1);

        boolean result = systemLogService.recordLoginLog(1L, "admin", "192.168.1.1", 1, null);

        assertTrue(result);
        verify(systemLogMapper, times(1)).insert(any(SystemLog.class));
    }

    @Test
    @DisplayName("测试记录异常日志")
    void testRecordExceptionLog() {
        when(systemLogMapper.insert(any(SystemLog.class))).thenReturn(1);

        boolean result = systemLogService.recordExceptionLog(
            "系统异常", 
            "com.aiready.system.service.impl.TestService.test()", 
            "NullPointerException", 
            "{}"
        );

        assertTrue(result);
        verify(systemLogMapper, times(1)).insert(any(SystemLog.class));
    }

    @Test
    @DisplayName("测试获取用户最近日志")
    void testGetRecentLogsByUser() {
        SystemLog log = new SystemLog();
        log.setId(1L);
        log.setUserId(1L);

        when(systemLogMapper.selectList(any())).thenReturn(Arrays.asList(log));

        List<SystemLog> result = systemLogService.getRecentLogsByUser(1L, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试统计时间范围内的日志数量")
    void testCountByTimeRange() {
        when(systemLogMapper.selectCount(any())).thenReturn(5L);

        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        Long count = systemLogService.countByTimeRange(start, end);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("测试批量删除日志")
    void testBatchDelete() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        when(systemLogMapper.deleteByIds(any())).thenReturn(3);

        boolean result = systemLogService.batchDelete(ids);

        assertTrue(result);
    }
}
