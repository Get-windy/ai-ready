package cn.aiedge.erp.purchase.contract.test.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 环境配置管理类
 * 负责加载和管理测试环境配置
 */
@Data
@Slf4j
public class EnvironmentConfig {
    
    private static final String CONFIG_FILE = "application-test.yml";
    private static final String ENV_PREFIX = "ERP_TEST_";
    private static EnvironmentConfig instance;
    
    // 基础配置
    private String environment;
    private String baseUrl;
    private boolean sslEnabled;
    private int connectionTimeout;
    private int readTimeout;
    
    // 数据库配置
    private String databaseUrl;
    private String databaseUsername;
    private String databasePassword;
    private String databaseDriver;
    private int connectionPoolSize;
    
    // 认证配置
    private String authType;
    private String authToken;
    private String apiKey;
    private String username;
    private String password;
    private String jwtSecret;
    
    // 测试配置
    private String testDataDir;
    private String reportDir;
    private boolean screenshotEnabled;
    private boolean videoRecordingEnabled;
    private boolean headlessMode;
    
    // 并发配置
    private int maxThreads;
    private int threadPoolSize;
    private int retryCount;
    private int retryDelayMs;
    
    // 性能配置
    private int responseTimeThreshold;
    private int throughputThreshold;
    private int errorRateThreshold;
    
    // 通知配置
    private String notificationEmail;
    private String slackWebhookUrl;
    private boolean emailNotificationEnabled;
    
    private EnvironmentConfig() {
        loadConfig();
    }
    
    public static synchronized EnvironmentConfig getInstance() {
        if (instance == null) {
            instance = new EnvironmentConfig();
        }
        return instance;
    }
    
    /**
     * 加载配置文件和环境变量
     */
    private void loadConfig() {
        // 从环境变量加载（优先级最高）
        loadFromEnvironment();
        
        // 从YAML配置文件加载
        loadFromYamlConfig();
        
        // 设置默认值
        setDefaultValues();
        
        log.info("环境配置加载完成: {}", this);
    }
    
    /**
     * 从环境变量加载配置
     */
    private void loadFromEnvironment() {
        environment = getEnv("ENVIRONMENT", "test");
        baseUrl = getEnv("BASE_URL", "http://localhost:8080");
        sslEnabled = Boolean.parseBoolean(getEnv("SSL_ENABLED", "false"));
        connectionTimeout = Integer.parseInt(getEnv("CONNECTION_TIMEOUT", "30000"));
        readTimeout = Integer.parseInt(getEnv("READ_TIMEOUT", "60000"));
        
        // 数据库配置
        databaseUrl = getEnv("DATABASE_URL", "jdbc:h2:mem:testdb");
        databaseUsername = getEnv("DATABASE_USERNAME", "sa");
        databasePassword = getEnv("DATABASE_PASSWORD", "");
        databaseDriver = getEnv("DATABASE_DRIVER", "org.h2.Driver");
        connectionPoolSize = Integer.parseInt(getEnv("CONNECTION_POOL_SIZE", "10"));
        
        // 认证配置
        authType = getEnv("AUTH_TYPE", "jwt");
        authToken = getEnv("AUTH_TOKEN", "");
        apiKey = getEnv("API_KEY", "");
        username = getEnv("USERNAME", "test");
        password = getEnv("PASSWORD", "test123");
        jwtSecret = getEnv("JWT_SECRET", "test-secret-key");
        
        // 测试配置
        testDataDir = getEnv("TEST_DATA_DIR", "src/test/resources/test-data");
        reportDir = getEnv("REPORT_DIR", "target/reports");
        screenshotEnabled = Boolean.parseBoolean(getEnv("SCREENSHOT_ENABLED", "true"));
        videoRecordingEnabled = Boolean.parseBoolean(getEnv("VIDEO_RECORDING_ENABLED", "false"));
        headlessMode = Boolean.parseBoolean(getEnv("HEADLESS_MODE", "true"));
        
        // 并发配置
        maxThreads = Integer.parseInt(getEnv("MAX_THREADS", "10"));
        threadPoolSize = Integer.parseInt(getEnv("THREAD_POOL_SIZE", "5"));
        retryCount = Integer.parseInt(getEnv("RETRY_COUNT", "3"));
        retryDelayMs = Integer.parseInt(getEnv("RETRY_DELAY_MS", "1000"));
        
        // 性能配置
        responseTimeThreshold = Integer.parseInt(getEnv("RESPONSE_TIME_THRESHOLD", "5000"));
        throughputThreshold = Integer.parseInt(getEnv("THROUGHPUT_THRESHOLD", "100"));
        errorRateThreshold = Integer.parseInt(getEnv("ERROR_RATE_THRESHOLD", "5"));
        
        // 通知配置
        notificationEmail = getEnv("NOTIFICATION_EMAIL", "");
        slackWebhookUrl = getEnv("SLACK_WEBHOOK_URL", "");
        emailNotificationEnabled = Boolean.parseBoolean(getEnv("EMAIL_NOTIFICATION_ENABLED", "false"));
    }
    
    /**
     * 从YAML配置文件加载
     */
    private void loadFromYamlConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                // 这里使用简单的Properties解析，实际项目中可以使用SnakeYAML等库
                Properties props = new Properties();
                // 由于是YAML文件，这里简化为Properties解析
                // 实际实现应该使用YAML解析器
                log.warn("YAML配置文件解析需要实现YAML解析器，当前使用环境变量");
            } else {
                log.warn("配置文件 {} 未找到，使用默认值", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("读取配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 设置默认值
     */
    private void setDefaultValues() {
        if (environment == null) environment = "test";
        if (baseUrl == null) baseUrl = "http://localhost:8080";
        if (databaseUrl == null) databaseUrl = "jdbc:h2:mem:testdb";
        if (databaseUsername == null) databaseUsername = "sa";
        if (databaseDriver == null) databaseDriver = "org.h2.Driver";
        if (testDataDir == null) testDataDir = "src/test/resources/test-data";
        if (reportDir == null) reportDir = "target/reports";
        if (authType == null) authType = "jwt";
    }
    
    /**
     * 获取环境变量值
     */
    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(ENV_PREFIX + key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key.toLowerCase().replace("_", "."), defaultValue);
        }
        return value;
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean validate() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.error("基础URL配置为空");
            return false;
        }
        
        if (environment == null || environment.trim().isEmpty()) {
            log.error("环境配置为空");
            return false;
        }
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            log.error("数据库URL配置为空");
            return false;
        }
        
        log.info("环境配置验证通过");
        return true;
    }
    
    /**
     * 获取完整的基础URL
     */
    public String getFullBaseUrl() {
        if (sslEnabled && !baseUrl.startsWith("https://")) {
            return baseUrl.replace("http://", "https://");
        } else if (!sslEnabled && baseUrl.startsWith("https://")) {
            return baseUrl.replace("https://", "http://");
        }
        return baseUrl;
    }
    
    /**
     * 获取数据库连接URL
     */
    public String getDatabaseConnectionUrl() {
        return databaseUrl + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    }
    
    /**
     * 获取测试数据文件路径
     */
    public String getTestDataFilePath(String fileName) {
        return testDataDir + "/" + fileName;
    }
    
    /**
     * 获取报告文件路径
     */
    public String getReportFilePath(String fileName) {
        return reportDir + "/" + fileName;
    }
    
    /**
     * 检查是否为生产环境
     */
    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment) || 
               "prod".equalsIgnoreCase(environment);
    }
    
    /**
     * 检查是否为测试环境
     */
    public boolean isTest() {
        return "test".equalsIgnoreCase(environment) || 
               "dev".equalsIgnoreCase(environment) ||
               "qa".equalsIgnoreCase(environment);
    }
    
    /**
     * 检查是否启用调试模式
     */
    public boolean isDebug() {
        return "debug".equalsIgnoreCase(environment) || 
               "local".equalsIgnoreCase(environment);
    }
    
    /**
     * 获取身份验证头部信息
     */
    public String getAuthHeader() {
        switch (authType.toLowerCase()) {
            case "jwt":
                return "Bearer " + authToken;
            case "apikey":
                return apiKey;
            case "basic":
                String credentials = username + ":" + password;
                return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            default:
                return "";
        }
    }
    
    /**
     * 获取认证类型描述
     */
    public String getAuthTypeDescription() {
        switch (authType.toLowerCase()) {
            case "jwt":
                return "JSON Web Token认证";
            case "apikey":
                return "API密钥认证";
            case "basic":
                return "Basic认证";
            case "oauth2":
                return "OAuth 2.0认证";
            default:
                return "未知认证类型";
        }
    }
    
    /**
     * 配置信息摘要
     */
    @Override
    public String toString() {
        return String.format(
            "EnvironmentConfig{environment='%s', baseUrl='%s', databaseUrl='%s', authType='%s', testDataDir='%s'}",
            environment, baseUrl, databaseUrl, authType, testDataDir
        );
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        synchronized (EnvironmentConfig.class) {
            instance = null;
            getInstance();
            log.info("环境配置已重新加载");
        }
    }
}