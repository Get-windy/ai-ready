package cn.aiedge.erp.infrastructure.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 测试配置类
 * 提供测试环境所需的Bean配置
 */
@TestConfiguration
public class TestConfiguration {
    
    /**
     * 嵌入式数据库数据源（用于单元测试）
     */
    @Bean(name = "testDataSource")
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .addScript("classpath:schema.sql")
            .addScript("classpath:test-data.sql")
            .build();
    }
    
    /**
     * 测试JdbcTemplate
     */
    @Bean(name = "testJdbcTemplate")
    @Primary
    public JdbcTemplate testJdbcTemplate(DataSource testDataSource) {
        return new JdbcTemplate(testDataSource);
    }
    
    /**
     * 测试事务模板
     */
    @Bean(name = "testTransactionTemplate")
    @Primary
    public TransactionTemplate testTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
    
    /**
     * 测试数据管理器
     */
    @Bean
    public TestDataManager testDataManager(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate) {
        return new TestDataManager(jdbcTemplate, transactionTemplate, "test-module");
    }
    
    /**
     * 异步测试执行器
     */
    @Bean(name = "testExecutor")
    public ExecutorService testExecutor() {
        return Executors.newFixedThreadPool(10);
    }
    
    /**
     * 测试属性配置
     */
    @Bean
    public TestProperties testProperties() {
        TestProperties properties = new TestProperties();
        properties.setTestMode("unit");
        properties.setMaxRetryCount(3);
        properties.setTimeoutSeconds(30);
        properties.setEnableMock(true);
        properties.setEnableCleanup(true);
        return properties;
    }
    
    /**
     * 测试属性类
     */
    public static class TestProperties {
        private String testMode;
        private int maxRetryCount;
        private int timeoutSeconds;
        private boolean enableMock;
        private boolean enableCleanup;
        
        public String getTestMode() {
            return testMode;
        }
        
        public void setTestMode(String testMode) {
            this.testMode = testMode;
        }
        
        public int getMaxRetryCount() {
            return maxRetryCount;
        }
        
        public void setMaxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
        }
        
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }
        
        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public boolean isEnableMock() {
            return enableMock;
        }
        
        public void setEnableMock(boolean enableMock) {
            this.enableMock = enableMock;
        }
        
        public boolean isEnableCleanup() {
            return enableCleanup;
        }
        
        public void setEnableCleanup(boolean enableCleanup) {
            this.enableCleanup = enableCleanup;
        }
    }
    
    /**
     * 测试常量定义
     */
    public static class TestConstants {
        public static final String TEST_ENVIRONMENT = "TEST";
        public static final String INTEGRATION_TEST_ENVIRONMENT = "INTEGRATION";
        public static final String E2E_TEST_ENVIRONMENT = "E2E";
        
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        
        public static final String TEST_USER_ID = "test_user_001";
        public static final String TEST_TENANT_ID = "test_tenant_001";
        public static final String TEST_ORDER_ID = "test_order_001";
        public static final String TEST_PRODUCT_ID = "test_product_001";
        
        public static final String[] REQUIRED_TEST_TABLES = {
            "batch_management",
            "order_details", 
            "product_inventory",
            "user_accounts"
        };
    }
}