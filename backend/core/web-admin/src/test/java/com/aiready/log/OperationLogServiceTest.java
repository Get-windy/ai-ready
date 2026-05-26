package com.aiready.log;

import com.aiready.log.dto.OperationLogQueryRequest;
import com.aiready.log.entity.OperationLog;
import com.aiready.log.service.OperationLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class OperationLogServiceTest {

    @Resource
    private OperationLogService operationLogService;

    @Test
    void testSaveAndGetLog() {
        // 创建测试日志
        OperationLog log = new OperationLog();
        log.setModule("Test Module");
        log.setOperationType("CREATE");
        log.setOperationDesc("Test operation description");
        log.setOperatorName("Test User");
        log.setOperatorIp("127.0.0.1");
        log.setStatus(1);

        // 保存日志
        operationLogService.saveLog(log);

        // 验证日志已保存
        assertNotNull(log.getId());

        // 获取日志详情
        var retrievedLog = operationLogService.getLogDetail(log.getId());
        assertNotNull(retrievedLog);
        assertEquals("Test Module", retrievedLog.getModule());
        assertEquals("CREATE", retrievedLog.getOperationType());
    }

    @Test
    void testQueryLogs() {
        // 创建查询请求
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        // 查询日志
        IPage<com.aiready.log.dto.OperationLogDTO> page = operationLogService.queryLogs(request);

        // 验证结果
        assertNotNull(page);
        assertTrue(page.getSize() >= 0);
    }

    @Test
    void testGetOperationTypeStats() {
        // 测试获取操作类型统计
        var stats = operationLogService.getOperationTypeStats(null, null);
        assertNotNull(stats);
    }

    @Test
    void testGetModuleStats() {
        // 测试获取模块统计
        var stats = operationLogService.getModuleStats(null, null);
        assertNotNull(stats);
    }

    @Test
    void testDeleteLog() {
        // 先创建一个日志
        OperationLog log = new OperationLog();
        log.setModule("Test Module");
        log.setOperationType("DELETE");
        log.setOperationDesc("Test delete operation");
        log.setOperatorName("Test User");
        log.setOperatorIp("127.0.0.1");
        log.setStatus(1);

        operationLogService.saveLog(log);
        Long logId = log.getId();
        assertNotNull(logId);

        // 删除日志
        operationLogService.deleteLog(logId);

        // 验证日志已被删除
        var retrievedLog = operationLogService.getLogDetail(logId);
        assertNull(retrievedLog);
    }
}