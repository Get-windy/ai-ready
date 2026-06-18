package com.qizhilian.api.inventory;

import com.qizhilian.api.base.ApiBaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存管理API测试类
 * 覆盖入库、出库、盘点、预警等核心接口
 */
@Feature("库存管理")
@Epic("企智连API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryApiTest extends ApiBaseTest {
    
    private static Long warehouseId;
    private static Long materialId;
    private static Long inventoryId;
    
    @BeforeAll
    public static void setup() {
        // 初始化测试数据
        warehouseId = 1L;
        materialId = 1L;
    }
    
    @Test
    @Order(1)
    @Story("入库管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建入库单成功")
    void testCreateInboundOrder() {
        Map<String, Object> inboundBody = new HashMap<>();
        inboundBody.put("warehouseId", warehouseId);
        inboundBody.put("supplierId", 1);
        inboundBody.put("inboundType", "PURCHASE");
        inboundBody.put("remark", "采购入库测试");
        
        Map<String, Object> item = new HashMap<>();
        item.put("materialId", materialId);
        item.put("quantity", 100);
        item.put("batchNo", "BATCH_" + System.currentTimeMillis());
        item.put("unitPrice", 50.00);
        
        inboundBody.put("items", new Map[]{item});
        
        Response response = post("/inventory/inbound", inboundBody);
        
        assertSuccess(response);
        inventoryId = response.jsonPath().getLong("data.id");
        assertNotNull(inventoryId, "入库单ID不应为空");
    }
    
    @Test
    @Order(2)
    @Story("入库管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("入库单审核")
    void testApproveInboundOrder() {
        Assumptions.assumeTrue(inventoryId != null, "需要先创建入库单");
        
        Map<String, Object> approveBody = new HashMap<>();
        approveBody.put("status", "APPROVED");
        approveBody.put("remark", "审核通过");
        
        Response response = put("/inventory/inbound/" + inventoryId + "/approve", approveBody);
        
        assertSuccess(response);
    }
    
    @Test
    @Order(3)
    @Story("出库管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建出库单成功")
    void testCreateOutboundOrder() {
        Map<String, Object> outboundBody = new HashMap<>();
        outboundBody.put("warehouseId", warehouseId);
        outboundBody.put("customerId", 1);
        outboundBody.put("outboundType", "SALE");
        outboundBody.put("remark", "销售出库测试");
        
        Map<String, Object> item = new HashMap<>();
        item.put("materialId", materialId);
        item.put("quantity", 10);
        item.put("batchNo", "BATCH_" + System.currentTimeMillis());
        
        outboundBody.put("items", new Map[]{item});
        
        Response response = post("/inventory/outbound", outboundBody);
        
        assertSuccess(response);
        Long outboundId = response.jsonPath().getLong("data.id");
        assertNotNull(outboundId, "出库单ID不应为空");
    }
    
    @Test
    @Order(4)
    @Story("库存查询")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询库存列表")
    void testGetInventoryList() {
        Response response = get("/inventory?page=1&size=20");
        
        assertSuccess(response);
        assertFieldExists(response, "data.list");
        assertFieldExists(response, "data.total");
    }
    
    @Test
    @Order(5)
    @Story("库存查询")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询指定物料库存")
    void testGetInventoryByMaterial() {
        Response response = get("/inventory/material/" + materialId);
        
        assertSuccess(response);
        assertFieldExists(response, "data.quantity");
        assertFieldExists(response, "data.warehouseName");
    }
    
    @Test
    @Order(6)
    @Story("库存盘点")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建盘点单")
    void testCreateStockCheck() {
        Map<String, Object> checkBody = new HashMap<>();
        checkBody.put("warehouseId", warehouseId);
        checkBody.put("checkType", "FULL");
        checkBody.put("planDate", "2024-12-31");
        checkBody.put("remark", "年终盘点");
        
        Response response = post("/inventory/stock-check", checkBody);
        
        assertSuccess(response);
        Long checkId = response.jsonPath().getLong("data.id");
        assertNotNull(checkId, "盘点单ID不应为空");
    }
    
    @Test
    @Order(7)
    @Story("库存预警")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询库存预警列表")
    void testGetInventoryAlerts() {
        Response response = get("/inventory/alerts");
        
        assertSuccess(response);
        assertFieldExists(response, "data.list");
    }
    
    @Test
    @Order(8)
    @Story("库存预警")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("设置库存预警阈值")
    void testSetInventoryAlertThreshold() {
        Map<String, Object> thresholdBody = new HashMap<>();
        thresholdBody.put("materialId", materialId);
        thresholdBody.put("warehouseId", warehouseId);
        thresholdBody.put("minStock", 20);
        thresholdBody.put("maxStock", 500);
        
        Response response = post("/inventory/alert-threshold", thresholdBody);
        
        assertSuccess(response);
    }
    
    @Test
    @Order(9)
    @Story("库存调拨")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("创建库存调拨单")
    void testCreateTransferOrder() {
        Map<String, Object> transferBody = new HashMap<>();
        transferBody.put("fromWarehouseId", warehouseId);
        transferBody.put("toWarehouseId", 2);
        transferBody.put("remark", "仓库间调拨");
        
        Map<String, Object> item = new HashMap<>();
        item.put("materialId", materialId);
        item.put("quantity", 20);
        
        transferBody.put("items", new Map[]{item});
        
        Response response = post("/inventory/transfer", transferBody);
        
        assertSuccess(response);
        Long transferId = response.jsonPath().getLong("data.id");
        assertNotNull(transferId, "调拨单ID不应为空");
    }
    
    @Test
    @Order(10)
    @Story("库存报表")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("查询库存流水")
    void testGetInventoryFlow() {
        Response response = get("/inventory/flow?materialId=" + materialId + "&startDate=2024-01-01&endDate=2024-12-31");
        
        assertSuccess(response);
        assertFieldExists(response, "data.list");
    }
    
    @Test
    @Order(11)
    @Story("库存报表")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("库存汇总统计")
    void testGetInventorySummary() {
        Response response = get("/inventory/summary");
        
        assertSuccess(response);
        assertFieldExists(response, "data.totalQuantity");
        assertFieldExists(response, "data.totalValue");
    }
}
