package com.aiedge.erp.test.framework;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * 基础集成测试类
 * 提供测试容器支持、REST API配置、测试数据管理等基础功能
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    // PostgreSQL测试容器
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("erp_test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    // RabbitMQ测试容器
    @Container
    static final RabbitMQContainer rabbitMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3-management-alpine"))
            .withExposedPorts(5672, 15672)
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // 配置数据库连接
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // 配置RabbitMQ连接
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");

        // 配置Flyway
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @BeforeAll
    static void beforeAll() {
        log.info("Starting test containers...");
        // 确保容器已启动
        if (!postgres.isRunning()) {
            postgres.start();
        }
        if (!rabbitMQ.isRunning()) {
            rabbitMQ.start();
        }
        log.info("Test containers started successfully");
    }

    @BeforeEach
    void setUp() {
        // 配置RestAssured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api";

        // 配置日志（仅在测试失败时记录）
        RestAssured.filters(
                new RequestLoggingFilter(),
                new ResponseLoggingFilter()
        );

        // 配置请求/响应规范
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        log.info("Test setup complete. Base URL: http://localhost:{}/api", port);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cleanupTestData();
        
        // 重置RestAssured配置
        RestAssured.reset();
        
        log.info("Test teardown complete");
    }

    /**
     * 清理测试数据
     * 子类可以重写此方法以实现特定的数据清理逻辑
     */
    protected void cleanupTestData() {
        log.debug("Cleaning up test data...");
        // 默认实现为空，子类可以覆盖
    }

    /**
     * 获取API基础路径
     */
    protected String getApiPath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 等待异步任务完成
     */
    protected void waitForAsyncTask(int timeoutSeconds) {
        try {
            Thread.sleep(timeoutSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待异步任务时被中断", e);
        }
    }

    /**
     * 验证API响应状态
     */
    protected void verifyApiResponse(int expectedStatus, io.restassured.response.Response response) {
        if (response.getStatusCode() != expectedStatus) {
            log.error("API响应状态不正确. 预期: {}, 实际: {}, 响应体: {}",
                    expectedStatus, response.getStatusCode(), response.getBody().asString());
            throw new AssertionError(String.format(
                    "API响应状态不正确. 预期: %d, 实际: %d", expectedStatus, response.getStatusCode()));
        }
    }

    /**
     * 创建测试数据唯一标识
     */
    protected String createUniqueIdentifier(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 获取PostgreSQL容器实例（用于直接数据库操作）
     */
    protected PostgreSQLContainer<?> getPostgresContainer() {
        return postgres;
    }

    /**
     * 获取RabbitMQ容器实例
     */
    protected RabbitMQContainer getRabbitMQContainer() {
        return rabbitMQ;
    }
}