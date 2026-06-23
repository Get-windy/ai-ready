package cn.aiedge.erp.purchase.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 基础集成测试类
 * 配置测试容器环境（PostgreSQL、RabbitMQ、Redis）
 * 提供通用的测试基础设施
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:14-alpine"))
            .withDatabaseName("erp_purchase_test")
            .withUsername("test")
            .withPassword("test123");

    @Container
    static final RabbitMQContainer rabbitMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3-management-alpine"))
            .withExposedPorts(5672, 15672)
            .withUser("guest", "guest");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 数据库配置
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // RabbitMQ配置
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);

        // Redis配置
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        // 应用特定配置
        registry.add("app.integration-test.enabled", () -> "true");
        registry.add("app.test-containers.postgres.enabled", () -> "true");
        registry.add("app.test-containers.rabbitmq.enabled", () -> "true");
        registry.add("app.test-containers.redis.enabled", () -> "true");
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("Starting test containers...");
        System.out.println("PostgreSQL URL: " + postgres.getJdbcUrl());
        System.out.println("RabbitMQ Host: " + rabbitMQ.getHost());
        System.out.println("Redis Host: " + redis.getHost());
    }

    @BeforeEach
    void setupDatabase(DataSource dataSource) throws SQLException {
        // 清理并准备测试数据
        cleanupDatabase(dataSource);
        setupTestData(dataSource);
    }

    private void cleanupDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 禁用外键约束
            stmt.execute("SET session_replication_role = 'replica'");
            
            // 清理所有表数据（按依赖顺序）
            String[] tables = {
                "purchase_contract_items",
                "purchase_contracts",
                "purchase_quote_items",
                "purchase_supplier_quotes",
                "purchase_inquiry_items",
                "purchase_inquiries",
                "purchase_suppliers",
                "purchase_products"
            };
            
            for (String table : tables) {
                stmt.execute("DELETE FROM " + table);
            }
            
            // 重新启用外键约束
            stmt.execute("SET session_replication_role = 'origin'");
            
            // 重置序列
            String[] sequences = {
                "purchase_inquiries_id_seq",
                "purchase_inquiry_items_id_seq",
                "purchase_supplier_quotes_id_seq",
                "purchase_quote_items_id_seq",
                "purchase_contracts_id_seq",
                "purchase_contract_items_id_seq"
            };
            
            for (String sequence : sequences) {
                stmt.execute("ALTER SEQUENCE " + sequence + " RESTART WITH 1");
            }
        }
    }

    private void setupTestData(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 插入基础测试数据
            stmt.execute("""
                INSERT INTO purchase_suppliers (id, name, code, contact_person, phone, email, status) 
                VALUES 
                (1, '供应商A', 'SUP-001', '张三', '13800138001', 'supplier_a@example.com', 'ACTIVE'),
                (2, '供应商B', 'SUP-002', '李四', '13800138002', 'supplier_b@example.com', 'ACTIVE'),
                (3, '供应商C', 'SUP-003', '王五', '13800138003', 'supplier_c@example.com', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
            """);
            
            stmt.execute("""
                INSERT INTO purchase_products (id, product_code, product_name, unit, category, status) 
                VALUES 
                (1, 'PROD-001', '服务器X1', '台', 'IT设备', 'ACTIVE'),
                (2, 'PROD-002', '办公电脑', '台', 'IT设备', 'ACTIVE'),
                (3, 'PROD-003', '办公桌椅', '套', '办公家具', 'ACTIVE')
                ON CONFLICT (id) DO NOTHING
            """);
        }
    }

    @AfterAll
    static void afterAll() {
        System.out.println("Test containers will be stopped automatically by Testcontainers");
    }
}