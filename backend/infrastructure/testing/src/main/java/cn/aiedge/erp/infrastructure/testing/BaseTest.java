package cn.aiedge.erp.infrastructure.testing;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * 基础测试类
 * 提供所有测试类共用的功能
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public abstract class BaseTest {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Container
    protected static final PostgreSQLContainer<?> postgresContainer = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected TestDataManager testDataManager;
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", 
            () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.redis.host", () -> "localhost");
        registry.add("spring.redis.port", () -> "6379");
    }
    
    @BeforeAll
    static void beforeAll() {
        logger.info("PostgreSQL容器启动中...");
        postgresContainer.start();
        
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(postgresContainer::isRunning);
        
        logger.info("PostgreSQL容器启动完成，URL: {}", postgresContainer.getJdbcUrl());
    }
    
    @AfterAll
    static void afterAll() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            logger.info("停止PostgreSQL容器");
            postgresContainer.stop();
        }
    }
    
    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        logger.info("开始执行测试: {} - {}", 
            testInfo.getDisplayName(), testInfo.getTags());
        
        // 清理测试数据
        testDataManager.cleanupAllTestData();
        
        // 插入基础测试数据
        setupTestData();
    }
    
    @AfterEach
    void afterEach(TestInfo testInfo) {
        logger.info("测试执行完成: {} - 状态: {}", 
            testInfo.getDisplayName(), getTestStatus(testInfo));
        
        // 可选：清理测试数据
        if (isCleanupEnabled()) {
            testDataManager.cleanupAllTestData();
        }
    }
    
    /**
     * 设置测试数据
     */
    protected void setupTestData() {
        // 基础测试数据
        testDataManager.insertTestData("user_accounts", 
            testDataManager.getTestData("users"));
        
        testDataManager.insertTestData("product_inventory",
            testDataManager.getTestData("products"));
        
        logger.info("测试数据设置完成");
    }
    
    /**
     * 获取测试状态
     */
    private String getTestStatus(TestInfo testInfo) {
        // 这里可以根据实际情况返回测试状态
        return "COMPLETED";
    }
    
    /**
     * 是否启用清理
     */
    protected boolean isCleanupEnabled() {
        return true;
    }
    
    /**
     * 等待条件满足
     */
    protected void waitForCondition(String description, Runnable conditionCheck) {
        try {
            await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    try {
                        conditionCheck.run();
                    } catch (Exception e) {
                        throw new AssertionError("条件检查失败: " + e.getMessage(), e);
                    }
                });
            logger.info("条件满足: {}", description);
        } catch (Exception e) {
            logger.error("等待条件超时: {}", description, e);
            throw new AssertionError("等待条件超时: " + description, e);
        }
    }
    
    /**
     * 记录测试步骤
     */
    protected void logStep(String step) {
        logger.info("测试步骤: {}", step);
    }
    
    /**
     * 记录测试断言
     */
    protected void logAssertion(String assertion) {
        logger.info("测试断言: {}", assertion);
    }
    
    /**
     * 记录测试数据
     */
    protected void logTestData(String dataName, Object data) {
        logger.info("测试数据 {}: {}", dataName, data);
    }
    
    /**
     * 性能监控
     */
    protected void measurePerformance(String operationName, Runnable operation) {
        long startTime = System.nanoTime();
        try {
            operation.run();
        } finally {
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // 转换为毫秒
            logger.info("操作 {} 执行时间: {}ms", operationName, duration);
            
            // 性能断言
            Assertions.assertTrue(duration < 1000, 
                String.format("操作 %s 执行时间过长: %dms", operationName, duration));
        }
    }
}