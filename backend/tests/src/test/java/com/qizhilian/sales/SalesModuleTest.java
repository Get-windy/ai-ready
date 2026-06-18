package com.qizhilian.sales;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import java.util.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SalesModuleTest {
    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");
    private String orderId;
    private String customerId;

    @BeforeAll
    void setUp() { RestAssured.baseURI = BASE_URL; }

    @Test
    @Order(1)
    @DisplayName("SALES-001: 单商品订单创建测试")
    void testCreateSingleProductOrder() {
        Map<String, Object> order = new HashMap<>();
        order.put("customerId", "C001");
        order.put("items", Arrays.asList(Map.of("productId", "P001", "quantity", 10, "price", 100)));
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(order).when().post("/api/v1/sales/orders").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        orderId = response.jsonPath().getString("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("SALES-002: 多商品订单创建测试")
    void testCreateMultiProductOrder() {
        Map<String, Object> order = new HashMap<>();
        order.put("customerId", "C002");
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("productId", "P001", "quantity", 5, "price", 100));
        items.add(Map.of("productId", "P002", "quantity", 3, "price", 200));
        order.put("items", items);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(order).when().post("/api/v1/sales/orders").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(3)
    @DisplayName("SALES-003: 订单状态流转测试")
    void testOrderStatusFlow() {
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("orderId", orderId);
        statusUpdate.put("status", "CONFIRMED");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(statusUpdate).when().put("/api/v1/sales/orders/status").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(4)
    @DisplayName("SALES-004: 订单变更测试")
    void testOrderUpdate() {
        Map<String, Object> update = new HashMap<>();
        update.put("orderId", orderId);
        update.put("remark", "订单备注更新");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(update).when().put("/api/v1/sales/orders/" + orderId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(5)
    @DisplayName("SALES-005: 订单取消测试")
    void testOrderCancel() {
        Map<String, Object> cancel = new HashMap<>();
        cancel.put("reason", "客户要求取消");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(cancel).when().post("/api/v1/sales/orders/" + orderId + "/cancel").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(6)
    @DisplayName("SALES-006: 订单查询导出测试")
    void testOrderQueryExport() {
        Response response = given().header("Authorization", "