package com.qizhilian.order;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderManagementTest extends ApiBaseTest {
    private static final String BASE_PATH = "/api/v1/order";

    @Test @Order(1) @DisplayName("订单创建测试")
    void testCreateOrder() {
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("{\"customerId\": \"cust_001\", \"items\": [{\"productId\": \"prod_001\", \"quantity\": 10, \"price\": 100}]}")
            .when().post(BASE_PATH)
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(2) @DisplayName("订单编辑测试")
    void testUpdateOrder() {
        Response r = given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("{\"customerId\": \"cust_001\", \"items\": [{\"productId\": \"prod_001\", \"quantity\": 5}]}")
            .when().post(BASE_PATH).then().extract().response();
        String orderId = r.jsonPath().getString("data.id");
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"quantity\": 20}")
            .when().put(BASE_PATH + "/" + orderId)
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(3) @DisplayName("订单取消测试")
    void testCancelOrder() {
        Response r = given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"customerId\": \"cust_001\"}")
            .when().post(BASE_PATH).then().extract().response();
        String orderId = r.jsonPath().getString("data.id");
        given().header("Authorization", "Bearer " + accessToken)
            .when().post(BASE_PATH + "/" + orderId + "/cancel")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(4) @DisplayName("订单查询测试")
    void testQueryOrders() {
        given().header("Authorization", "Bearer " + accessToken)
            .param("page", 1).param("size", 10)
            .when().get(BASE_PATH + "/list")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(5) @DisplayName("订单审核测试")
    void testApproveOrder() {
        Response r = given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"customerId\": \"cust_001\", \"status\": \"pending\"}")
            .when().post(BASE_PATH).then().extract().response();
        String orderId = r.jsonPath().getString("data.id");
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"status\": \"approved\"}")
            .when().post(BASE_PATH + "/" + orderId + "/approve")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(6) @DisplayName("订单明细管理测试")
    void testOrderItems() {
        given().header("Authorization", "Bearer " + accessToken)
            .param("orderId", "ord_001")
            .when().get(BASE_PATH + "/items")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(7) @DisplayName("订单价格计算测试")
    void testPriceCalculation() {
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("{\"items\": [{\"productId\": \"prod_001\", \"quantity\": 100, \"price\": 50, \"discount\": 0.1}]}")
            .when().post(BASE_PATH + "/calculate")
            .then().statusCode(HttpStatus.OK.value()).body("data.totalAmount", greaterThan(0));
    }

    @Test @Order(8) @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given().when().get(BASE_PATH + "/list")
            .then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test @Order(9) @DisplayName("订单金额边界测试")
    void testAmountBoundary() {
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("{\"customerId\": \"cust_001\", \"items\": [{\"productId\": \"prod_001\", \"quantity\": 999999, \"price\": 999999}]}")
            .when().post(BASE_PATH)
            .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test @Order(10) @DisplayName("订单详情查询测试")
    void testOrderDetail() {
        Response r = given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"customerId\": \"cust_001\"}")
            .when().post(BASE_PATH).then().extract().response();
        String orderId = r.jsonPath().getString("data.id");
        given().header("Authorization", "Bearer " + accessToken)
            .when().get(BASE_PATH + "/" + orderId)
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }
}