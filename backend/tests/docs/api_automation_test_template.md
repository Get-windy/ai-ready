# API自动化测试用例模板

## 测试用例结构

```java
package com.aiedge.sprint27+1.user;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 用户管理模块API测试
 * 测试类命名规范：{模块名}ApiTest
 */
@DisplayName("用户管理模块API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserManagementApiTest extends BaseTest {
    
    private static String accessToken;
    private static Integer testUserId;
    
    /**
     * 测试前置条件：用户登录获取Token
     */
    @BeforeAll
    public static void setup() {
        // 从配置读取测试账号
        String username = config.getTestUsername();
        String password = config.getTestPassword();
        
        // 登录获取Token
        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + password + "\"}")
            .when()
            .post("/api/v1/auth/login");
        
        accessToken = loginResponse.jsonPath().getString("accessToken");
        
        // 验证登录成功
        loginResponse.then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("tokenType", equalTo("Bearer"));
    }
    
    /**
     * TC001: 创建用户测试
     * 测试正常创建用户流程
     */
    @Test
    @Order(1)
    @DisplayName("TC001: 创建用户 - 正常流程")
    public void testCreateUser_Success() {
        // 准备测试数据
        String username = testDataFactory.generateUsername();
        String email = testDataFactory.generateEmail();
        String password = testDataFactory.generatePassword();
        
        // 发送创建用户请求
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body("{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
            .when()
            .post("/api/v1/users");
        
        // 验证响应
        response.then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("username", equalTo(username))
            .body("email", equalTo(email))
            .body("status", equalTo("ACTIVE"));
        
        // 保存测试用户ID供后续测试使用
        testUserId = response.jsonPath().getInt("id");
    }
    
    /**
     * TC002: 查询用户列表测试
     * 测试分页查询功能
     */
    @Test
    @Order(2)
    @DisplayName("TC002: 查询用户列表 - 分页查询")
    public void testGetUserList_Pagination() {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("sortBy", "createdAt")
            .queryParam("sortDir", "desc")
            .when()
            .get("/api/v1/users");
        
        response.then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("pageable.pageNumber", equalTo(0))
            .body("pageable.pageSize", equalTo(10))
            .body("totalElements", greaterThan(0));
    }
    
    /**
     * TC003: 查询用户详情测试
     * 测试根据ID查询用户
     */
    @Test
    @Order(3)
    @DisplayName("TC003: 查询用户详情 - 正常流程")
    public void testGetUserById_Success() {
        Assumptions.assumeTrue(testUserId != null, "测试用户ID不能为空");
        
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .pathParam("id", testUserId)
            .when()
            .get("/api/v1/users/{id}");
        
        response.then()
            .statusCode(200)
            .body("id", equalTo(testUserId))
            .body("username", notNullValue())
            .body("email", notNullValue());
    }
    
    /**
     * TC004: 更新用户信息测试
     */
    @Test
    @Order(4)
    @DisplayName("TC004: 更新用户信息 - 正常流程")
    public void testUpdateUser_Success() {
        Assumptions.assumeTrue(testUserId != null, "测试用户ID不能为空");
        
        String newFullName = testDataFactory.generateFullName();
        String newPhone = testDataFactory.generatePhoneNumber();
        
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .pathParam("id", testUserId)
            .body("{\"fullName\":\"" + newFullName + "\",\"phone\":\"" + newPhone + "\"}")
            .when()
            .put("/api/v1/users/{id}");
        
        response.then()
            .statusCode(200)
            .body("fullName", equalTo(newFullName))
            .body("phone", equalTo(newPhone));
    }
    
    /**
     * TC005: 错误参数测试 - 重复用户名
     * 测试创建用户时用户名重复的情况
     */
    @Test
    @Order(5)
    @DisplayName("TC005: 创建用户 - 用户名重复")
    public void testCreateUser_DuplicateUsername() {
        // 使用已存在的用户名
        String existingUsername = config.getTestUsername();
        
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body("{\"username\":\"" + existingUsername + "\",\"email\":\"duplicate@test.com\",\"password\":\"Test123456\"}")
            .when()
            .post("/api/v1/users");
        
        response.then()
            .statusCode(400)  // 假设返回400错误
            .body("message", containsString("用户名已存在"));
    }
    
    /**
     * TC006: 参数化测试 - 无效邮箱格式
     * 测试多种无效邮箱格式
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",
        "missing@domain",
        "@domain.com",
        "space in@email.com",
        "中国@邮箱.com"
    })
    @Order(6)
    @DisplayName("TC006: 创建用户 - 无效邮箱格式")
    public void testCreateUser_InvalidEmailFormat(String invalidEmail) {
        Response response = given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body("{\"username\":\"testuser_" + System.currentTimeMillis() + "\",\"email\":\"" + invalidEmail + "\",\"password\":\"Test123456\"}")
            .when()
            .post("/api/v1/users");
        
        response.then()
            .statusCode(400)
            .body("message", containsString("邮箱"));
    }
    
    /**
     * TC007: 无权限访问测试
     * 测试没有Token访问受保护接口
     */
    @Test
    @Order(7)
    @DisplayName("TC007: 无Token访问用户列表")
    public void testGetUserList_WithoutToken() {
        Response response = given()
            .when()
            .get("/api/v1/users");
        
        response.then()
            .statusCode(401)  // 未授权
            .body("message", containsString("未授权"));
    }
    
    /**
     * TC008: 无效Token访问测试
     */
    @Test
    @Order(8)
    @DisplayName("TC008: 无效Token访问用户列表")
    public void testGetUserList_InvalidToken() {
        Response response = given()
            .header("Authorization", "Bearer invalid_token_12345")
            .when()
            .get("/api/v1/users");
        
        response.then()
            .statusCode(401)
            .body("message", containsString("Token"));
    }
    
    /**
     * 测试后清理：删除测试用户
     */
    @AfterAll
    public static void cleanup() {
        if (testUserId != null) {
            // 这里假设有删除用户的接口
            // 实际项目中可能需要调用删除接口
            System.out.println("测试完成，测试用户ID: " + testUserId + " 需要清理");
        }
    }
}
```

## 测试基类模板

```java
package com.aiedge.sprint27+1.base;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.BeforeAll;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 测试基类
 * 提供通用的配置和工具方法
 */
public class BaseTest {
    
    protected static TestConfig config;
    protected static TestDataFactory testDataFactory;
    
    @BeforeAll
    public static void setupBase() {
        // 加载配置文件
        config = new TestConfig();
        
        // 配置RestAssured
        RestAssured.baseURI = config.getBaseUrl();
        RestAssured.basePath = config.getApiVersion();
        
        // 启用日志（仅在调试时启用）
        if (config.isDebugEnabled()) {
            RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        
        // 初始化测试数据工厂
        testDataFactory = new TestDataFactory();
    }
}
```

## 配置类模板

```java
package com.aiedge.sprint27+1.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 测试配置管理
 */
public class TestConfig {
    
    private Properties properties;
    
    public TestConfig() {
        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream("src/test/resources/application.properties");
            properties.load(fis);
        } catch (IOException e) {
            // 使用默认配置
            setDefaultProperties();
        }
    }
    
    private void setDefaultProperties() {
        properties.setProperty("base.url", "http://localhost:8083");
        properties.setProperty("api.version", "");
        properties.setProperty("test.username", "testuser");
        properties.setProperty("test.password", "Test123456");
        properties.setProperty("debug.enabled", "false");
    }
    
    public String getBaseUrl() {
        return properties.getProperty("base.url", "http://localhost:8080");
    }
    
    public String getApiVersion() {
        return properties.getProperty("api.version", "/api/v1");
    }
    
    public String getTestUsername() {
        return properties.getProperty("test.username", "admin");
    }
    
    public String getTestPassword() {
        return properties.getProperty("test.password", "admin123");
    }
    
    public boolean isDebugEnabled() {
        return Boolean.parseBoolean(properties.getProperty("debug.enabled", "false"));
    }
}
```

## 测试数据工厂模板

```java
package com.aiedge.sprint27+1.base;

import com.github.javafaker.Faker;
import java.util.Locale;

/**
 * 测试数据工厂
 * 生成随机的测试数据
 */
public class TestDataFactory {
    
    private Faker faker;
    
    public TestDataFactory() {
        faker = new Faker(new Locale("zh-CN"));
    }
    
    public String generateUsername() {
        return "test_" + faker.name().username() + "_" + System.currentTimeMillis();
    }
    
    public String generateEmail() {
        return "test_" + System.currentTimeMillis() + "@example.com";
    }
    
    public String generatePassword() {
        return "Test@" + faker.number().digits(8);
    }
    
    public String generateFullName() {
        return faker.name().fullName();
    }
    
    public String generatePhoneNumber() {
        return "1" + faker.number().digits(10);
    }
    
    public String generateProductName() {
        return faker.commerce().productName();
    }
    
    public Double generatePrice() {
        return faker.number().randomDouble(2, 10, 1000);
    }
}
```

## 配置文件模板 (application.properties)

```properties
# API基础配置
base.url=http://localhost:8083
api.version=/api/v1

# 超时配置
connection.timeout=10000
socket.timeout=30000

# 测试账号
test.username=admin
test.password=admin123

# 调试配置
debug.enabled=false
log.requests=false
log.responses=false

# 数据库配置（可选）
test.db.url=jdbc:postgresql://localhost:5432/test_db
test.db.username=test_user
test.db.password=test_password

# 测试执行配置
test.retry.count=3
test.retry.delay=1000
test.parallel.threads=4
```

## 使用说明

1. **复制测试类模板**，根据实际API接口修改
2. **配置测试环境**，编辑application.properties文件
3. **执行测试**：`mvn clean test`
4. **查看报告**：`mvn allure:serve`

## 最佳实践

1. **测试独立性**：每个测试用例应该独立，不依赖其他测试的结果
2. **数据清理**：测试结束后清理测试数据
3. **参数化测试**：使用@ParameterizedTest覆盖多种测试场景
4. **断言明确**：使用明确的断言，便于问题定位
5. **日志记录**：适当记录测试日志，便于调试
6. **错误处理**：正确处理测试失败的情况