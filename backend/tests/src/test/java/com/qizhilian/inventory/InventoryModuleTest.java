package com.qizhilian.inventory;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import java.util.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InventoryModuleTest {
    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");
    private String warehouseId;
    private String productId;
    private String stockInId;
    private String stockOutId;
    private String checkId;
    private String transferId;

    @BeforeAll
    void setUp() { RestAssured.baseURI = BASE_URL; }

    // 库存查询测试
    @Test
    @Order(1)
    @DisplayName("INV-QUERY-001: 库存总览查询测试")
    void testInventoryOverview() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).when().get("/api/v1/inventory/overview").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getMap("data")).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("INV-QUERY-002: 商品库存明细查询测试")
    void testProductInventoryDetail() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).queryParam("productId", "P001").when().get("/api/v1/inventory/detail").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(3)
    @DisplayName("INV-QUERY-003: 仓库库存分布查询测试")
    void testWarehouseDistribution() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).when().get("/api/v1/inventory/warehouse/distribution").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(4)
    @DisplayName("INV-QUERY-004: 库存预警查询测试")
    void testInventoryWarning() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).when().get("/api/v1/inventory/warning").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // 入库管理测试
    @Test
    @Order(10)
    @DisplayName("INV-IN-001: 采购入库测试")
    void testPurchaseStockIn() {
        Map<String, Object> stockIn = new HashMap<>();
        stockIn.put("type", "PURCHASE");
        stockIn.put("productId", "P001");
        stockIn.put("quantity", 100);
        stockIn.put("warehouseId", "W001");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(stockIn).when().post("/api/v1/inventory/stock-in").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        stockInId = response.jsonPath().getString("data.id");
    }

    @Test
    @Order(11)
    @DisplayName("INV-IN-002: 生产入库测试")
    void testProductionStockIn() {
        Map<String, Object> stockIn = new HashMap<>();
        stockIn.put("type", "PRODUCTION");
        stockIn.put("productId", "P002");
        stockIn.put("quantity", 50);
        stockIn.put("warehouseId", "W001");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(stockIn).when().post("/api/v1/inventory/stock-in").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(12)
    @DisplayName("INV-IN-003: 退货入库测试")
    void testReturnStockIn() {
        Map<String, Object> stockIn = new HashMap<>();
        stockIn.put("type", "RETURN");
        stockIn.put("productId", "P001");
        stockIn.put("quantity", 10);
        stockIn.put("warehouseId", "W001");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(stockIn).when().post("/api/v1/inventory/stock-in").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(13)
    @DisplayName("INV-IN-004: 入库质检测试")
    void testQualityCheck() {
        Map<String, Object> qualityCheck = new HashMap<>();
        qualityCheck.put("stockInId", stockInId);
        qualityCheck.put("result", "PASS");
        qualityCheck.put("inspector", "张三");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(qualityCheck).when().post("/api/v1/inventory/quality-check").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // 出库管理测试
    @Test
    @Order(20)
    @DisplayName("INV-OUT-001: 销售出库测试")
    void testSalesStockOut() {
        Map<String, Object> stockOut = new HashMap<>();
        stockOut.put("type", "SALES");
        stockOut.put("productId", "P001");
        stockOut.put("quantity", 20);
        stockOut.put("warehouseId", "W001");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(stockOut).when().post("/api/v1/inventory/stock-out").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        stockOutId = response.jsonPath().getString("data.id");
    }

    @Test
    @Order(21)
    @DisplayName("INV-OUT-002: 生产出库测试")
    void testProductionStockOut() {
        Map<String, Object> stockOut = new HashMap<>();
        stockOut.put("type", "PRODUCTION");
        stockOut.put("productId", "P002");
        stockOut.put("quantity", 15);
        stockOut.put("warehouseId", "W001");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(stockOut).when().post("/api/v1/inventory/stock-out").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(22)
    @DisplayName("INV-OUT-003: 调拨出库测试")
    void testTransferStockOut() {
        Map<String, Object> stockOut = new HashMap<>();
        stockOut.put("type", "TRANSFER");
        stockOut.put("productId", "P001");
        stockOut.put("quantity", 30);
        stockOut.put("warehouseId", "W001");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(stockOut).when().post("/api/v1/inventory/stock-out").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(23)
    @DisplayName("INV-OUT-004: 出库复核测试")
    void testStockOutReview() {
        Map<String, Object> review = new HashMap<>();
        review.put("stockOutId", stockOutId);
        review.put("reviewer", "李四");
        review.put("result", "APPROVED");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(review).when().post("/api/v1/inventory/stock-out/review").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // 库存盘点测试
    @Test
    @Order(30)
    @DisplayName("INV-CHECK-001: 盘点单创建测试")
    void testCreateCheckSheet() {
        Map<String, Object> checkSheet = new HashMap<>();
        checkSheet.put("warehouseId", "W001");
        checkSheet.put("checker", "王五");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(checkSheet).when().post("/api/v1/inventory/check").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        checkId = response.jsonPath().getString("data.id");
    }

    @Test
    @Order(31)
    @DisplayName("INV-CHECK-002: 盘点数据录入测试")
    void testCheckDataEntry() {
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productId", "P001");
        item.put("systemQty", 100);
        item.put("actualQty", 98);
        items.add(item);
        Map<String, Object> data = new HashMap<>();
        data.put("checkId", checkId);
        data.put("items", items);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(data).when().post("/api/v1/inventory/check/data").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(32)
    @DisplayName("INV-CHECK-003: 盘点差异处理测试")
    void testCheckDifferenceHandle() {
        Map<String, Object> handle = new HashMap<>();
        handle.put("checkId", checkId);
        handle.put("handler", "赵六");
        handle.put("action", "ADJUST");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(handle).when().post("/api/v1/inventory/check/handle").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(33)
    @DisplayName("INV-CHECK-004: 盘点盈亏调整测试")
    void testProfitLossAdjustment() {
        Map<String, Object> adjustment = new HashMap<>();
        adjustment.put("checkId", checkId);
        adjustment.put("reason", "自然损耗");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(adjustment).when().post("/api/v1/inventory/check/adjustment").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // 库存调拨测试
    @Test
    @Order(40)
    @DisplayName("INV-TRANS-001: 调拨单创建测试")
    void testCreateTransferOrder() {
        Map<String, Object> transfer = new HashMap<>();
        transfer.put("fromWarehouse", "W001");
        transfer.put("toWarehouse", "W002");
        transfer.put("operator", "孙七");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(transfer).when().post("/api/v1/inventory/transfer").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        transferId = response.jsonPath().getString("data.id");
    }

    @Test
    @Order(41)
    @DisplayName("INV-TRANS-002: 调拨出库测试")
    void testTransferOut() {
        Map<String, Object> items = new HashMap<>();
        items.put("transferId", transferId);
        items.put("productId", "P001");
        items.put("quantity", 25);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(items).when().post("/api/v1/inventory/transfer/out").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(42)
    @DisplayName("INV-TRANS-003: 调拨入库测试")
    void testTransferIn() {
        Map<String, Object> items = new HashMap<>();
        items.put("transferId", transferId);
        items.put("productId", "P001");
        items.put("quantity", 25);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(items).when().post("/api/v1/inventory/transfer/in").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(43)
    @DisplayName("INV-TRANS-004: 调拨状态跟踪测试")
    void testTransferStatusTracking() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).pathParam("transferId", transferId).when().get("/api/v1/inventory/transfer/{transferId}/status").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }
}
