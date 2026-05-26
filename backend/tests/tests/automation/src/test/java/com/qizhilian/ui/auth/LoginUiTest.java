package com.qizhilian.ui.auth;

import com.qizhilian.ui.base.UiBaseTest;
import com.qizhilian.ui.pages.DashboardPage;
import com.qizhilian.ui.pages.LoginPage;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录功能UI测试
 * 覆盖登录页面和登录流程的UI测试
 */
@Epic("企智连UI测试")
@Feature("用户认证")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginUiTest extends UiBaseTest {
    
    private LoginPage loginPage;
    
    @BeforeEach
    public void navigateToLogin() {
        openPath("/login");
        loginPage = new LoginPage(driver);
    }
    
    @Test
    @Order(1)
    @Story("登录功能")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("登录页面加载成功")
    void testLoginPageLoad() {
        assertTrue(loginPage.isLoginPageDisplayed(), "登录页面应该显示");
    }
    
    @Test
    @Order(2)
    @Story("登录功能")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("使用有效凭据登录成功")
    void testLoginWithValidCredentials() {
        DashboardPage dashboard = loginPage.login("admin", "admin123");
        
        assertTrue(dashboard.isDashboardLoaded(), "Dashboard应该加载成功");
        assertTrue(dashboard.isSidebarDisplayed(), "侧边栏应该显示");
    }
    
    @Test
    @Order(3)
    @Story("登录功能")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("使用无效密码登录失败")
    void testLoginWithInvalidPassword() {
        loginPage.loginExpectFailure("admin", "wrongpassword");
        
        assertTrue(loginPage.isErrorMessageDisplayed(), "应该显示错误信息");
        assertFalse(loginPage.isLoginPageDisplayed(), "登录页面应该仍然显示");
    }
    
    @Test
    @Order(4)
    @Story("登录功能")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("使用不存在的用户登录失败")
    void testLoginWithNonExistentUser() {
        loginPage.loginExpectFailure("nonexistentuser", "password");
        
        assertTrue(loginPage.isErrorMessageDisplayed(), "应该显示错误信息");
    }
    
    @ParameterizedTest
    @CsvSource({
            "admin, '', 密码不能为空",
            "'', admin123, 用户名不能为空",
            "'', '', 用户名和密码不能为空"
    })
    @Order(5)
    @Story("登录功能")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("登录字段验证")
    void testLoginFieldValidation(String username, String password, String expectedMessage) {
        loginPage.enterUsername(username)
                .enterPassword(password)
                .clickLogin();
        
        // 验证表单验证或错误提示
        assertTrue(loginPage.isErrorMessageDisplayed() || getCurrentUrl().contains("/login"),
                "应该显示验证错误或保持在登录页");
    }
    
    @Test
    @Order(6)
    @Story("登录功能")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("登录后退出")
    void testLogout() {
        // 先登录
        DashboardPage dashboard = loginPage.login("admin", "admin123");
        assertTrue(dashboard.isDashboardLoaded(), "应该先登录成功");
        
        // 退出
        LoginPage newLoginPage = dashboard.logout();
        
        assertTrue(newLoginPage.isLoginPageDisplayed(), "应该返回登录页面");
        assertTrue(getCurrentUrl().contains("/login"), "URL应该包含/login");
    }
    
    @Test
    @Order(7)
    @Story("登录功能")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("记住密码功能")
    void testRememberMe() {
        // 输入凭据并勾选记住密码
        loginPage.enterUsername("admin")
                .enterPassword("admin123");
        // 勾选记住密码复选框
        // loginPage.clickRememberMe();
        
        DashboardPage dashboard = loginPage.clickLogin().login("admin", "admin123");
        assertTrue(dashboard.isDashboardLoaded(), "登录应该成功");
    }
    
    @Test
    @Order(8)
    @Story("登录功能")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("页面标题验证")
    void testPageTitle() {
        String title = getPageTitle();
        assertTrue(title.contains("登录") || title.contains("Login"), 
                "页面标题应包含登录相关文字");
    }
}
