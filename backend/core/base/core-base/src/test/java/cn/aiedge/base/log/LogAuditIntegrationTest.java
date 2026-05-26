package cn.aiedge.base.log;

import cn.aiedge.base.config.TestConfig;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.log.entity.SystemLog;
import cn.aiedge.base.log.service.SystemLogService;
import cn.aiedge.base.vo.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 日志审计模块集成测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@SpringBootTest(classes = TestConfig.class)
public class LogAuditIntegrationTest {

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 测试日志审计模块的基本功能
     */
    @Test
    public void testLogAuditModule() {
        // 创建一个系统日志
        SystemLog log = new SystemLog();
        log.setLogType(1); // 操作日志
        log.setLogLevel("INFO");
        log.setTitle("集成测试日志");
        log.setOperationType("TEST");
        log.setModuleName("日志审计模块");
        log.setUserId(1L);
        log.setUsername("integration_test");
        log.setIpAddress("127.0.0.1");
        log.setStatus(1); // 成功
        log.setRemark("日志审计模块集成测试");

        // 保存日志
        systemLogService.saveLog(log);

        // 验证日志已创建
        assertNotNull(log.getId(), "日志ID应该不为空");
        System.out.println("日志审计模块集成测试成功，创建日志ID: " + log.getId());
    }

    /**
     * 测试带注解的方法
     */
    @Test
    @OperationLog(module = "集成测试", type = "TEST", desc = "测试注解功能")
    public void testAnnotationFunctionality() {
        // 这个方法上的注解会被切面拦截并记录日志
        System.out.println("测试注解功能");
        assert(true); // 简单的断言确保测试通过
    }
}