package com.qizhilian.api.base;

import com.qizhilian.config.TestConfig;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.PrintStream;
import java.io.StringWriter;

import static io.restassured.RestAssured.given;

/**
 * API测试基类
 * 提供REST Assured的基础配置和通用方法
 */
@Slf4j
public abstract class ApiBaseTest {
    
    protected static TestConfig config;
    protected RequestSpecification requestSpec;
    protected String authToken;
    
    @BeforeAll
    public static void globalSetup() {
        config = TestConfig.getInstance();
        RestAssured.baseURI = config.getBaseUrl();
        RestAssured.basePath = config.getApiBasePath();
        
        // 配置日志
        RestAssured.config = RestAssuredConfig.config()
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
        
        log.info("API测试环境初始化完成: {}", RestAssured.baseURI + RestAssured.basePath);
    }
    
    @BeforeEach
    public void setup() {
        requestSpec = buildRequestSpec();
    }
    
    /**
     * 构建请求规范
     */
    protected RequestSpecification buildRequestSpec() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter());
        
        // 添加认证头
        if (authToken != null && !authToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }
        
        return builder.build();
    }
    
    /**
     * 设置认证Token
     */
    @Step("设置认证Token: {token}")
    protected void setAuthToken(String token) {
        this.authToken = token;
        this.requestSpec = buildRequestSpec();
        log.info("认证Token已设置");
    }
    
    /**
     * 执行登录获取Token
     */
    @Step("用户登录: {username}")
    protected String login(String username, String password) {
        Response response = given()
                .spec(requestSpec)
                .body(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        String token = response.jsonPath().getString("data.token");
        setAuthToken(token);
        return token;
    }
    
    /**
     * 发送GET请求
     */
    @Step("GET请求: {endpoint}")
    protected Response get(String endpoint) {
        return given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }
    
    /**
     * 发送GET请求（带查询参数）
     */
    @Step("GET请求: {endpoint}, 参数: {params}")
    protected Response get(String endpoint, Object... params) {
        return given()
                .spec(requestSpec)
                .queryParams(params)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }
    
    /**
     * 发送POST请求
     */
    @Step("POST请求: {endpoint}")
    protected Response post(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }
    
    /**
     * 发送PUT请求
     */
    @Step("PUT请求: {endpoint}")
    protected Response put(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
    }
    
    /**
     * 发送DELETE请求
     */
    @Step("DELETE请求: {endpoint}")
    protected Response delete(String endpoint) {
        return given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }
    
    /**
     * 发送PATCH请求
     */
    @Step("PATCH请求: {endpoint}")
    protected Response patch(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .extract()
                .response();
    }
    
    /**
     * 验证响应状态码
     */
    @Step("验证状态码: 期望={expected}, 实际={actual}")
    protected void assertStatusCode(Response response, int expected) {
        int actual = response.getStatusCode();
        if (actual != expected) {
            log.error("状态码验证失败: 期望={}, 实际={}, 响应体={}", expected, actual, response.getBody().asString());
        }
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, 
                String.format("状态码验证失败: 期望=%d, 实际=%d", expected, actual));
    }
    
    /**
     * 验证响应包含指定字段
     */
    @Step("验证响应字段存在: {fieldPath}")
    protected void assertFieldExists(Response response, String fieldPath) {
        Object value = response.jsonPath().get(fieldPath);
        org.junit.jupiter.api.Assertions.assertNotNull(value, 
                String.format("字段不存在: %s", fieldPath));
    }
    
    /**
     * 验证响应字段值
     */
    @Step("验证字段值: {fieldPath} = {expected}")
    protected void assertFieldEquals(Response response, String fieldPath, Object expected) {
        Object actual = response.jsonPath().get(fieldPath);
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual,
                String.format("字段值不匹配 [%s]: 期望=%s, 实际=%s", fieldPath, expected, actual));
    }
    
    /**
     * 验证响应成功
     */
    @Step("验证响应成功")
    protected void assertSuccess(Response response) {
        assertStatusCode(response, 200);
        String code = response.jsonPath().getString("code");
        org.junit.jupiter.api.Assertions.assertEquals("0", code, "业务状态码验证失败");
    }
}
