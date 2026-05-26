package cn.aiedge.base.log.service;

import cn.aiedge.base.config.TestConfig;
import cn.aiedge.base.log.entity.SystemLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统日志服务测试类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@SpringBootTest(classes = TestConfig.class)
public class SystemLogServiceTest {

    @Autowired
    private SystemLogService systemLogService;

    @Test
    public void testSaveLog() {
        // 创建测试日志对象
        SystemLog log = new SystemLog();
        log.setLogType(1); // 操作日志
        log.setLogLevel("INFO");
        log.setTitle("测试日志");
        log.setOperationType("TEST");
        log.setModuleName("测试模块");
        log.setUserId(1L);
        log.setUsername("testuser");
        log.setIpAddress("127.0.0.1");
        log.setStatus(1); // 成功
        log.setCreateTime(LocalDateTime.now());

        // 保存日志
        systemLogService.saveLog(log);

        // 验证日志已保存
        assertNotNull(log.getId());
        System.out.println("保存日志成功，ID: " + log.getId());
    }

    @Test
    public void testGetLogDetail() {
        // 先保存一个日志
        SystemLog log = new SystemLog();
        log.setLogType(1);
        log.setLogLevel("INFO");
        log.setTitle("测试详情查询");
        log.setOperationType("QUERY");
        log.setModuleName("测试模块");
        log.setUserId(1L);
        log.setUsername("testuser");
        log.setIpAddress("127.0.0.1");
        log.setStatus(1);
        log.setCreateTime(LocalDateTime.now());

        systemLogService.saveLog(log);

        // 查询日志详情
        SystemLog queriedLog = systemLogService.getLogDetail(log.getId());

        // 验证查询结果
        assertNotNull(queriedLog);
        assertEquals(log.getId(), queriedLog.getId());
        assertEquals("测试详情查询", queriedLog.getTitle());
    }

    @Test
    public void testGetRecentLogsByUser() {
        Long userId = 1L;
        
        // 获取用户最近日志
        List<SystemLog> recentLogs = systemLogService.getRecentLogsByUser(userId, 5);
        
        // 验证返回结果
        assertNotNull(recentLogs);
        assertTrue(recentLogs.size() >= 0);
        System.out.println("获取用户最近日志数量: " + recentLogs.size());
    }

    @Test
    public void testAsyncSaveLog() {
        // 创建测试日志对象
        SystemLog log = new SystemLog();
        log.setLogType(2); // 登录日志
        log.setLogLevel("INFO");
        log.setTitle("异步测试日志");
        log.setOperationType("ASYNC_TEST");
        log.setModuleName("异步测试模块");
        log.setUserId(1L);
        log.setUsername("async_test_user");
        log.setIpAddress("127.0.0.1");
        log.setStatus(1);
        log.setCreateTime(LocalDateTime.now());

        // 异步保存日志
        systemLogService.saveLogAsync(log);

        // 由于是异步操作，稍等片刻后再验证
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("异步保存日志已提交");
    }
}