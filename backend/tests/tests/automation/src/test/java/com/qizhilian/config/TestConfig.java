package com.qizhilian.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 测试配置管理类
 * 负责加载和管理测试环境配置
 */
@Slf4j
public class TestConfig {
    
    private static final String CONFIG_FILE = "config/test.properties";
    private static final Properties properties = new Properties();
    private static volatile TestConfig instance;
    
    private TestConfig() {
        loadConfig();
    }
    
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
    
    private void loadConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                log.info("测试配置加载成功: {}", CONFIG_FILE);
            } else {
                log.warn("配置文件未找到: {}，使用默认配置", CONFIG_FILE);
                loadDefaultConfig();
            }
        } catch (IOException e) {
            log.error("加载配置文件失败: {}", e.getMessage());
            loadDefaultConfig();
        }
    }
    
    private void loadDefaultConfig() {
        // 默认配置
        properties.setProperty("base.url", "http://localhost:8080");
        properties.setProperty("api.base.path", "/api/v1");
        properties.setProperty("ui.base.url", "http://localhost:3000");
        properties.setProperty("timeout.seconds", "30");
        properties.setProperty("retry.count", "3");
        properties.setProperty("browser.type", "chrome");
        properties.setProperty("headless.mode", "false");
        properties.setProperty("mobile.platform", "android");
        properties.setProperty("appium.server.url", "http://localhost:4723");
    }
    
    public String getBaseUrl() {
        return getProperty("base.url", "http://localhost:8080");
    }
    
    public String getApiBasePath() {
        return getProperty("api.base.path", "/api/v1");
    }
    
    public String getFullApiUrl() {
        return getBaseUrl() + getApiBasePath();
    }
    
    public String getUiBaseUrl() {
        return getProperty("ui.base.url", "http://localhost:3000");
    }
    
    public int getTimeout() {
        return Integer.parseInt(getProperty("timeout.seconds", "30"));
    }
    
    public int getRetryCount() {
        return Integer.parseInt(getProperty("retry.count", "3"));
    }
    
    public String getBrowserType() {
        return getProperty("browser.type", "chrome");
    }
    
    public boolean isHeadless() {
        return Boolean.parseBoolean(getProperty("headless.mode", "false"));
    }
    
    public String getMobilePlatform() {
        return getProperty("mobile.platform", "android");
    }
    
    public String getAppiumServerUrl() {
        return getProperty("appium.server.url", "http://localhost:4723");
    }
    
    public String getProperty(String key) {
        // 优先从系统属性读取（支持命令行覆盖）
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // 其次从环境变量读取
        value = System.getenv(key.toUpperCase().replace(".", "_"));
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // 最后从配置文件读取
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
}
