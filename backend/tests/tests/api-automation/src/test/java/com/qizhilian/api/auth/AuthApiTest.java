package com.qizhilian.api.auth;

import com.qizhilian.api.base.BaseApiTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 用户认证接口测试
 * 包含登录、注册、权限管理等接口的测试用例
 */
@Feature("用户认证模块")
@Story("用户认证API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthApiTest extends BaseApiTest {
    
    private static String testUsername;
    private static String testPassword;
    private static String testEmail;
    private static String testPhone;
    private static Long testUserId;
    
    @BeforeAll
    static void initTestData() {
        testUsername = dataGenerator.generateUsername();
        testPassword = "Test@123456";
        testEmail = dataGenerator.generateEmail();
        testPhone = dataGenerator.generatePhone();
    }
    
    // ==================== 用户注册接口测试 ====================
    
    @Test
    @Order(1)
    @DisplayName("用户注册 - 正向测试")
    @Description("使用有效数据注册新用户")
    @Severity(SeverityLevel.CRITICAL)
    void testRegisterSuccess() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", testUsername);
        requestBody.put("password", testPassword);
        requestBody.put("email", testEmail);
        requestBody.put("phone", testPhone);
        requestBody.put("realName", dataGenerator.generateName());
        requestBody.put("companyName", dataGenerator.generateCompanyName());
        
        Response response = post("/auth/register", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"))
            .body("data.userId", notNullValue())
            .body("data.username", equalTo(testUsername));
        
        testUserId = response.jsonPath().getLong("data.userId");
    }
    
    @Test
    @Order(2)
    @DisplayName("用户注册 - 重复用户名")
    @Description("使用已存在的用户名注册，应返回错误")
    @Severity(SeverityLevel.NORMAL)
    void testRegisterDuplicateUsername() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", testUsername);
        requestBody.put("password", testPassword);
        requestBody.put("email", dataGenerator.generateEmail());
        requestBody.put("phone", dataGenerator.generatePhone());
        
        Response response = post("/auth/register", requestBody);
        
        assertStatusCode(response, 409);
        response.then()
            .body("code", equalTo(409))
            .body("message", containsString("已存在"));
    }
    
    @ParameterizedTest
    @Order(3)
    @DisplayName("用户注册 - 参数校验")
    @Description("测试各种无效参数")
    @Severity(SeverityLevel.NORMAL)
    @CsvSource({
        "'', Test@123, test@test.com, 用户名不能为空",
        "user, '', test@test.com, 密码不能为空",
        "user, short, test@test.com, 密码长度不能少于8位",
        "user, Test@123, invalid-email, 邮箱格式不正确"
    })
    void testRegisterValidation(String username, String password, String email, String expectedMsg) {
        Map<String, Object> requestBody = new HashMap<>();
        if (!username.isEmpty()) requestBody.put("username", username);
        if (!password.isEmpty()) requestBody.put("password", password);
        if (!email.isEmpty()) requestBody.put("email", email);
        requestBody.put("phone", "13800138000");
        
        Response response = post("/auth/register", requestBody);
        
        assertStatusCode(response, 400);
        response.then()
            .body("code", equalTo(400));
    }
    
    // ==================== 用户登录接口测试 ====================
    
    @Test
    @Order(10)
    @DisplayName("用户登录 - 正向测试（用户名+密码）")
    @Description("使用正确的用户名和密码登录")
    @Severity(SeverityLevel.CRITICAL)
    void testLoginWithUsername() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", testUsername);
        requestBody.put("password", testPassword);
        
        Response response = post("/auth/login", requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"))
            .body("data.token", notNullValue())
            .body("data.token", not(emptyString()))
            .body("data.userId", equalTo(testUserId.intValue()))
            .body("data.username", equalTo(testUsername))
            .body("data.expiresIn", greaterThan(0));
        
        authToken = response.jsonPath().getString("data.token");
        setAuthToken(authToken);
    }
    
    @Test
    @Order(11)
    @DisplayName("用户登录 - 邮箱登录")
    @Description("使用邮箱和密码登录")
    @Severity(SeverityLevel.NORMAL)
    void testLoginWithEmail() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("email", testEmail);
        requestBody.put("password", testPassword);
        
        Response response = post("/auth/login", requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.token", notNullValue());
    }
    
    @Test
    @Order(12)
    @DisplayName("用户登录 - 手机号登录")
    @Description("使用手机号和密码登录")
    @Severity(SeverityLevel.NORMAL)
    void testLoginWithPhone() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("phone", testPhone);
        requestBody.put("password", testPassword);
        
        Response response = post("/auth/login", requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.token", notNullValue());
    }
    
    @Test
    @Order(13)
    @DisplayName("用户登录 - 错误密码")
    @Description("使用错误的密码登录")
    @Severity(SeverityLevel.NORMAL)
    void testLoginWrongPassword() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", testUsername);
        requestBody.put("password", "WrongPassword123");
        
        Response response = post("/auth/login", requestBody);
        
        assertStatusCode(response, 401);
        response.then()
            .body("code", equalTo(401))
            .body("message", containsString("密码错误"));
    }
    
    @Test
    @Order(14)
    @DisplayName("用户登录 - 不存在的用户")
    @Description("使用不存在的用户名登录")
    @Severity(SeverityLevel.NORMAL)
    void testLoginNonExistentUser() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "nonexistentuser" + System.currentTimeMillis());
        requestBody.put("password", testPassword);
        
        Response response = post("/auth/login", requestBody);
        
        assertStatusCode(response, 404);
        response.then()
            .body("code", equalTo(404))
            .body("message", containsString("不存在"));
    }
    
    // ==================== Token相关接口测试 ====================
    
    @Test
    @Order(20)
    @DisplayName("Token验证 - 有效Token")
    @Description("验证有效的Token")
    @Severity(SeverityLevel.CRITICAL)
    void testVerifyToken() {
        if (authToken == null) {
            testLoginWithUsername();
        }
        
        Response response = get("/auth/verify");
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.valid", equalTo(true))
            .body("data.userId", equalTo(testUserId.intValue()));
    }
    
    @Test
    @Order(21)
    @DisplayName("Token验证 - 无效Token")
    @Description("验证无效的Token")
    @Severity(SeverityLevel.NORMAL)
    void testVerifyInvalidToken() {
        clearAuthToken();
        setAuthToken("invalid_token_12345");
        
        Response response = get("/auth/verify");
        
        assertStatusCode(response, 401);
        response.then()
            .body("code", equalTo(401))
            .body("message", containsString("无效"));
        
        setAuthToken(authToken);
    }
    
    @Test
    @Order(22)
    @DisplayName("Token刷新")
    @Description("刷新访问Token")
    @Severity(SeverityLevel.NORMAL)
    void testRefreshToken() {
        if (authToken == null) {
            testLoginWithUsername();
        }
        
        Response response = post("/auth/refresh", null);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.token", notNullValue())
            .body("data.token", not(equalTo(authToken)));
        
        authToken = response.jsonPath().getString("data.token");
        setAuthToken(authToken);
    }
    
    // ==================== 用户信息接口测试 ====================
    
    @Test
    @Order(30)
    @DisplayName("获取当前用户信息")
    @Description("获取当前登录用户的详细信息")
    @Severity(SeverityLevel.NORMAL)
    void testGetCurrentUser() {
        if (authToken == null) {
            testLoginWithUsername();
        }
        
        Response response = get("/auth/user/info");
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.userId", equalTo(testUserId.intValue()))
            .body("data.username", equalTo(testUsername))
            .body("data.email", equalTo(testEmail))
            .body("data.phone", equalTo(testPhone));
    }
    
    @Test
    @Order(31)
    @DisplayName("更新用户信息")
    @Description("更新当前用户的个人信息")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateUserInfo() {
        if (authToken == null) {
            testLoginWithUsername();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("realName", dataGenerator.generateName());
        requestBody.put("companyName", dataGenerator.generateCompanyName());
        
        Response response = put("/auth/user/info", requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
    }
    
    @Test
    @Order(32)
    @DisplayName("修改密码")
    @Description("修改当前用户的密码")
    @Severity(SeverityLevel.CRITICAL)
    void testChangePassword() {
        if (authToken == null) {
            testLoginWithUsername();
        }
        
        String newPassword = "NewTest@789";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("oldPassword", testPassword);
        requestBody.put("newPassword", newPassword);
        
        Response response = put("/auth/user/password", requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
        
        testPassword = newPassword;
    }
    
    // ==================== 登出接口测试 ====================
    
    @Test
    @Order(40)
    @DisplayName("用户登出")
    @Description("用户登出并注销Token")
    @Severity(SeverityLevel.NORMAL)
    void testLogout() {
        if (authToken == null) {
            testLoginWithUsername();
        }
        
        Response response = post("/auth/logout", null);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
        
        Response verifyResponse = get("/auth/verify");
        assertStatusCode(verifyResponse, 401);
        
        clearAuthToken();
    }
}
