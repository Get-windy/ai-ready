package com.qizhilian.api.erp;

import com.qizhilian.api.base.BaseApiTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * ERP采购模块接口测试
 */
@Feature("ERP模块")
@Story("采购管理API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PurchaseApiTest extends BaseApiTest {
    
    private static Long supplierId;
    private static Long purchaseOrderId;
    private static String purchaseOrderNo;
    
    @BeforeAll
    static void setup() {
        // 确保已登录
        if (authToken == null) {
            // 登录获取token
        }
    }
    
    // ==================== 供应商管理 ====================
    
    @Test
    @Order(1)
    @DisplayName("创建供应商 - 正向测试")
    @Description("创建新的供应商信息")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateSupplier() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", dataGenerator.generateCompanyName());
        requestBody.put("code", "SUP" + System.currentTimeMillis());
        requestBody.put("contactName", dataGenerator.generateName());
        requestBody.put("contactPhone", dataGenerator.generatePhone());
        requestBody.put("email", dataGenerator.generateEmail());
        requestBody.put("address", dataGenerator.generateAddress());
        requestBody.put("creditCode", dataGenerator.generateUnifiedCreditCode());
        requestBody.put("status", "active");
        
        Response response = post("/erp/suppliers", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.supplierId", notNullValue())
            .body("data.name", equalTo(requestBody.get("name")));
        
        supplierId = response.jsonPath().getLong("data.supplierId");
    }
    
    @Test
    @Order(2)
    @DisplayName("获取供应商列表")
    @Description("分页获取供应商列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetSupplierList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        params.put("status", "active");
        
        Response response = get("/erp/suppliers", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue())
            .body("data.total", greaterThanOrEqualTo(0));
    }
    
    @Test
    @Order(3)
    @DisplayName("获取供应商详情")
    @Description("根据ID获取供应商详细信息")
    @Severity(SeverityLevel.NORMAL)
    void testGetSupplierDetail() {
        if (supplierId == null) {
            testCreateSupplier();
        }
        
        Response response = get("/erp/suppliers/" + supplierId);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.supplierId", equalTo(supplierId.intValue()));
    }
    
    @Test
    @Order(4)
    @DisplayName("更新供应商信息")
    @Description("更新供应商的联系方式等信息")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateSupplier() {
        if (supplierId == null) {
            testCreateSupplier();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contactName", dataGenerator.generateName());
        requestBody.put("contactPhone", dataGenerator.generatePhone());
        
        Response response = put("/erp/suppliers/" + supplierId, requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
    }
    
    // ==================== 采购订单管理 ====================
    
    @Test
    @Order(10)
    @DisplayName("创建采购订单 - 正向测试")
    @Description("创建新的采购订单")
    @Severity(SeverityLevel.CRITICAL)
    void testCreatePurchaseOrder() {
        if (supplierId == null) {
            testCreateSupplier();
        }
        
        purchaseOrderNo = "PO" + System.currentTimeMillis();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orderNo", purchaseOrderNo);
        requestBody.put("supplierId", supplierId);
        requestBody.put("orderDate", dataGenerator.generateCurrentTime());
        requestBody.put("deliveryDate", dataGenerator.generateDateTime(7));
        requestBody.put("remark", "测试采购订单");
        requestBody.put("status", "draft");
        
        // 订单明细
        java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("productName", dataGenerator.generateProductName());
        item1.put("sku", dataGenerator.generateSku());
        item1.put("quantity", dataGenerator.generateQuantity(1, 100));
        item1.put("unitPrice", dataGenerator.generatePrice());
        items.add(item1);
        
        requestBody.put("items", items);
        
        Response response = post("/erp/purchase-orders", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.orderId", notNullValue())
            .body("data.orderNo", equalTo(purchaseOrderNo));
        
        purchaseOrderId = response.jsonPath().getLong("data.orderId");
    }
    
    @Test
    @Order(11)
    @DisplayName("获取采购订单列表")
    @Description("分页查询采购订单")
    @Severity(SeverityLevel.NORMAL)
    void testGetPurchaseOrderList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        params.put("status", "draft");
        
        Response response = get("/erp/purchase-orders", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue())
            .body("data.total", greaterThanOrEqualTo(0));
    }
    
    @Test
    @Order(12)
    @DisplayName("获取采购订单详情")
    @Description("根据ID获取采购订单详情")
    @Severity(SeverityLevel.NORMAL)
    void testGetPurchaseOrderDetail() {
        if (purchaseOrderId == null) {
            testCreatePurchaseOrder();
        }
        
        Response response = get("/erp/purchase-orders/" + purchaseOrderId);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.orderId", equalTo(purchaseOrderId.intValue()))
            .body("data.items", notNullValue());
    }
    
    @Test
    @Order(13)
    @DisplayName("提交采购订单")
    @Description("将草稿状态的采购订单提交审核")
    @Severity(SeverityLevel.CRITICAL)
    void testSubmitPurchaseOrder() {
        if (purchaseOrderId == null) {
            testCreatePurchaseOrder();
        }
        
        Response response = post("/erp/purchase-orders/" + purchaseOrderId + "/submit", null);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.status", equalTo("pending"));
    }
    
    @Test
    @Order(14)
    @DisplayName("审批采购订单")
    @Description("审批通过的采购订单")
    @Severity(SeverityLevel.CRITICAL)
    void testApprovePurchaseOrder() {
        if (purchaseOrderId == null) {
            testCreatePurchaseOrder();
            testSubmitPurchaseOrder();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("approved", true);
        requestBody.put("remark", "审批通过");
        