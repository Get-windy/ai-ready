package com.qizhilian.portal;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 客户门户模块API测试
 * 
 * 测试覆盖：
 * - 商品浏览功能测试（列表/分类/详情/搜索/分页）
 * - 购物车功能测试（添加/修改/删除/结算）
 * - 在线下单功能测试（订单确认/地址选择/支付方式/提交）
 * - 订单追踪功能测试（状态展示/物流追踪/详情/确认收货）
 * - 权限测试（未登录访问/已登录访问/会话过期）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PortalModuleTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/portal";

    // ==================== 商品浏览功能测试 ====================
    @Test
    @Order(1)
    @DisplayName("商品列表展示测试")
    void testProductList() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("page", 1)
                .param("size", 20)
                .when()
                .get(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .body("data.number", equalTo(0))
                .extract().response();

        Assertions.assertTrue(response.jsonPath().getInt("data.totalElements") >= 0, "商品列表应有数据");
    }

    @Test
    @Order(2)
    @DisplayName("商品分类筛选测试")
    void testProductCategoryFilter() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("categoryId", "cat_electronics")
                .param("page", 1)
                .param("size", 20)
                .when()
                .get(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("商品详情展示测试")
    void testProductDetail() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/products/prod_123456")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", equalTo("prod_123456"))
                .body("data.name", notNullValue())
                .body("data.price", notNullValue())
                .body("data.stock", notNullValue())
                .extract().response();
    }

    @Test
    @Order(4)
    @DisplayName("商品搜索功能测试")
    void testProductSearch() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("keyword", "手机")
                .param("page", 1)
                .param("size", 20)
                .when()
                .get(BASE_PATH + "/products/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .extract().response();
    }

    @Test
    @Order(5)
    @DisplayName("商品分页测试")
    void testProductPagination() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("page", 2)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.number", equalTo(1))
                .body("data.size", equalTo(10))
                .extract().response();
    }

    // ==================== 购物车功能测试 ====================
    @Test
    @Order(6)
    @DisplayName("添加商品到购物车测试")
    void testAddToCart() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"quantity\": 2,\n" +
                        "  \"remarks\": \"测试加入购物车\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.productId", equalTo("prod_123456"))
                .body("data.quantity", equalTo(2))
                .extract().response();

        String cartItemId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(cartItemId, "购物车项ID不应为空");
    }

    @Test
    @Order(7)
    @DisplayName("修改购物车商品数量测试")
    void testUpdateCartQuantity() {
        // 先添加商品到购物车
        Response addResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"quantity\": 1\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String cartItemId = addResponse.jsonPath().getString("data.id");

        // 修改数量
        Response updateResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"quantity\": 5\n" +
                        "}")
                .when()
                .put(BASE_PATH + "/cart/" + cartItemId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.quantity", equalTo(5))
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("删除购物车商品测试")
    void testRemoveFromCart() {
        // 先添加商品到购物车
        Response addResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"quantity\": 1\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String cartItemId = addResponse.jsonPath().getString("data.id");

        // 删除购物车商品
        Response deleteResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete(BASE_PATH + "/cart/" + cartItemId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(9)
    @DisplayName("购物车结算测试")
    void testCartCheckout() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/cart/checkout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.items", notNullValue())
                .body("data.totalAmount", notNullValue())
                .extract().response();
    }

    @Test
    @Order(10)
    @DisplayName("购物车数量超库存测试")
    void testCartQuantityExceedStock() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"quantity\": 999999\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("库存不足"));
    }

    // ==================== 在线下单功能测试 ====================
    @Test
    @Order(11)
    @DisplayName("订单确认流程测试")
    void testOrderConfirmation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 2\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"addressId\": \"addr_123456\",\n" +
                        "  \"paymentMethod\": \"online\",\n" +
                        "  \"remark\": \"测试订单\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.orderNo", notNullValue())
                .body("data.status", equalTo("pending_payment"))
                .extract().response();

        String orderId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(orderId, "订单ID不应为空");
    }

    @Test
    @Order(12)
    @DisplayName("收货地址选择测试")
    void testSelectAddress() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/addresses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .extract().response();
    }

    @Test
    @Order(13)
    @DisplayName("新增收货地址测试")
    void testAddAddress() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"receiverName\": \"张三\",\n" +
                        "  \"phone\": \"13800138000\",\n" +
                        "  \"province\": \"北京市\",\n" +
                        "  \"city\": \"北京市\",\n" +
                        "  \"district\": \"朝阳区\",\n" +
                        "  \"detailAddress\": \"测试路123号\",\n" +
                        "  \"isDefault\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/addresses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.receiverName", equalTo("张三"))
                .body("data.isDefault", equalTo(true))
                .extract().response();

        String addressId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(addressId, "地址ID不应为空");
    }

    @Test
    @Order(14)
    @DisplayName("支付方式选择测试")
    void testSelectPaymentMethod() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/payment-methods")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .extract().response();
    }

    @Test
    @Order(15)
    @DisplayName("订单提交空地址测试")
    void testOrderWithoutAddress() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"items\": [\n" +
                        "    {\"productId\": \"prod_123456\", \"quantity\": 1}\n" +
                        "  ],\n" +
                        "  \"addressId\": \"\",\n" +
                        "  \"paymentMethod\": \"online\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("收货地址"));
    }

    // ==================== 订单追踪功能测试 ====================
    @Test
    @Order(16)
    @DisplayName("订单状态展示测试")
    void testOrderStatusList() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("page", 1)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/orders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .extract().response();
    }

    @Test
    @Order(17)
    @DisplayName("物流信息追踪测试")
    void testLogisticsTracking() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/orders/order_123456/logistics")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.trackingNumber", notNullValue())
                .body("data.carrier", notNullValue())
                .body("data.traces", notNullValue())
                .extract().response();
    }

    @Test
    @Order(18)
    @DisplayName("订单详情查看测试")
    void testOrderDetail() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/orders/order_123456")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", equalTo("order_123456"))
                .body("data.items", notNullValue())
                .body("data.address", notNullValue())
                .body("data.paymentInfo", notNullValue())
                .extract().response();
    }

    @Test
    @Order(19)
    @DisplayName("确认收货测试")
    void testConfirmReceipt() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"order_123456\",\n" +
                        "  \"confirm\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/orders/order_123456/confirm-receipt")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("completed"))
                .extract().response();
    }

    // ==================== 权限测试 ====================
    @Test
    @Order(20)
    @DisplayName("未登录用户访问控制测试")
    void testUnauthorizedAccess() {
        given()
                .when()
                .get(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given()
                .when()
                .get(BASE_PATH + "/orders")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(21)
    @DisplayName("已登录用户功能访问测试")
    void testLoggedInAccess() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(22)
    @DisplayName("会话过期测试")
    void testSessionExpired() {
        given()
                .header("Authorization", "Bearer " + "expired_token")
                .when()
                .get(BASE_PATH + "/cart")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", not(equalTo(0)));
    }

    // ==================== 异常测试 ====================
    @Test
    @Order(23)
    @DisplayName("无效商品ID测试")
    void testInvalidProductId() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/products/invalid_product_999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", not(equalTo(0)));
    }

    @Test
    @Order(24)
    @DisplayName("无效订单ID测试")
    void testInvalidOrderId() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/orders/invalid_order_999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", not(equalTo(0)));
    }

    @Test
    @Order(25)
    @DisplayName("购物车为空时结算测试")
    void testEmptyCartCheckout() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/cart/checkout")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("购物车为空"));
    }
}