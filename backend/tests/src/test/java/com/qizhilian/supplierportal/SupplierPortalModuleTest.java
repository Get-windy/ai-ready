package com.qizhilian.supplierportal;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 供应商门户模块API测试
 * 
 * 测试覆盖：
 * - 供应商注册与认证测试（注册流程/资质认证/审核状态/账号激活）
 * - 商品管理功能测试（商品发布/编辑/上下架/库存同步/价格管理）
 * - 订单处理功能测试（订单接收/确认/发货/物流更新/状态跟踪）
 * - 结算查询功能测试（对账单/结算明细/发票/付款记录）
 * - 权限与安全测试（角色权限/数据隔离/审计日志/会话超时）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SupplierPortalModuleTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/supplier-portal";

    // ==================== 供应商注册与认证测试 ====================
    @Test
    @Order(1)
    @DisplayName("供应商注册流程测试")
    void testSupplierRegistration() {
        Response response = given()
                .contentType("application/json")
                .body("{\n" +
                        "  \"companyName\": \"测试供应商有限公司\",\n" +
                        "  \"contactPerson\": \"张三\",\n" +
                        "  \"phone\": \"13800138000\",\n" +
                        "  \"email\": \"test@supplier.com\",\n" +
                        "  \"username\": \"test_supplier_001\",\n" +
                        "  \"password\": \"Test@123456\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("pending_review"))
                .extract().response();

        String supplierId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(supplierId, "供应商ID不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("资质认证上传测试")
    void testQualificationUpload() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", "business_license.jpg", "data".getBytes())
                .multiPart("type", "business_license")
                .when()
                .post(BASE_PATH + "/qualification/upload")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.fileName", notNullValue())
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("审核状态查询测试")
    void testReviewStatus() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/review/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", notNullValue())
                .extract().response();
    }

    @Test
    @Order(4)
    @DisplayName("账号激活测试")
    void testAccountActivation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post(BASE_PATH + "/account/activate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("active"))
                .extract().response();
    }

    @Test
    @Order(5)
    @DisplayName("重复企业名称注册测试")
    void testDuplicateRegistration() {
        given()
                .contentType("application/json")
                .body("{\n" +
                        "  \"companyName\": \"已存在的供应商\",\n" +
                        "  \"contactPerson\": \"李四\",\n" +
                        "  \"phone\": \"13800138001\",\n" +
                        "  \"username\": \"dup_supplier\",\n" +
                        "  \"password\": \"Test@123456\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/register")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("已存在"));
    }

    // ==================== 商品管理功能测试 ====================
    @Test
    @Order(6)
    @DisplayName("商品发布测试")
    void testPublishProduct() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productName\": \"测试商品\",\n" +
                        "  \"categoryId\": \"cat_electronics\",\n" +
                        "  \"price\": 99.99,\n" +
                        "  \"stock\": 100,\n" +
                        "  \"description\": \"测试商品描述\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("pending_review"))
                .extract().response();

        String productId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(productId, "商品ID不应为空");
    }

    @Test
    @Order(7)
    @DisplayName("商品编辑测试")
    void testUpdateProduct() {
        // 先发布商品
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productName\": \"待编辑商品\",\n" +
                        "  \"price\": 88.88,\n" +
                        "  \"stock\": 50\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String productId = createResponse.jsonPath().getString("data.id");

        // 编辑商品
        Response updateResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productName\": \"已编辑商品\",\n" +
                        "  \"price\": 108.88\n" +
                        "}")
                .when()
                .put(BASE_PATH + "/products/" + productId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.productName", equalTo("已编辑商品"))
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("商品上下架测试")
    void testProductShelves() {
        // 先发布商品
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productName\": \"上下架测试商品\",\n" +
                        "  \"price\": 77.77,\n" +
                        "  \"stock\": 30\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String productId = createResponse.jsonPath().getString("data.id");

        // 上架商品
        Response shelfResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"action\": \"shelves\",\n" +
                        "  \"productId\": \"" + productId + "\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/products/" + productId + "/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("online"))
                .extract().response();
    }

    @Test
    @Order(9)
    @DisplayName("库存同步测试")
    void testInventorySync() {
        // 先发布商品
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productName\": \"库存同步测试商品\",\n" +
                        "  \"price\": 66.66,\n" +
                        "  \"stock\": 20\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String productId = createResponse.jsonPath().getString("data.id");

        // 更新库存
        Response inventoryResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"" + productId + "\",\n" +
                        "  \"stock\": 50\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/inventory/sync")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(10)
    @DisplayName("价格管理测试")
    void testPriceManagement() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"price\": 128.88,\n" +
                        "  \"originalPrice\": 158.88\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/price/update")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(11)
    @DisplayName("商品库存为负数测试")
    void testNegativeStock() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"stock\": -10\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/inventory/update")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("库存"));
    }

    // ==================== 订单处理功能测试 ====================
    @Test
    @Order(12)
    @DisplayName("采购订单接收测试")
    void testOrderReception() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("status", "new")
                .when()
                .get(BASE_PATH + "/orders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .extract().response();
    }

    @Test
    @Order(13)
    @DisplayName("订单确认测试")
    void testOrderConfirmation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"po_123456\",\n" +
                        "  \"deliveryDate\": \"2026-04-20\",\n" +
                        "  \"remark\": \"确认订单\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/orders/po_123456/confirm")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("confirmed"))
                .extract().response();
    }

    @Test
    @Order(14)
    @DisplayName("订单发货测试")
    void testOrderDelivery() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"po_123456\",\n" +
                        "  \"carrier\": \"顺丰速运\",\n" +
                        "  \"trackingNumber\": \"SF123456789CN\",\n" +
                        "  \"sentDate\": \"2026-04-15\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/orders/po_123456/delivery")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("shipped"))
                .extract().response();
    }

    @Test
    @Order(15)
    @DisplayName("物流信息更新测试")
    void testUpdateLogistics() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"po_123456\",\n" +
                        "  \"carrier\": \"顺丰速运\",\n" +
                        "  \"trackingNumber\": \"SF987654321CN\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/orders/po_123456/logistics")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.trackingNumber", equalTo("SF987654321CN"))
                .extract().response();
    }

    @Test
    @Order(16)
    @DisplayName("订单状态跟踪测试")
    void testOrderTracking() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/orders/po_123456/track")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.history", notNullValue())
                .extract().response();
    }

    // ==================== 结算查询功能测试 ====================
    @Test
    @Order(17)
    @DisplayName("对账单查询测试")
    void testReconciliation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/settlement/reconciliation")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period", equalTo("2026-03"))
                .body("data.totalAmount", notNullValue())
                .extract().response();
    }

    @Test
    @Order(18)
    @DisplayName("结算明细查看测试")
    void testSettlementDetails() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("reconciliationId", "rec_123456")
                .when()
                .get(BASE_PATH + "/settlement/details")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.items", notNullValue())
                .extract().response();
    }

    @Test
    @Order(19)
    @DisplayName("发票管理测试")
    void testInvoiceManagement() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", "invoice.pdf", "data".getBytes())
                .multiPart("type", "invoice")
                .when()
                .post(BASE_PATH + "/invoice/upload")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.fileName", notNullValue())
                .extract().response();
    }

    @Test
    @Order(20)
    @DisplayName("付款记录查询测试")
    void testPaymentRecords() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-03-31")
                .when()
                .get(BASE_PATH + "/payments/records")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.records", notNullValue())
                .extract().response();
    }

    // ==================== 权限与安全测试 ====================
    @Test
    @Order(21)
    @DisplayName("角色权限控制测试")
    void testRolePermissions() {
        // 供应商角色尝试访问管理员接口
        Response response = given()
                .header("Authorization", "Bearer " + supplierToken)
                .when()
                .get(BASE_PATH + "/admin/users")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    @Test
    @Order(22)
    @DisplayName("数据隔离验证测试")
    void testDataIsolation() {
        // 供应商A尝试访问供应商B的数据
        Response response = given()
                .header("Authorization", "Bearer " + supplierAToken)
                .when()
                .get(BASE_PATH + "/orders/supplier_b_orders")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    @Test
    @Order(23)
    @DisplayName("敏感操作审计测试")
    void testAuditLog() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("operation", "price_update")
                .param("startDate", "2026-04-01")
                .param("endDate", "2026-04-14")
                .when()
                .get(BASE_PATH + "/audit/log")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.logs", notNullValue())
                .extract().response();
    }

    @Test
    @Order(24)
    @DisplayName("会话超时测试")
    void testSessionTimeout() {
        given()
                .header("Authorization", "Bearer " + "expired_token_999")
                .when()
                .get(BASE_PATH + "/orders")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", not(equalTo(0)));
    }

    // ==================== 异常测试 ====================
    @Test
    @Order(25)
    @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given()
                .when()
                .get(BASE_PATH + "/products")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(26)
    @DisplayName("无效供应商ID测试")
    void testInvalidSupplierId() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/suppliers/invalid_supplier_999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", not(equalTo(0)));
    }
}