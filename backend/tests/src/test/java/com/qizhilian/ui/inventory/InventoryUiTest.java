package com.qizhilian.ui.inventory;

import com.qizhilian.ui.base.UiBaseTest;
import com.qizhilian.ui.pages.DashboardPage;
import com.qizhilian.ui.pages.LoginPage;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存管理UI测试
 * 覆盖入库、出库、盘点等核心流程
 */
@Epic("企智连UI测试")
@Feature("库存管理")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryUiTest extends UiBaseTest {

    private LoginPage loginPage;
    private DashboardPage dashboardPage;

    @BeforeEach
    public void navigateToLogin() {
        openPath("/login");
        loginPage = new LoginPage(driver);
    }

    @Test
    @Order(1)
    @Story("库存查询")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("库存列表页面加载")
    void testInventoryListPageLoad() {
        // 登录
        dashboardPage = loginPage.login("admin", "admin123");
        assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard应该加载成功");

        // 导航到库存管理
        dashboardPage.clickMenu("库存管理");
        dashboardPage.clickMenu("库存查询");

        // 验证页面加载
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("inventory-list")));
        WebElement inventoryList = driver.findElement(By.className("inventory-list"));
        assertTrue(inventoryList.isDisplayed(), "库存列表应该显示");
    }

    @Test
    @Order(2)
    @Story("入库管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建入库单流程")
    void testCreateInboundOrder() {
        // 登录
        dashboardPage = loginPage.login("admin", "admin123");
        assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard应该加载成功");

        // 导航到入库管理
        dashboardPage.clickMenu("库存管理");
        dashboardPage.clickMenu("入库管理");

        // 点击新增入库单
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-add"))).click();

        // 填写入库单信息
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("warehouseId")));
        driver.findElement(By.name("warehouseId")).sendKeys("1");
        driver.findElement(By.name("supplierId")).sendKeys("1");
        driver.findElement(By.name("inboundType")).sendKeys("PURCHASE");
        driver.findElement(By.name("remark")).sendKeys("UI测试入库单");

        // 添加入库明细
        driver.findElement(By.cssSelector(".btn-add-item")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("materialId")));
        driver.findElement(By.name("materialId")).sendKeys("1");
        driver.findElement(By.name("quantity")).sendKeys("100");
        driver.findElement(By.name("batchNo")).sendKeys("BATCH_UI_001");

        // 保存入库单
        driver.findElement(By.cssSelector(".btn-save")).click();

        // 验证保存成功
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("success-message")));
        WebElement successMessage = driver.findElement(By.className("success-message"));
        assertTrue(successMessage.isDisplayed(), "应该显示保存成功消息");
    }

    @Test
    @Order(3)
    @Story("出库管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建出库单流程")
    void testCreateOutboundOrder() {
        // 登录
        dashboardPage = loginPage.login("admin", "admin123");
        assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard应该加载成功");

        // 导航到出库管理
        dashboardPage.clickMenu("库存管理");
        dashboardPage.clickMenu("出库管理");

        // 点击新增出库单
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-add"))).click();

        // 填写出库单信息
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("warehouseId")));
        driver.findElement(By.name("warehouseId")).sendKeys("1");
        driver.findElement(By.name("customerId")).sendKeys("1");
        driver.findElement(By.name("outboundType")).sendKeys("SALE");
        driver.findElement(By.name("remark")).sendKeys("UI测试出库单");

        // 添加出库明细
        driver.findElement(By.cssSelector(".btn-add-item")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("materialId")));
        driver.findElement(By.name("materialId")).sendKeys("1");
        driver.findElement(By.name("quantity")).sendKeys("10");

        // 保存出库单
        driver.findElement(By.cssSelector(".btn-save")).click();

        // 验证保存成功
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("success-message")));
        WebElement successMessage = driver.findElement(By.className("success-message"));
        assertTrue(successMessage.isDisplayed(), "应该显示保存成功消息");
    }

    @Test
    @Order(4)
    @Story("库存盘点")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建盘点单流程")
    void testCreateStockCheck() {
        // 登录
        dashboardPage = loginPage.login("admin", "admin123");
        assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard应该加载成功");

        // 导航到库存盘点
        dashboardPage.clickMenu("库存管理");
        dashboardPage.clickMenu("库存盘点");

        // 点击新增盘点单
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-add"))).click();

        // 填写盘点单信息
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("warehouseId")));
        driver.findElement(By.name("warehouseId")).sendKeys("1");
        driver.findElement(By.name("checkType")).sendKeys("FULL");
        driver.findElement(By.name("planDate")).sendKeys("2024-12-31");
        driver.findElement(By.name("remark")).sendKeys("UI测试盘点单");

        // 保存盘点单
        driver.findElement(By.cssSelector(".btn-save")).click();

        // 验证保存成功
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("success-message")));
        WebElement successMessage = driver.findElement(By.className("success-message"));
        assertTrue(successMessage.isDisplayed(), "应该显示保存成功消息");
    }

    @Test
    @Order(5)
    @Story("库存查询")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("库存筛选和搜索")
    void testInventorySearchAndFilter() {
        // 登录
        dashboardPage = loginPage.login("admin", "admin123");
        assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard应该加载成功");

        // 导航到库存查询
        dashboardPage.clickMenu("库存管理");
        dashboardPage.clickMenu("库存查询");

        // 等待页面加载
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("inventory-list")));

        // 按仓库筛选
        driver.findElement(By.name("warehouseFilter")).sendKeys("1");
        driver.findElement(By.cssSelector(".btn-search")).click();

        // 等待筛选结果
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("filtered-results")));

        // 验证筛选结果
        WebElement results = driver.findElement(By.className("filtered-results"));
        assertTrue(results.isDisplayed(), "筛选结果应该显示");

        // 按物料搜索
        driver.findElement(By.name("materialSearch")).sendKeys("测试物料");
        driver.findElement(By.cssSelector(".btn-search")).click();

        // 等待搜索结果
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("search-results")));
    }

    @Test
    @Order(6)
    @Story("库存预警")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("库存预警页面")
    void testInventoryAlertPage() {
        // 登录
        dashboardPage = loginPage.login("admin", "admin123");
        assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard应该加载成功");

        // 导航到库存预警
        dashboardPage.clickMenu("库存管理");
        dashboardPage.clickMenu("库存预警");

        // 验证页面加载
        wait.un
        // 验证页面加载
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("alert-list")));
        WebElement alertList = driver.findElement(By.className("alert-list"));
        assertTrue(alertList.isDisplayed(), "预警列表应该显示");
    }
}
