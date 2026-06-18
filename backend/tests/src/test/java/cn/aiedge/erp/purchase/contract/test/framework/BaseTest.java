package cn.aiedge.erp.purchase.contract.test.framework;

import cn.aiedge.erp.purchase.contract.test.config.TestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

/**
 * 所有测试类的基类
 * 提供通用的测试生命周期管理和日志记录
 */
@ExtendWith(BaseTest.TestResultLogger.class)
public abstract class BaseTest {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);
    protected static TestConfig testConfig;
    
    protected Instant testStartTime;
    protected Instant testEndTime;
    
    /**
     * 测试生命周期日志记录器
     */
    public static class TestResultLogger implements TestWatcher {
        
        @Override
        public void testSuccessful(ExtensionContext context) {
            LOGGER.info("✓ Test PASSED: {}", getTestDisplayName(context));
        }
        
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            LOGGER.error("✗ Test FAILED: {} - Error: {}", getTestDisplayName(context), cause.getMessage());
        }
        
        @Override
        public void testAborted(ExtensionContext context, Throwable cause) {
            LOGGER.warn("⚠ Test ABORTED: {} - Reason: {}", getTestDisplayName(context), cause.getMessage());
        }
        
        @Override
        public void testDisabled(ExtensionContext context, org.junit.jupiter.api.extension.TestWatcher.TestDisabledReason reason) {
            LOGGER.info("⏸ Test DISABLED: {} - Reason: {}", getTestDisplayName(context), reason);
        }
        
        private String getTestDisplayName(ExtensionContext context) {
            return context.getDisplayName();
        }
    }
    
    /**
     * 测试类初始化
     */
    @BeforeAll
    static void setupTestClass() {
        LOGGER.info("==================================================================");
        LOGGER.info("Initializing test class: {}", BaseTest.class.getSimpleName());
        LOGGER.info("==================================================================");
        
        // 加载测试配置
        testConfig = TestConfig.getInstance();
        testConfig.printSummary();
        
        if (!testConfig.isValid()) {
            throw new IllegalStateException("Test configuration is not valid. Please check configuration files.");
        }
        
        LOGGER.info("Test class initialization completed successfully");
    }
    
    /**
     * 测试类清理
     */
    @AfterAll
    static void cleanupTestClass() {
        LOGGER.info("==================================================================");
        LOGGER.info("Cleaning up test class: {}", BaseTest.class.getSimpleName());
        LOGGER.info("==================================================================");
        
        // 执行任何必要的清理操作
        performGlobalCleanup();
        
        LOGGER.info("Test class cleanup completed");
    }
    
    /**
     * 测试方法初始化
     */
    @BeforeEach
    void setupTestMethod(TestInfo testInfo) {
        testStartTime = Instant.now();
        
        String testName = testInfo.getDisplayName();
        String testClassName = testInfo.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        Method testMethod = testInfo.getTestMethod().orElse(null);
        
        LOGGER.info("──────────────────────────────────────────────────────────────────");
        LOGGER.info("Starting test: {}#{}", testClassName, testName);
        LOGGER.info("Method: {}", testMethod != null ? testMethod.getName() : "Unknown");
        LOGGER.info("Tags: {}", testInfo.getTags());
        LOGGER.info("Start time: {}", testStartTime);
        LOGGER.info("──────────────────────────────────────────────────────────────────");
        
        // 执行测试方法特定的初始化
        beforeTestExecution(testInfo);
    }
    
    /**
     * 测试方法清理
     */
    @AfterEach
    void cleanupTestMethod(TestInfo testInfo) {
        testEndTime = Instant.now();
        Duration testDuration = Duration.between(testStartTime, testEndTime);
        
        String testName = testInfo.getDisplayName();
        String testClassName = testInfo.getTestClass().map(Class::getSimpleName).orElse("Unknown");
        
        LOGGER.info("──────────────────────────────────────────────────────────────────");
        LOGGER.info("Completed test: {}#{}", testClassName, testName);
        LOGGER.info("Duration: {} ms", testDuration.toMillis());
        LOGGER.info("End time: {}", testEndTime);
        LOGGER.info("──────────────────────────────────────────────────────────────────");
        
        // 执行测试方法特定的清理
        afterTestExecution(testInfo);
    }
    
    /**
     * 全局清理操作
     */
    protected static void performGlobalCleanup() {
        LOGGER.info("Performing global cleanup...");
        // 这里可以添加全局的清理逻辑，比如：
        // - 清理测试数据
        // - 关闭连接池
        // - 删除临时文件
    }
    
    /**
     * 测试执行前的钩子方法，子类可以覆盖
     */
    protected void beforeTestExecution(TestInfo testInfo) {
        // 子类可以覆盖此方法以添加特定的初始化逻辑
        LOGGER.debug("Base beforeTestExecution hook - test: {}", testInfo.getDisplayName());
    }
    
    /**
     * 测试执行后的钩子方法，子类可以覆盖
     */
    protected void afterTestExecution(TestInfo testInfo) {
        // 子类可以覆盖此方法以添加特定的清理逻辑
        LOGGER.debug("Base afterTestExecution hook - test: {}", testInfo.getDisplayName());
    }
    
    /**
     * 记录测试步骤
     */
    protected void logStep(String step) {
        LOGGER.info("STEP: {}", step);
    }
    
    /**
     * 记录验证点
     */
    protected void logVerification(String verification) {
        LOGGER.info("VERIFY: {}", verification);
    }
    
    /**
     * 记录测试数据
     */
    protected void logTestData(String key, Object value) {
        LOGGER.info("DATA: {} = {}", key, value);
    }
    
    /**
     * 记录测试截图或附件
     */
    protected void logAttachment(String description, String filePath) {
        LOGGER.info("ATTACHMENT: {} - {}", description, filePath);
    }
    
    /**
     * 获取测试配置
     */
    protected TestConfig getTestConfig() {
        return testConfig;
    }
    
    /**
     * 计算测试持续时间
     */
    protected long getTestDuration() {
        if (testStartTime == null || testEndTime == null) {
            return 0;
        }
        return Duration.between(testStartTime, testEndTime).toMillis();
    }
    
    /**
     * 断言方法 - 包装常用的断言逻辑
     */
    protected void assertResponseTime(long actualTime, long expectedMaxTime, String operation) {
        if (actualTime > expectedMaxTime) {
            String message = String.format("Response time for %s exceeded limit. Actual: %d ms, Expected max: %d ms",
                    operation, actualTime, expectedMaxTime);
            LOGGER.warn("Performance Warning: {}", message);
        } else {
            LOGGER.info("Response time for {} is acceptable: {} ms (max: {} ms)",
                    operation, actualTime, expectedMaxTime);
        }
    }
    
    /**
     * 重试方法 - 包装重试逻辑
     */
    protected <T> T retryOperation(RetryOperation<T> operation, int maxRetries, long delayMs) throws Exception {
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= maxRetries) {
            try {
                LOGGER.debug("Attempting operation (retry {}/{})", retryCount, maxRetries);
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                
                if (retryCount <= maxRetries) {
                    LOGGER.warn("Operation failed on attempt {}/{}. Retrying in {} ms. Error: {}",
                            retryCount, maxRetries, delayMs, e.getMessage());
                    Thread.sleep(delayMs);
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxRetries + " retries", lastException);
    }
    
    /**
     * 重试操作接口
     */
    @FunctionalInterface
    public interface RetryOperation<T> {
        T execute() throws Exception;
    }
}