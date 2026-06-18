package com.qizhilian.ui.pages;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * 登录页面
 * 封装登录相关的页面元素和操作
 */
@Slf4j
public class LoginPage {
    
    private WebDriver driver;
    private WebDriverWait wait;
    
    // 页面元素
    @FindBy(name = "username")
    private WebElement usernameInput;
    
    @FindBy(name = "password")
    private WebElement passwordInput;
    
    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;
    
    @FindBy(className = "error-message")
    private WebElement errorMessage;
    
    @FindBy(className = "login-form")
    private WebElement loginForm;
    
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }
    
    /**
     * 输入用户名
     */
    @Step("输入用户名: {username}")
    public LoginPage enterUsername(String username) {
        wait.until(ExpectedConditions.visibilityOf(usernameInput));
        usernameInput.clear();
        usernameInput.sendKeys(username);
        log.info("输入用户名: {}", username);
        return this;
    }
    
    /**
     * 输入密码
     */
    @Step("输入密码")
    public LoginPage enterPassword(String password) {
        wait.until(ExpectedConditions.visibilityOf(passwordInput));
        passwordInput.clear();
        passwordInput.sendKeys(password);
        log.info("输入密码");
        return this;
    }
    
    /**
     * 点击登录按钮
     */
    @Step("点击登录按钮")
    public LoginPage clickLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(loginButton));
        loginButton.click();
        log.info("点击登录按钮");
        return this;
    }
    
    /**
     * 执行登录
     */
    @Step("执行登录: {username}")
    public DashboardPage login(String username, String password) {
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        
        // 等待登录成功，跳转到Dashboard
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        return new DashboardPage(driver);
    }
    
    /**
     * 执行登录（预期失败）
     */
    @Step("执行登录(预期失败): {username}")
    public LoginPage loginExpectFailure(String username, String password) {
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        return this;
    }
    
    /**
     * 获取错误信息
     */
    @Step("获取错误信息")
    public String getErrorMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(errorMessage));
            return errorMessage.getText();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 检查是否在登录页面
     */
    @Step("检查登录页面是否显示")
    public boolean isLoginPageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(loginForm)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查错误信息是否显示
     */
    @Step("检查错误信息是否显示")
    public boolean isErrorMessageDisplayed() {
        try {
            return errorMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 清空表单
     */
    @Step("清空登录表单")
    public LoginPage clearForm() {
        usernameInput.clear();
        passwordInput.clear();
        return this;
    }
}
