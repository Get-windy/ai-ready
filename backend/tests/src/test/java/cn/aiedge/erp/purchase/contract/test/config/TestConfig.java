package cn.aiedge.erp.purchase.contract.test.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * 测试配置管理类
 * 负责加载和管理测试环境配置
 */
@Data
public class TestConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestConfig.class);
    private static TestConfig instance;
    
    private String environment;
    private String baseUrl;
    private DatabaseConfig database;
    private TimeoutConfig timeout;
    private ReportConfig report;
    private UserConfig user;
    
    // 数据库配置
    @Data
    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private Integer maxPoolSize;
        private Integer connectionTimeout;
    }
    
    // 超时配置
    @Data
    public static class TimeoutConfig {
        private Integer implicit;
        private Integer pageLoad;
        private Integer script;
        private Integer api;
        private Integer database;
    }
    
    // 报告配置
    @Data
    public static class ReportConfig {
        private String outputDir;
        private String format;
        private Boolean attachments;
        private String theme;
        private Boolean detailedSteps;
    }
    
    // 用户配置
    @Data
    public static class UserConfig {
        private String adminUsername;
        private String adminPassword;
        private String normalUsername;
        private String normalPassword;
        private String testUsername;
        private String testPassword;
    }
    
    private TestConfig() {
        loadConfiguration();
    }
    
    /**
     * 获取配置单例
     */
    public static TestConfig getInstance() {
        if (instance == null) {
            synchronized (TestConfig.class) {
                if (instance == null) {
                    instance = new TestConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载配置
     */
    private void loadConfiguration() {
        try {
            String configFile = getConfigFileName();
            LOGGER.info("Loading test configuration from: {}", configFile);
            
            Properties properties = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
                if (input != null) {
                    properties.load(input);
                } else {
                    LOGGER.warn("Config file not found, using default configuration");
                    loadDefaultConfiguration();
                    return;
                }
            }
            
            // 加载基本配置
            this.environment = properties.getProperty("test.environment", "test");
            this.baseUrl = properties.getProperty("test.base-url", "http://localhost:8080");
            
            // 加载数据库配置
            this.database = new DatabaseConfig();
            this.database.setUrl(properties.getProperty("test.database.url", "jdbc:mysql://localhost:3306/erp_test"));
            this.database.setUsername(properties.getProperty("test.database.username", "root"));
            this.database.setPassword(properties.getProperty("test.database.password", "password"));
            this.database.setDriverClassName(properties.getProperty("test.database.driver", "com.mysql.cj.jdbc.Driver"));
            this.database.setMaxPoolSize(Integer.parseInt(properties.getProperty("test.database.max-pool-size", "10")));
            this.database.setConnectionTimeout(Integer.parseInt(properties.getProperty("test.database.connection-timeout", "30000")));
            
            // 加载超时配置
            this.timeout = new TimeoutConfig();
            this.timeout.setImplicit(Integer.parseInt(properties.getProperty("test.timeout.implicit", "10")));
            this.timeout.setPageLoad(Integer.parseInt(properties.getProperty("test.timeout.page-load", "30")));
            this.timeout.setScript(Integer.parseInt(properties.getProperty("test.timeout.script", "60")));
            this.timeout.setApi(Integer.parseInt(properties.getProperty("test.timeout.api", "30")));
            this.timeout.setDatabase(Integer.parseInt(properties.getProperty("test.timeout.database", "10")));
            
            // 加载报告配置
            this.report = new ReportConfig();
            this.report.setOutputDir(properties.getProperty("test.report.output-dir", "target/reports/"));
            this.report.setFormat(properties.getProperty("test.report.format", "html,json"));
            this.report.setAttachments(Boolean.parseBoolean(properties.getProperty("test.report.attachments", "true")));
            this.report.setTheme(properties.getProperty("test.report.theme", "default"));
            this.report.setDetailedSteps(Boolean.parseBoolean(properties.getProperty("test.report.detailed-steps", "true")));
            
            // 加载用户配置
            this.user = new UserConfig();
            this.user.setAdminUsername(properties.getProperty("test.user.admin.username", "admin"));
            this.user.setAdminPassword(properties.getProperty("test.user.admin.password", "admin123"));
            this.user.setNormalUsername(properties.getProperty("test.user.normal.username", "user"));
            this.user.setNormalPassword(properties.getProperty("test.user.normal.password", "user123"));
            this.user.setTestUsername(properties.getProperty("test.user.test.username", "tester"));
            this.user.setTestPassword(properties.getProperty("test.user.test.password", "tester123"));
            
            LOGGER.info("Configuration loaded successfully for environment: {}", this.environment);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load test configuration", e);
            loadDefaultConfiguration();
        }
    }
    
    /**
     * 根据环境获取配置文件名称
     */
    private String getConfigFileName() {
        String env = System.getProperty("test.environment", "test");
        return String.format("application-%s.yml", env);
    }
    
    /**
     * 加载默认配置
     */
    private void loadDefaultConfiguration() {
        LOGGER.info("Loading default test configuration");
        
        this.environment = "test";
        this.baseUrl = "http://localhost:8080";
        
        this.database = new DatabaseConfig();
        this.database.setUrl("jdbc:mysql://localhost:3306/erp_test");
        this.database.setUsername("root");
        this.database.setPassword("password");
        this.database.setDriverClassName("com.mysql.cj.jdbc.Driver");
        this.database.setMaxPoolSize(10);
        this.database.setConnectionTimeout(30000);
        
        this.timeout = new TimeoutConfig();
        this.timeout.setImplicit(10);
        this.timeout.setPageLoad(30);
        this.timeout.setScript(60);
        this.timeout.setApi(30);
        this.timeout.setDatabase(10);
        
        this.report = new ReportConfig();
        this.report.setOutputDir("target/reports/");
        this.report.setFormat("html,json");
        this.report.setAttachments(true);
        this.report.setTheme("default");
        this.report.setDetailedSteps(true);
        
        this.user = new UserConfig();
        this.user.setAdminUsername("admin");
        this.user.setAdminPassword("admin123");
        this.user.setNormalUsername("user");
        this.user.setNormalPassword("user123");
        this.user.setTestUsername("tester");
        this.user.setTestPassword("tester123");
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfiguration();
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isValid() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            LOGGER.error("Base URL is not configured");
            return false;
        }
        
        if (database == null || database.getUrl() == null || database.getUrl().trim().isEmpty()) {
            LOGGER.error("Database configuration is not valid");
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取API完整URL
     */
    public String getApiUrl(String endpoint) {
        if (endpoint.startsWith("/")) {
            return baseUrl + endpoint;
        }
        return baseUrl + "/" + endpoint;
    }
    
    /**
     * 获取模块特定的API URL
     */
    public String getPurchaseContractApiUrl(String endpoint) {
        String baseEndpoint = "/api/erp/purchase-contract/v1";
        if (endpoint.startsWith("/")) {
            return getApiUrl(baseEndpoint + endpoint);
        }
        return getApiUrl(baseEndpoint + "/" + endpoint);
    }
    
    /**
     * 打印配置摘要
     */
    public void printSummary() {
        LOGGER.info("=== Test Configuration Summary ===");
        LOGGER.info("Environment: {}", environment);
        LOGGER.info("Base URL: {}", baseUrl);
        LOGGER.info("Database URL: {}", database.getUrl());
        LOGGER.info("API Timeout: {} seconds", timeout.getApi());
        LOGGER.info("Report Output: {}", report.getOutputDir());
        LOGGER.info("==================================");
    }
}