package com.qizhilian.api.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * API测试配置管理类
 * 负责加载和管理测试配置文件
 */
@Slf4j
@Getter
public class ApiConfig {
    
    private static final String CONFIG_FILE = "application.properties";
    private static ApiConfig instance;
    private final Properties properties;
    
    // 配置属性
    private final String baseUrl;
    private final String apiVersion;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final int retryMaxAttempts;
    private final int retryIntervalMs;
    private final String authTokenHeader;
    private final String authTokenPrefix;
    private final String testDataLocale;
    
    private ApiConfig() {
        properties = new Properties();
        loadProperties();
        
        // 初始化配置
        baseUrl = getProperty("base.url", "http://localhost:8080");
        apiVersion = getProperty("api.version", "/api/v1");
        connectionTimeout = getIntProperty("connection.timeout", 10000);
        socketTimeout = getIntProperty("socket.timeout", 30000);
        retryMaxAttempts = getIntProperty("retry.max.attempts", 3);
        retryIntervalMs = getIntProperty("retry.interval.ms", 1000);
        authTokenHeader = getProperty("auth.token.header", "Authorization");
        authTokenPrefix = getProperty("auth.token.prefix", "Bearer");
        testDataLocale = getProperty("test.data.locale", "zh-CN");
    }
    
    public static synchronized ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
        }
        return instance;
    }
    
    private void loadProperties() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                log.info("成功加载配置文件: {}", CONFIG_FILE);
            } else {
                log.warn("配置文件 {} 未找到，使用默认配置", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("加载配置文件失败: {}", e.getMessage());
        }
    }
    
    public String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key, defaultValue);
        }
        return value;
    }
    
    public String getProperty(String key) {
        return getProperty(key, null);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置项 {} 的值 '{}' 不是有效的整数，使用默认值 {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * 获取完整的API URL
     */
    public String getFullApiUrl(String endpoint) {
        String url = baseUrl + apiVersion + endpoint;
        log.debug("构建API URL: {}", url);
        return url;
    }
    
    /**
     * 重置配置实例（用于测试）
     */
    public static void reset() {
        instance = null;
    }
}
