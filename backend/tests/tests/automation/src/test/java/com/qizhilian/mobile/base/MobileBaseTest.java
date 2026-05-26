package com.qizhilian.mobile.base;

import com.qizhilian.config.TestConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * 移动端测试基类
 * 提供Appium Driver管理和基础移动端操作方法
 */
@Slf4j
public abstract class MobileBaseTest {
    
    protected static TestConfig config;
    protected AppiumDriver driver;
    
    @BeforeAll
    public static void globalSetup() {
        config = TestConfig.getInstance();
        log.info("移动端测试环境初始化完成");
    }
    
    @BeforeEach
    public void setUp() {
        driver = createDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            log.info("Appium Driver已关闭");
        }
    }
    
    /**
     * 创建Appium Driver实例
     */
    @Step("初始化Appium Driver: {platform}")
    protected AppiumDriver createDriver() {
        String platform = config.getMobilePlatform().toLowerCase();
        String serverUrl = config.getAppiumServerUrl();
        
        log.info("创建Appium Driver: platform={}, server={}", platform, serverUrl);
        
        try {
            URL url = new URL(serverUrl);
            
            switch (platform) {
                case "android":
                    return createAndroidDriver(url);
                case "ios":
                    return createIOSDriver(url);
                default:
                    log.warn("未知的平台类型: {}，使用默认Android", platform);
                    return createAndroidDriver(url);
            }
        } catch (MalformedURLException e) {
            log.error("Appium Server URL格式错误: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 创建Android Driver
     */
    private AndroidDriver createAndroidDriver(URL url) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setPlatformVersion(config.getProperty("android.platform.version", "13"))
                .setDeviceName(config.getProperty("android.device.name", "Pixel_4"))
                .setApp(config.getProperty("android.app.path", ""))
                .setAppPackage(config.getProperty("android.app.package", "com.qizhilian.app"))
                .setAppActivity(config.getProperty("android.app.activity", ".MainActivity"))
                .setAutomationName("UiAutomator2")
                .setNoReset(false)
                .setFullReset(false);
        
        return new AndroidDriver(url, options);
    }
    
    /**
     * 创建iOS Driver
     */
    private IOSDriver createIOSDriver(URL url) {
        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName("iOS")
                .setPlatformVersion(config.getProperty("ios.platform.version", "17.0"))
                .setDeviceName(config.getProperty("ios.device.name", "iPhone 14"))
                .setApp(config.getProperty("ios.app.path", ""))
                .setBundleId(config.getProperty("ios.bundle.id", "com.qizhilian.app"))
                .setAutomationName("XCUITest")
                .setNoReset(false);
        
        return new IOSDriver(url, options);
    }
    
    /**
     * 点击元素
     */
    @Step("点击元素: {element}")
    protected void click(WebElement element) {
        element.click();
        log.info("点击元素");
    }
    
    /**
     * 输入文本
     */
    @Step("输入文本: {text}")
    protected void sendKeys(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
        log.info("输入文本: {}", text);
    }
    
    /**
     * 获取元素文本
     */
    @Step("获取元素文本")
    protected String getText(WebElement element) {
        return element.getText();
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
    
    /**
     * 隐藏键盘（iOS）
     */
    @Step("隐藏键盘")
    protected void hideKeyboard() {
        if (driver instanceof IOSDriver) {
            ((IOSDriver) driver).hideKeyboard();
        }
    }
    
    /**
     * 按返回键（Android）
     */
    @Step("按返回键")
    protected void pressBack() {
        if (driver instanceof AndroidDriver) {
            ((AndroidDriver) driver).pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
        }
    }
    
    /**
     * 截图
     */
    @Step("截图")
    protected void takeScreenshot(String name) {
        // 截图逻辑，可集成Allure
        log.info("截图: {}", name);
    }
}
