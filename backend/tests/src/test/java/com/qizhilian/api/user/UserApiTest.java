package com.qizhilian.api.user;

import com.qizhilian.api.base.ApiBaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户管理API测试类
 * 覆盖用户CRUD、登录认证等核心接口
 */
@Feature("用户管理")
@Epic("企智连API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserApiTest extends ApiBaseTest {
    
    private static Long createdUserId;
    private static final String TEST_USERNAME_PREFIX = "test_user_";
    
    @BeforeAll
    public static void setup() {
        // 全局初始化
    }
    
    @Test
    @Order(1)
    @Story("用户认证")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("用户登录成功")
    void testLoginSuccess() {
        Map<String, Object> loginBody = new HashMap<>();
        loginBody.put("username", "admin");
        loginBody.put("password", "admin123");
        
        Response response = post("/auth/login", loginBody);
        
        assertSuccess(response);
        assertFieldExists(response, "data.token");
        assertFieldExists(response, "data.userId");
        
        String token = response.jsonPath().getString("data.token");
        setAuthToken(token);
    }
    
    @Test
    @Order(2)
    @Story("用户认证")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("用户登录失败-密码错误")
    void testLoginFailure() {
        Map<String, Object> loginBody = new HashMap<>();
        loginBody.put("username", "admin");
        loginBody.put("password", "wrong_password");
        
        Response response = post("/auth/login", loginBody);
        
        assertStatusCode(response, 401);
    }
    
    @Test
    @Order(3)
    @Story("用户管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建用户成功")
    void testCreateUser() {
        String username = TEST_USERNAME_PREFIX + System.currentTimeMillis();
        
        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", username);
        userBody.put("password", "Test@123");
        userBody.put("email", username + "@test.com");
        userBody.put("phone", "13800138000");
        userBody.put("realName", "测试用户");
        userBody.put("roleIds", new Integer[]{1, 2});
        userBody.put("status", "ACTIVE");
        
        Response response = post("/users", userBody);
        
        assertSuccess(response);
        createdUserId = response.jsonPath().getLong("data.id");
        assertNotNull(createdUserId, "用户ID不应为空");
    }
    
    @Test
    @Order(4)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询用户列表")
    void testGetUserList() {
        Response response = get("/users?page=1&size=20");
        
        assertSuccess(response);
        assertFieldExists(response, "data.list");
        assertFieldExists(response, "data.total");
    }
    
    @Test
    @Order(5)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询指定用户")
    void testGetUserById() {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        Response response = get("/users/" + createdUserId);
        
        assertSuccess(response);
        assertFieldEquals(response, "data.id", createdUserId.intValue());
    }
    
    @Test
    @Order(6)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("更新用户信息")
    void testUpdateUser() {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("realName", "更新后的测试用户");
        updateBody.put("email", "updated_" + System.currentTimeMillis() + "@test.com");
        updateBody.put("phone", "13900139000");
        
        Response response = put("/users/" + createdUserId, updateBody);
        
        assertSuccess(response);
    }
    
    @Test
    @Order(7)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("禁用用户")
    void testDisableUser() {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        Map<String, Object> statusBody = new HashMap<>();
        statusBody.put("status", "DISABLED");
        
        Response response = patch("/users/" + createdUserId + "/status", statusBody);
        
        assertSuccess(response);
    }
    
    @Test
    @Order(8)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("启用用户")
    void testEnableUser() {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        Map<String, Object> statusBody = new HashMap<>();
        statusBody.put("status", "ACTIVE");
        
        Response response = patch("/users/" + createdUserId + "/status", statusBody);
        
        assertSuccess(response);
    }
    
    @Test
    @Order(9)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("重置用户密码")
    void testResetPassword() {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        Map<String, Object> resetBody = new HashMap<>();
        resetBody.put("newPassword", "NewPass@456");
        
        Response response = post("/users/" + createdUserId + "/reset-password", resetBody);
        
        assertSuccess(response);
    }
    
    @Test
    @Order(10)
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("删除用户")
    void testDeleteUser() {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        Response response = delete("/users/" + createdUserId);
        
        assertSuccess(response);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "user@name", "user name"})
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("创建用户-用户名格式校验")
    void testCreateUserInvalidUsername(String username) {
        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", username);
        userBody.put("password", "Test@123");
        userBody.put("email", "test@test.com");
        
        Response response = post("/users", userBody);
        
        assertStatusCode(response, 400);
    }
    
    @ParameterizedTest
    @CsvSource({
        "test@test.com, true",
        "invalid_email, false",
        "@test.com, false",
        "test@, false"
    })
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("创建用户-邮箱格式校验")
    void testCreateUserEmailValidation(String email, boolean shouldSucceed) {
        String username = TEST_USERNAME_PREFIX + System.currentTimeMillis();
        
        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", username);
        userBody.put("password", "Test@123");
        userBody.put("email", email);
        
        Response response = post("/users", userBody);
        
        if (shouldSucceed) {
            assertSuccess(response);
        } else {
            assertStatusCode(response, 400);
        }
    }
    
    @Test
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询用户-按角色筛选")
    void testGetUsersByRole() {
        Response response = get("/users?roleId=1");
        
        assertSuccess(response);
        assertFieldExists(response, "data.list");
    }
    
    @Test
    @Story("用户管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询用户-按状态筛选")
    void testGetUsersByStatus() {
