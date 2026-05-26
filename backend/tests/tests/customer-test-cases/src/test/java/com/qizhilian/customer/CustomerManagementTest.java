package com.qizhilian.customer;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 客户管理模块API测试
 * 
 * 测试覆盖：
 * - 客户档案基础测试（新增/编辑/删除/查询）
 * - 客户跟进测试（跟进记录/提醒设置）
 * - 客户信用测试（信用额度/额度控制）
 * - 权限测试（未授权访问/无权限操作）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerManagementTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/customer";

    @Test
    @Order(1)
    @DisplayName("客户新增测试")
    void testCreateCustomer() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"测试客户\", \"contact\": \"张三\", \"phone\": \"13800138000\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .extract().response();
        Assertions.assertNotNull(response.jsonPath().getString("data.id"));
    }

    @Test
    @Order(2)
    @DisplayName("客户编辑测试")
    void testUpdateCustomer() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"待编辑\", \"contact\": \"李四\", \"phone\": \"13800138001\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .extract().response();
        String customerId = createResponse.jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"已编辑\", \"contact\": \"王五\"}")
                .when()
                .put(BASE_PATH + "/" + customerId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0));
    }

    @Test
    @Order(3)
    @DisplayName("客户删除测试")
    void testDeleteCustomer() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"待删除\", \"contact\": \"测试\", \"phone\": \"13800138002\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .extract().response();
        String customerId = createResponse.jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete(BASE_PATH + "/" + customerId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0));
    }

    @Test
    @Order(4)
    @DisplayName("客户查询测试")
    void testQueryCustomers() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .param("page", 1)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/list")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("客户跟进记录测试")
    void testCreateFollowUpRecord() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"跟进测试\", \"contact\": \"跟进\", \"phone\": \"13800138003\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .extract().response();
        String customerId = createResponse.jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"content\": \"客户有意向\", \"followUpType\": \"phone\"}")
                .when()
                .post(BASE_PATH + "/" + customerId + "/follow-up")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0));
    }

    @Test
    @Order(6)
    @DisplayName("客户信用额度设置测试")
    void testSetCreditLimit() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"信用测试\", \"contact\": \"信用\", \"phone\": \"13800138004\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .extract().response();
        String customerId = createResponse.jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"creditLimit\": 50000, \"creditPeriod\": 30}")
                .when()
                .put(BASE_PATH + "/" + customerId + "/credit")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0));
    }

    @Test
    @Order(7)
    @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given()
                .when()
                .get(BASE_PATH + "/list")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(8)
    @DisplayName("客户名称空值测试")
    void testEmptyCustomerName() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"\", \"contact\": \"测试\", \"phone\": \"13800138005\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
    }

    @Test
    @Order(9)
    @DisplayName("客户详情查询测试")
    void testGetCustomerDetail() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"name\": \"详情测试\", \"contact\": \"详情\", \"phone\": \"13800138006\"}")
                .when()
                .post(BASE_PATH)
                .then()
                .extract().response();
        String customerId = createResponse.jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/" + customerId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", equalTo(customerId));
    }

    @Test
    @Order(10)
    @DisplayName("客户分类筛选测试")
    void testFilterByCategory() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .param("category", "普通客户")
                .param("page", 1)
                .param("size", 10)
                .when()
                .get(BASE_PATH +/list")
