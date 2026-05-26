package com.aiedge.erp.invoice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Base class for all invoice management integration tests.
 * Provides common setup, teardown, and utilities for integration testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

    protected static final PostgreSQLContainer<?> postgresContainer;
    protected static final RabbitMQContainer rabbitmqContainer;
    protected static final WireMockServer wireMockServer;

    protected static final String FINANCE_SERVICE_MOCK_HOST = "localhost";
    protected static final int FINANCE_SERVICE_MOCK_PORT = 8081;
    protected static final String SALES_SERVICE_MOCK_HOST = "localhost";
    protected static final int SALES_SERVICE_MOCK_PORT = 8082;
    protected static final String PURCHASE_SERVICE_MOCK_HOST = "localhost";
    protected static final int PURCHASE_SERVICE_MOCK_PORT = 8083;
    protected static final String TAX_SERVICE_MOCK_HOST = "localhost";
    protected static final int TAX_SERVICE_MOCK_PORT = 8084;

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;
    protected ObjectMapper objectMapper;

    static {
        // Initialize PostgreSQL container
        postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("erp_invoice_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true);

        // Initialize RabbitMQ container
        rabbitmqContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"))
                .withExposedPorts(5672, 15672)
                .withReuse(true);

        // Initialize WireMock server for external service mocking
        wireMockServer = new WireMockServer(options()
                .port(FINANCE_SERVICE_MOCK_PORT)
                .bindAddress(FINANCE_SERVICE_MOCK_HOST));

        // Start containers
        postgresContainer.start();
        rabbitmqContainer.start();
        wireMockServer.start();

        // Configure WireMock for finance service
        configureFor(FINANCE_SERVICE_MOCK_HOST, FINANCE_SERVICE_MOCK_PORT);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);

        // RabbitMQ properties
        registry.add("spring.rabbitmq.host", rabbitmqContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitmqContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmqContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmqContainer::getAdminPassword);

        // External service URLs
        registry.add("integration.finance-service.url", 
                () -> "http://" + FINANCE_SERVICE_MOCK_HOST + ":" + FINANCE_SERVICE_MOCK_PORT);
        registry.add("integration.sales-service.url", 
                () -> "http://" + SALES_SERVICE_MOCK_HOST + ":" + SALES_SERVICE_MOCK_PORT);
        registry.add("integration.purchase-service.url", 
                () -> "http://" + PURCHASE_SERVICE_MOCK_HOST + ":" + PURCHASE_SERVICE_MOCK_PORT);
        registry.add("integration.tax-service.url", 
                () -> "http://" + TAX_SERVICE_MOCK_HOST + ":" + TAX_SERVICE_MOCK_PORT);

        // Test specific properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("logging.level.com.aiedge", () -> "DEBUG");
        registry.add("logging.level.org.springframework", () -> "INFO");
        registry.add("logging.level.org.hibernate", () -> "INFO");
    }

    @BeforeAll
    void setupAll() {
        logger.info("Setting up integration test environment...");
        logger.info("PostgreSQL URL: {}", postgresContainer.getJdbcUrl());
        logger.info("RabbitMQ URL: {}:{}", rabbitmqContainer.getHost(), rabbitmqContainer.getAmqpPort());
        logger.info("WireMock server started on port: {}", FINANCE_SERVICE_MOCK_PORT);
    }

    @BeforeEach
    void setup() {
        // Configure RestAssured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        // Configure ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Configure RestAssured to use our ObjectMapper
        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(new ObjectMapperConfig()
                        .jackson2ObjectMapperFactory((cls, charset) -> objectMapper));

        // Create request specification with common settings
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        // Reset WireMock stubs before each test
        wireMockServer.resetAll();

        logger.info("Test setup completed. Base URL: {}:{}{}", RestAssured.baseURI, RestAssured.port, RestAssured.basePath);
    }

    @AfterEach
    void cleanup() {
        logger.info("Cleaning up after test...");
        // Clean up any test data if needed
    }

    @AfterAll
    static void tearDownAll() {
        logger.info("Tearing down integration test environment...");
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
        // Note: Containers are automatically stopped by TestContainers
    }

    /**
     * Get the base URL for the application under test
     */
    protected String getBaseUrl() {
        return String.format("http://localhost:%d/api/v1", port);
    }

    /**
     * Get the URL for finance service mock
     */
    protected String getFinanceServiceUrl() {
        return String.format("http://%s:%d", FINANCE_SERVICE_MOCK_HOST, FINANCE_SERVICE_MOCK_PORT);
    }

    /**
     * Get the URL for sales service mock
     */
    protected String getSalesServiceUrl() {
        return String.format("http://%s:%d", SALES_SERVICE_MOCK_HOST, SALES_SERVICE_MOCK_PORT);
    }

    /**
     * Get the URL for purchase service mock
     */
    protected String getPurchaseServiceUrl() {
        return String.format("http://%s:%d", PURCHASE_SERVICE_MOCK_HOST, PURCHASE_SERVICE_MOCK_PORT);
    }

    /**
     * Get the URL for tax service mock
     */
    protected String getTaxServiceUrl() {
        return String.format("http://%s:%d", TAX_SERVICE_MOCK_HOST, TAX_SERVICE_MOCK_PORT);
    }

    /**
     * Wait for a condition with timeout
     */
    protected void waitForCondition(Runnable conditionCheck, String conditionDescription, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                conditionCheck.run();
                return;
            } catch (AssertionError e) {
                // Condition not met yet, continue waiting
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for condition: " + conditionDescription, ie);
                }
            }
        }
        throw new AssertionError("Condition not met after " + timeoutMillis + "ms: " + conditionDescription);
    }

    /**
     * Generate a unique test identifier
     */
    protected String generateTestId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * Log test step for better test reporting
     */
    protected void logTestStep(String step) {
        logger.info("TEST STEP: {}", step);
    }
}