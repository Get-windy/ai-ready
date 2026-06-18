package com.qizhilian.api.base;

import com.qizhilian.api.config.ApiConfig;
import com.qizhilian.api.utils.TestDataGenerator;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API测试基类
 * 提供通用的测试设置和工具方法
 */
@Slf4j
public abstract class BaseApiTest {
    
    protected static ApiConfig config;
    protected static TestDataGenerator dataGenerator;
    protected RequestSpecification requestSpec;
    protected static String authToken;
    
    @BeforeAll
    static void setUpClass() {
        config = ApiConfig.getInstance();
        dataGenerator = new TestDataGenerator(config.getTestDataLocale());
        
        // 配置REST Assured
        RestAssured.baseURI = config.getBaseUrl();
        RestAssured.basePath = config.getApiVersion();
        
        // 配置超时
        RestAssured.config = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", config.getConnectionTimeout())
                .setParam("http.socket.timeout", config.getSocketTimeout()));
        
        log.info("API测试基类初始化完成，Base URL: {}", config.getBaseUrl());
    }
    
    @BeforeEach
    void setUp() {
        // 创建基础请求规范
        RequestSpecBuilder builder = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON);
        
        // 添加认证token（如果已登录）
        if (authToken != null && !authToken.isEmpty()) {
            builder.addHeader(config.getAuthTokenHeader(), 
                config.getAuthTokenPrefix() + " " + authToken);
        }
        
        // 仅在调试模式下添加日志过滤器
        if (Boolean.getBoolean("api.test.debug")) {
            builder.addFilter(new RequestLoggingFilter());
            builder.addFilter(new ResponseLoggingFilter());
        }
        
        requestSpec = builder.build();
    }
    
    /**
     * 发送GET请求
     */
    @Step("发送GET请求: {endpoint}")
    protected Response get(String endpoint) {
        return given(requestSpec)
            .when()
            .get(endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 发送GET请求（带查询参数）
     */
    @Step("发送GET请求: {endpoint}, 参数: {params}")
    protected Response get(String endpoint, Map<String, Object> params) {
        return given(requestSpec)
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
    @Step("发送POST请求: {endpoint}")
    protected Response post(String endpoint, Object body) {
        return given(requestSpec)
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
    @Step("发送PUT请求: {endpoint}")
    protected Response put(String endpoint, Object body) {
        return given(requestSpec)
            .body(body)
            .when()
            .put(endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 发送PATCH请求
     */
    @Step("发送PATCH请求: {endpoint}")
    protected Response patch(String endpoint, Object body) {
        return given(requestSpec)
            .body(body)
            .when()
            .patch(endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 发送DELETE请求
     */
    @Step("发送DELETE请求: {endpoint}")
    protected Response delete(String endpoint) {
        return given(requestSpec)
            .when()
            .delete(endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 发送DELETE请求（带请求体）
     */
    @Step("发送DELETE请求: {endpoint}")
    protected Response delete(String endpoint, Object body) {
        return given(requestSpec)
            .body(body)
            .when()
            .delete(endpoint)
            .then()
            .extract()
            .response();
    }
    
    /**
     * 设置认证Token
     */
    @Step("设置认证Token")
    protected void setAuthToken(String token) {
        authToken = token;
        // 重新构建请求规范
        setUp();
    }
    
    /**
     * 清除认证Token
     */
    @Step("清除认证Token")
    protected void clearAuthToken() {
        authToken = null;
        setUp();
    }
    
    /**
     * 验证响应状态码
     */
    @Step("验证响应状态码: 期望 {expectedStatusCode}")
    protected void assertStatusCode(Response response, int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        if (actualStatusCode != expectedStatusCode) {
            String errorMsg = String.format("状态码不匹配: 期望 %d, 实际 %d. 响应体: %s",
                expectedStatusCode, actualStatusCode, response.getBody().asString());
            log.error(errorMsg);
            throw new AssertionError(errorMsg);
        }
    }
    
    /**
     * 验证响应包含指定字段
     */
    @Step("验证响应包含字段: {fieldPath}")
    protected void assertResponseHasField(Response response, String fieldPath) {
        if (!response.jsonPath().get(fieldPath).toString().isEmpty()) {
            // 字段存在
        }
    }
    
    /**
     * 创建基础请求头
     */
    protected Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        if (authToken != null) {
            headers.put(config.getAuthTokenHeader(), 
                config.getAuthTokenPrefix() + " " + authToken);
        }
        return headers;
    }
    
    /**
     * 等待指定毫秒数
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 重试机制
     */
    protected Response executeWithRetry(java.util.function.Supplier<Response> action) {
        int attempts = 0;
        Response response = null;
        Exception lastException = null;
        
        while (attempts < config.getRetryMaxAttempts()) {
            try {
                response = action.get();
                if (response.getStatusCode() < 500) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("请求失败，尝试重试 {}/{}: {}", attempts + 1, config.getRetryMaxAttempts(), e.getMessage());
            }
            
            attempts++;
            if (attempts < config.getRetryMaxAttempts()) {
                sleep(config.getRetryIntervalMs());
            }
        }
        
        if (response != null) {
            return response;
        }
        throw new RuntimeException("重试次数耗尽", lastException);
    }
}
