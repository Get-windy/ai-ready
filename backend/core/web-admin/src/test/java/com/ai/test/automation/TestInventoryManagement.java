package com.ai.test.automation;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * 库存管理模块自动化测试
 * 任务: task_1776090578468_b4c9v7ypf
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestInventoryManagement {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long inboundOrderId;
    private static Long outboundOrderId;
    private static Long stockCheckId;
    private static final String BASE_URL = "/api/inventory";

    @Test
    @Order(1)
    @DisplayName("INV-001: 库存列表查询")
    public void testQueryInventoryList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("INV-002: 按仓库筛选库存")
    public void testQueryByWarehouse() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stock").param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(3)
    @DisplayName("INV-006: 库存预警查询")
    public void testQueryStockAlert() throws Exception {
        mockMvc.perform(get(BASE_URL + "/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("INV-010: 入库单创建")
    public void testCreateInboundOrder() throws Exception {
        ObjectNode inboundOrder = objectMapper.createObjectNode();
        inboundOrder.put("orderType", "PURCHASE");
        inboundOrder.put("warehouseId", 1);
        inboundOrder.put("supplierId", 1);

        ArrayNode items = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("productId", 1);
        item.put("quantity", 100);
        items.add(item);
        inboundOrder.set("items", items);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/inbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inboundOrder.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        inboundOrderId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(5)
    @DisplayName("INV-012: 入库审核通过")
    public void testApproveInboundOrder() throws Exception {
        ObjectNode approveData = objectMapper.createObjectNode();
        approveData.put("approved", true);
        approveData.put("remark", "审核通过");

        mockMvc.perform(post(BASE_URL + "/inbound/" + inboundOrderId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveData.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @Order(6)
    @DisplayName("INV-016: 出库单创建")
    public void testCreateOutboundOrder() throws Exception {
        ObjectNode outboundOrder = objectMapper.createObjectNode();
        outboundOrder.put("orderType", "SALE");
        outboundOrder.put("warehouseId", 1);
        outboundOrder.put("customerId", 1);

        ArrayNode items = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("productId", 1);
        item.put("quantity", 20);
        items.add(item);
        outboundOrder.set("items", items);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(outboundOrder.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        outboundOrderId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(7)
    @DisplayName("INV-017: 出库单库存不足验证")
    public void testOutboundInsufficientStock() throws Exception {
        ObjectNode outboundOrder = objectMapper.createObjectNode();
        outboundOrder.put("orderType", "SALE");
        outboundOrder.put("warehouseId", 1);

        ArrayNode items = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("productId", 1);
        item.put("quantity", 999999);
        items.add(item);
        outboundOrder.set("items", items);

        mockMvc.perform(post(BASE_URL + "/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(outboundOrder.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(8)
    @DisplayName("INV-018: 出库审核通过")
    public void testApproveOutboundOrder() throws Exception {
        ObjectNode approveData = objectMapper.createObjectNode();
        approveData.put("approved", true);
        approveData.put("remark", "审核通过");

        mockMvc.perform(post(BASE_URL + "/outbound/" + outboundOrderId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveData.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @Order(9)
    @DisplayName("INV-023: 盘点任务创建")
    public void testCreateStockCheck() throws Exception {
        ObjectNode stockCheck = objectMapper.createObjectNode();
        stockCheck.put("checkType", "FULL");
        stockCheck.put("warehouseId", 1);
        stockCheck.put("planDate", "2026-04-15");

        MvcResult result = mockMvc.perform(post(BASE_URL + "/stock-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockCheck.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        stockCheckId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(10)
    @DisplayName("INV-026: 盘点差异处理-盘盈")
    public void testStockCheckSurplus() throws Exception {
        ObjectNode checkResult = objectMapper.createObjectNode();
        checkResult.put("productId", 1);
        checkResult.put("systemQuantity", 100);
        checkResult.put("actualQuantity", 105);
        checkResult.put("difference", 5);

        mockMvc.perform(post(BASE_URL + "/stock-check/" + stockCheckId + "/results")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkResult.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(11)
    @DisplayName("INV-027: 盘点差异处理-盘亏")
    public void testStockCheckDeficit() throws Exception {
        ObjectNode checkResult = objectMapper.createObjectNode();
        checkResult.put("productId", 2);
        checkResult.put("systemQuantity", 100);
        checkResult.put("actualQuantity", 95);
        checkResult.put("difference", -5);

        mockMvc.perform(post(BASE_URL + "/stock-check/" + stockCheckId + "/results")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkResult.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(12)
    @DisplayName("INV-028: 盘点差异审核")
    public void testApproveStockCheck() throws Exception {
        ObjectNode approveData = objectMapper.createObjectNode();
        approveData.put("approved", true);
        approveData.put("remark", "差异审核通过");

        mockMvc.perform(post(BASE_URL + "/stock-check/" + stockCheckId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(approveData.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
