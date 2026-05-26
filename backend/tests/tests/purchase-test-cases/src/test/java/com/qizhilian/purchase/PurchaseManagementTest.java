package com.qizhilian.purchase;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 采购模块API测试
 * 
 * 测试覆盖：
 * - 供应商管理测试（新增/编辑/删除/分类/评估）
 * - 采购订单测试（创建/审核/状态流转/变更/查询）
 * - 采购入库测试（入库单创建/数量校验/质检流程）
 * - 采购退货测试（退货单创建/审批流程/入库处理）
 * - 权限测试（不同角色操作权限）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PurchaseManagementTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/purchase";

    // 供应商管理测试
    @Test
    @Order(1)
    @DisplayName("供应商新增测试")
    void testCreateSupplier() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"测试供应商-自动创建\",\n" +
                        "  \"contact\": \"张三\",\n" +
                        "  \"phone\": \"13800138000\",\n" +
                        "  \"address\": \"北京市朝阳区测试路123号\",\n" +
                        "  \"category\": \"电子产品\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/supplier")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.name", equalTo("测试供应商-自动创建"))
                .extract().response();

        String supplierId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(supplierId, "供应商ID不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("供应商编辑测试")
    void testUpdateSupplier() {
        // 先创建供应商
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"待编辑供应商\",\n" +
                        "  \"contact\": \"李四\",\n" +
                        "  \"phone\": \"13800138001\",\n" +
                        "  \"address\": \"北京市朝阳区测试路456号\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/supplier")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String supplierId = createResponse.jsonPath().getString("data.id");

        // 编辑供应商
        Response updateResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"已编辑供应商\",\n" +
                        "  \"contact\": \"王五\",\n" +
                        "  \"phone\": \"13800138002\",\n" +
                        "  \"address\": \"北京市海淀区 edits路789号\"\n" +
                        "}")
                .when()
                .put(BASE_PATH + "/supplier/" + supplierId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.name", equalTo("已编辑供应商"))
                .body("data.contact", equalTo("王五"))
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("供应商删除测试")
    void testDeleteSupplier() {
        // 先创建供应商
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"待删除供应商\",\n" +
                        "  \"contact\": \"测试用户\",\n" +
                        "  \"phone\": \"13800138003\",\n" +
                        "  \"address\": \"北京市朝阳区测试路\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/supplier")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String supplierId = createResponse.jsonPath().getString("data.id");

        // 删除供应商
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete(BASE_PATH + "/supplier/" + supplierId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0));
    }

    @Test
    @Order(4)
    @DisplayName("供应商分类管理测试")
    void testSupplierCategory() {
        // 创建分类
        Response createCategoryResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"测试分类\",\n" +
                        "  \"description\": \"测试用分类\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/supplier/category")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();

        String categoryId = createCategoryResponse.jsonPath().getString("data.id");

        // 查询分类列表
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/supplier/categories")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .extract().response();

        // 删除分类
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete(BASE_PATH + "/supplier/category/" + categoryId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0));
    }

    @Test
    @Order(5)
    @DisplayName("供应商评估打分测试")
    void testSupplierEvaluation() {
        // 先创建供应商
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"待评估供应商\",\n" +
                        "  \"contact\": \"测试用户\",\n" +
                        "  \"phone\": \"13800138004\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/supplier")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String supplierId = createResponse.jsonPath().getString("data.id");

        // 添加评估
        Response evaluateResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"" + supplierId + "\",\n" +
                        "  \"score\": 85,\n" +
                        "  \"comment\": \" Supplier质量良好，交货及时\",\n" +
                        "  \"evaluationDate\": \"2026-04-14\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/supplier/evaluation")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();

        // 查询评估记录
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/supplier/" + supplierId + "/evaluations")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.evaluations", notNullValue())
                .extract().response();
    }

    // 采购订单测试
    @Test
    @Order(6)
    @DisplayName("采购订单创建测试-单商品")
    void testCreatePurchaseOrderSingleItem() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 100,\n" +
                        "      \"unitPrice\": 25.50,\n" +
                        "      \"taxRate\": 0.13\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"deliveryDate\": \"2026-05-01\",\n" +
                        "  \"notes\": \"测试采购订单\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.orderNo", notNullValue())
                .body("data.status", equalTo("pending"))
                .body("data.totalAmount", greaterThan(0))
                .extract().response();
    }

    @Test
    @Order(7)
    @DisplayName("采购订单创建测试-多商品")
    void testCreatePurchaseOrderMultiItems() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 50,\n" +
                        "      \"unitPrice\": 25.50\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_789012\",\n" +
                        "      \"quantity\": 200,\n" +
                        "      \"unitPrice\": 15.00\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_345678\",\n" +
                        "      \"quantity\": 100,\n" +
                        "      \"unitPrice\": 35.00\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"deliveryDate\": \"2026-05-01\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.items", notNullValue())
                .body("data.items.size()", equalTo(3))
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("采购订单审核流程测试")
    void testPurchaseOrderApproval() {
        // 先创建订单
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 50,\n" +
                        "      \"unitPrice\": 25.50\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"deliveryDate\": \"2026-05-01\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String orderId = createResponse.jsonPath().getString("data.id");

        // 审核订单
        Response approveResponse = given()
                .header("Authorization", "Bearer " +mainUserToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"" + orderId + "\",\n" +
                        "  \"status\": \"approved\",\n" +
                        "  \"approvalComment\": \"同意采购\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order/" + orderId + "/approve")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // 采购入库测试
    @Test
    @Order(9)
    @DisplayName("入库单创建测试-关联订单")
    void testCreateInboundOrder() {
        // 先创建采购订单
        Response createOrderResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 100,\n" +
                        "      \"unitPrice\": 25.50\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"deliveryDate\": \"2026-05-01\",\n" +
                        "  \"status\": \"approved\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/order")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String orderId = createOrderResponse.jsonPath().getString("data.id");

        // 创建入库单
        Response createInboundResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"" + orderId + "\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"orderId\": \"" + orderId + "\",\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 100,\n" +
                        "      \"actualQuantity\": 98\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"warehouseId\": \"wh_123456\",\n" +
                        "  \"qualityCheckStatus\": \"pending\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/inbound")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.orderId", equalTo(orderId))
                .body("data.status", equalTo("pending"))
                .extract().response();
    }

    @Test
    @Order(10)
    @DisplayName("入库数量校验测试")
    void testInboundQuantityValidation() {
        // 验证入库数量不能大于订单数量
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"ord_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"orderId\": \"ord_123456\",\n" +
                        "      \"quantity\": 150,\n" +
                        "      \"actualQuantity\": 150\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/inbound")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400))
                .body("message", containsString("入库数量不能超过订单数量"));
    }

    // 采购退货测试
    @Test
    @Order(11)
    @DisplayName("退货单创建测试")
    void testCreateReturnOrder() {
        // 先创建入库单
        Response createInboundResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"ord_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"quantity\": 100,\n" +
                        "      \"actualQuantity\": 100\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"warehouseId\": \"wh_123456\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/inbound")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String inboundId = createInboundResponse.jsonPath().getString("data.id");

        // 创建退货单
        Response createReturnResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"inboundId\": \"" + inboundId + "\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_123456\",\n" +
                        "      \"inboundId\": \"" + inboundId + "\",\n" +
                        "      \"quantity\": 10,\n" +
                        "      \"reason\": \"商品损坏\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"returnType\": \"quality\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/return")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.inboundId", equalTo(inboundId))
                .body("data.status", equalTo("pending"))
                .extract().response();
    }

    @Test
    @Order(12)
    @DisplayName("采购订单查询测试")
    void testQueryPurchaseOrders() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("page", 1)
                .param("size", 10)
                .param("status", "pending")
                .when()
                .get(BASE_PATH + "/orders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .body("data.number", equalTo(0))
                .extract().response();
    }

    // 错误场景测试
    @Test
    @Order(13)
    @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given()
                .when()
                .get(BASE_PATH + "/orders")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(14)
    @DisplayName("无效订单ID测试")
    void testInvalidOrderId() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/order/invalid-id")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @Order(15)
    @DisplayName("库存不足测试")
    void testInsufficientStock() {
        // 尝试创建超出库存的入库单
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"ord_123456\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"prod_999999\",\n" +
                        "      \"quantity\": 999999,\n" +
                        "      \"actualQuantity\": 999999\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/inbound")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400))
                .body("message", containsString("库存不足"));
    }
}