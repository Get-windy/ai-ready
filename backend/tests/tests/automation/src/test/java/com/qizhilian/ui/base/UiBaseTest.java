package com.qizhilian.ui.base;

import com.qizhilian.config.TestConfig;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * UI测试基类
 * 提供WebDriver管理和基础UI操作方法
 */
@Slf4j
public abstract class UiBaseTest {
    
    protected static TestConfig config;
    protected WebDriver driver;
    protected WebDriverWait wait;
    
    @BeforeAll
    public static void globalSetup() {
        config = TestConfig.getInstance();
        log.info("UI测试环境初始化完成");
    }
    
    @BeforeEach
    public void setUp() {
        driver = createWebDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(config.getTimeout()));
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }
    
    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            log.info("WebDriver已关闭");
        }
    }
    
    /**
     * 创建WebDriver实例
     */
    @Step("初始化WebDriver: {browserType}")
    protected WebDriver createWebDriver() {
        String browserType = config.getBrowserType().toLowerCase();
        boolean headless = config.isHeadless();
        
        log.info("创建WebDriver: browser={}, headless={}", browserType, headless);
        
        switch (browserType) {
            case "chrome":
                return createChromeDriver(headless);
            case "firefox":
                return createFirefoxDriver(headless);
            case "edge":
                return createEdgeDriver(headless);
            default:
                log.warn("未知的浏览器类型: {}，使用默认Chrome", browserType);
                return createChromeDriver(headless);
        }
    }
    
    private WebDriver createChromeDriver(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        
        return new ChromeDriver(options);
    }
    
    private WebDriver createFirefoxDriver(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("--headless");
        }
        return new FirefoxDriver(options);
    }
    
    private WebDriver createEdgeDriver(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        return new EdgeDriver(options);
    }
    
    /**
     * 打开页面
     */
    @Step("打开页面: {url}")
    protected void open(String url) {
        driver.get(url);
        log.info("打开页面: {}", url);
    }
    
    /**
     * 打开相对路径
     */
    @Step("打开相对路径: {path}")
    protected void openPath(String path) {
        String fullUrl = config.getUiBaseUrl() + path;
        open(fullUrl);
    }
    
    /**
     * 获取当前URL
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
    
    /**
     * 获取页面标题
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }
    
    /**
     * 刷新页面
     */
    @Step("刷新页面")
    protected void refresh() {
        driver.navigate().refresh();
    }
    
    /**
     * 后退
     */
    @Step("浏览器后退")
    protected void back() {
        driver.navigate().back();
    }
    
    /**
     * 截图
     */
    @Step("截图")
    protected void takeScreenshot(String name) {
        // 截图逻辑，可集成Allure
        log.info("截图: {}", name);
    }
    
    /**
     * 等待指定时间
     */
    protected void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
