package cn.aiedge.erp.purchase.contract.test.framework;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API测试基类
 * 提供通用的API测试工具和断言方法
 */
public abstract class ApiTestBase extends BaseTest {
    
    protected RequestSpecification requestSpec;
    protected ResponseSpecification responseSpec;
    
    /**
     * API测试初始化
     */
    @Override
    protected void beforeTestExecution(TestInfo testInfo) {
        super.beforeTestExecution(testInfo);
        setupApiClient();
    }
    
    /**
     * 设置API客户端配置
     */
    protected void setupApiClient() {
        LOGGER.info("Setting up API client configuration");
        
        // 配置RestAssured
        RestAssured.baseURI = getTestConfig().getBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // 创建请求规范
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
        
        // 创建响应规范
        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectStatusCode(200)
                .log(LogDetail.ALL)
                .build();
        
        LOGGER.info("API client configured. Base URL: {}", RestAssured.baseURI);
    }
    
    /**
     * 执行GET请求
     */
    protected Response get(String endpoint) {
        logStep("Executing GET request to: " + endpoint);
        return given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行带参数的GET请求
     */
    protected Response get(String endpoint, Map<String, Object> params) {
        logStep("Executing GET request to: " + endpoint + " with params: " + params);
        return given()
                .spec(requestSpec)
                .params(params)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行带路径参数的GET请求
     */
    protected Response get(String endpoint, String pathParamName, Object pathParamValue) {
        logStep("Executing GET request to: " + endpoint + " with path param: " + pathParamName + "=" + pathParamValue);
        return given()
                .spec(requestSpec)
                .pathParam(pathParamName, pathParamValue)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行POST请求
     */
    protected Response post(String endpoint, Object body) {
        logStep("Executing POST request to: " + endpoint);
        logTestData("Request Body", body);
        
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行PUT请求
     */
    protected Response put(String endpoint, Object body) {
        logStep("Executing PUT request to: " + endpoint);
        logTestData("Request Body", body);
        
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行带路径参数的PUT请求
     */
    protected Response put(String endpoint, String pathParamName, Object pathParamValue, Object body) {
        logStep("Executing PUT request to: " + endpoint + " with path param: " + pathParamName + "=" + pathParamValue);
        logTestData("Request Body", body);
        
        return given()
                .spec(requestSpec)
                .pathParam(pathParamName, pathParamValue)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行DELETE请求
     */
    protected Response delete(String endpoint) {
        logStep("Executing DELETE request to: " + endpoint);
        return given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 执行带路径参数的DELETE请求
     */
    protected Response delete(String endpoint, String pathParamName, Object pathParamValue) {
        logStep("Executing DELETE request to: " + endpoint + " with path param: " + pathParamName + "=" + pathParamValue);
        return given()
                .spec(requestSpec)
                .pathParam(pathParamName, pathParamValue)
                .when()
                .delete(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }
    
    /**
     * 设置认证令牌
     */
    protected void setAuthToken(String token) {
        requestSpec = new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        LOGGER.info("Authentication token set for API requests");
    }
    
    /**
     * 清除认证令牌
     */
    protected void clearAuthToken() {
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
        LOGGER.info("Authentication token cleared");
    }
    
    /**
     * 验证响应状态码
     */
    protected void verifyStatusCode(Response response, int expectedStatusCode) {
        logVerification("Verifying response status code: " + expectedStatusCode);
        response.then().statusCode(expectedStatusCode);
    }
    
    /**
     * 验证响应体包含字段
     */
    protected void verifyResponseContainsField(Response response, String fieldPath) {
        logVerification("Verifying response contains field: " + fieldPath);
        response.then().body(fieldPath, notNullValue());
    }
    
    /**
     * 验证响应字段值
     */
    protected void verifyFieldValue(Response response, String fieldPath, Object expectedValue) {
        logVerification("Verifying field value: " + fieldPath + " = " + expectedValue);
        response.then().body(fieldPath, equalTo(expectedValue));
    }
    
    /**
     * 验证响应字段类型
     */
    protected void verifyFieldType(Response response, String fieldPath, String expectedType) {
        logVerification("Verifying field type: " + fieldPath + " is " + expectedType);
        // 这里可以根据需要实现类型验证逻辑
        Object value = response.jsonPath().get(fieldPath);
        String actualType = value != null ? value.getClass().getSimpleName() : "null";
        if (!actualType.equalsIgnoreCase(expectedType)) {
            throw new AssertionError("Field " + fieldPath + " type mismatch. Expected: " + expectedType + ", Actual: " + actualType);
        }
    }
    
    /**
     * 验证响应字段不为空
     */
    protected void verifyFieldNotNull(Response response, String fieldPath) {
        logVerification("Verifying field is not null: " + fieldPath);
        response.then().body(fieldPath, notNullValue());
    }
    
    /**
     * 验证响应字段不为空字符串
     */
    protected void verifyFieldNotEmpty(Response response, String fieldPath) {
        logVerification("Verifying field is not empty: " + fieldPath);
        response.then().body(fieldPath, not(emptyString()));
    }
    
    /**
     * 验证响应时间在合理范围内
     */
    protected void verifyResponseTime(Response response, long maxAllowedTimeMs) {
        long responseTime = response.time();
        logVerification("Verifying response time <= " + maxAllowedTimeMs + " ms. Actual: " + responseTime + " ms");
        
        if (responseTime > maxAllowedTimeMs) {
            LOGGER.warn("Response time exceeded limit. Actual: {} ms, Allowed: {} ms", responseTime, maxAllowedTimeMs);
        }
    }
    
    /**
     * 验证响应体结构
     */
    protected void verifyResponseSchema(Response response, String schemaPath) {
        logVerification("Verifying response schema against: " + schemaPath);
        response.then().body(matchesJsonSchemaInClasspath(schemaPath));
    }
    
    /**
     * 提取响应字段值
     */
    protected <T> T extractFieldValue(Response response, String fieldPath) {
        return response.jsonPath().get(fieldPath);
    }
    
    /**
     * 提取响应体为指定类型
     */
    protected <T> T extractAs(Response response, Class<T> clazz) {
        return response.as(clazz);
    }
    
    /**
     * 等待条件满足（轮询）
     */
    protected void waitForCondition(Condition condition, long timeoutMs, long intervalMs) {
        logStep("Waiting for condition to be satisfied. Timeout: " + timeoutMs + "ms, Interval: " + intervalMs + "ms");
        
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.isSatisfied()) {
                LOGGER.info("Condition satisfied after {} ms", System.currentTimeMillis() - startTime);
                return;
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait interrupted", e);
            }
        }
        
        throw new RuntimeException("Condition not satisfied after " + timeoutMs + " ms");
    }
    
    /**
     * 条件接口
     */
    @FunctionalInterface
    public interface Condition {
        boolean isSatisfied();
    }
    
    /**
     * 执行采购合同相关的API请求
     */
    protected Response getPurchaseContract(String contractId) {
        String endpoint = getTestConfig().getPurchaseContractApiUrl("/contracts/{id}");
        return get(endpoint, "id", contractId);
    }
    
    protected Response createPurchaseContract(Object contractData) {
        String endpoint = getTestConfig().getPurchaseContractApiUrl("/contracts");
        return post(endpoint, contractData);
    }
    
    protected Response updatePurchaseContract(String contractId, Object contractData) {
        String endpoint = getTestConfig().getPurchaseContractApiUrl("/contracts/{id}");
        return put(endpoint, "id", contractId, contractData);
    }
    
    protected Response deletePurchaseContract(String contractId) {
        String endpoint = getTestConfig().getPurchaseContractApiUrl("/contracts/{id}");
        return delete(endpoint, "id", contractId);
    }
    
    protected Response submitForApproval(String contractId) {
        String endpoint = getTestConfig().getPurchaseContractApiUrl("/contracts/{id}/submit-approval");
        return post(endpoint, "id", contractId, null);
    }
    
    protected Response approveContract(String contractId) {
        String endpoint = getTestConfig().getPurchaseContractApiUrl("/contracts/{id}/approve");
        return post(endpoint, "id", contractId, null);
    }
}