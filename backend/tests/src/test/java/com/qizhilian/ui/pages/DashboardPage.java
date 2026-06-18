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
import java.util.List;

/**
 * 仪表板页面
 * 封装Dashboard相关的页面元素和操作
 */
@Slf4j
public class DashboardPage {
    
    private WebDriver driver;
    private WebDriverWait wait;
    
    // 页面元素
    @FindBy(className = "dashboard-container")
    private WebElement dashboardContainer;
    
    @FindBy(className = "user-avatar")
    private WebElement userAvatar;
    
    @FindBy(className = "logout-button")
    private WebElement logoutButton;
    
    @FindBy(css = ".sidebar-menu")
    private WebElement sidebarMenu;
    
    @FindBy(css = ".menu-item")
    private List<WebElement> menuItems;
    
    @FindBy(className = "page-title")
    private WebElement pageTitle;
    
    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }
    
    /**
     * 检查Dashboard是否加载成功
     */
    @Step("检查Dashboard是否加载")
    public boolean isDashboardLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(dashboardContainer)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取页面标题
     */
    @Step("获取页面标题")
    public String getPageTitle() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(pageTitle)).getText();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 点击菜单项
     */
    @Step("点击菜单: {menuName}")
    public DashboardPage clickMenu(String menuName) {
        WebElement menuItem = driver.findElement(
                By.xpath("//span[contains(text(),'" + menuName + "')]/parent::*"));
        wait.until(ExpectedConditions.elementToBeClickable(menuItem));
        menuItem.click();
        log.info("点击菜单: {}", menuName);
        return this;
    }
    
    /**
     * 展开子菜单
     */
    @Step("展开子菜单: {menuName}")
    public DashboardPage expandSubMenu(String menuName) {
        WebElement menuItem = driver.findElement(
                By.xpath("//span[contains(text(),'" + menuName + "')]/ancestor::div[contains(@class,'menu-item')]"));
        wait.until(ExpectedConditions.elementToBeClickable(menuItem));
        menuItem.click();
        log.info("展开子菜单: {}", menuName);
        return this;
    }
    
    /**
     * 点击用户头像
     */
    @Step("点击用户头像")
    public DashboardPage clickUserAvatar() {
        wait.until(ExpectedConditions.elementToBeClickable(userAvatar));
        userAvatar.click();
        return this;
    }
    
    /**
     * 点击退出登录
     */
    @Step("点击退出登录")
    public LoginPage logout() {
        clickUserAvatar();
        wait.until(ExpectedConditions.elementToBeClickable(logoutButton));
        logoutButton.click();
        log.info("退出登录");
        return new LoginPage(driver);
    }
    
    /**
     * 获取当前用户名
     */
    @Step("获取当前用户名")
    public String getCurrentUsername() {
        try {
            WebElement usernameElement = driver.findElement(By.className("username"));
            return usernameElement.getText();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 检查侧边栏是否显示
     */
    @Step("检查侧边栏是否显示")
    public boolean isSidebarDisplayed() {
        try {
            return sidebarMenu.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取菜单数量
     */
    @Step("获取菜单数量")
    public int getMenuCount() {
        return menuItems.size();
    }
    
    /**
     * 等待页面加载完成
     */
    @Step("等待页面加载")
    public DashboardPage waitForPageLoad() {
        wait.until(ExpectedConditions.visibilityOf(dashboardContainer));
        return this;
    }
}
