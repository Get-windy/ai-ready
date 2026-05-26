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

import java.math.BigDecimal;

/**
 * 客户等级价格模块自动化测试
 * 任务: task_1776058470114_pyg6f78e4
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCustomerPriceLevel {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long vipLevelId;
    private static Long productId;
    private static final String BASE_URL = "/api/price-level";

    private ObjectNode createLevelData(String name, String code, double discount) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("levelName", name);
        data.put("levelCode", code);
        data.put("discountRate", discount);
        data.put("status", "ACTIVE");
        return data;
    }

    @Test
    @Order(1)
    @DisplayName("CPL-001: 创建VIP客户等级")
    public void testCreateVIPLevel() throws Exception {
        ObjectNode levelData = createLevelData("VIP客户", "VIP", 0.85);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(levelData.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.levelName").value("VIP客户"))
                .andExpect(jsonPath("$.data.discountRate").value(0.85))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        vipLevelId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("CPL-002: 创建多个客户等级")
    public void testCreateMultipleLevels() throws Exception {
        // 创建金牌客户
        ObjectNode goldLevel = createLevelData("金牌客户", "GOLD", 0.90);
        mockMvc.perform(post(BASE_URL + "/levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(goldLevel.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discountRate").value(0.90));

        // 创建银牌客户
        ObjectNode silverLevel = createLevelData("银牌客户", "SILVER", 0.95);
        mockMvc.perform(post(BASE_URL + "/levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(silverLevel.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discountRate").value(0.95));
    }

    @Test
    @Order(3)
    @DisplayName("CPL-003: 查询客户等级列表")
    public void testQueryLevels() throws Exception {
        mockMvc.perform(get(BASE_URL + "/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list.length()").value(greaterThanOrEqualTo(3)));
    }

    @Test
    @Order(4)
    @DisplayName("CPL-004: 设置产品等级价格")
    public void testSetProductLevelPrice() throws Exception {
        // 创建测试产品
        ObjectNode product = objectMapper.createObjectNode();
        product.put("productName", "测试产品A");
        product.put("standardPrice", 100.00);

        MvcResult productResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(product.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        ObjectNode productJson = (ObjectNode) objectMapper.readTree(productResponse);
        productId = productJson.get("data").get("id").asLong();

        // 设置VIP价格
        ObjectNode vipPrice = objectMapper.createObjectNode();
        vipPrice.put("productId", productId);
        vipPrice.put("levelId", vipLevelId);
        vipPrice.put("price", 85.00);

        mockMvc.perform(post(BASE_URL + "/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vipPrice.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.price").value(85.00));
    }

    @Test
    @Order(5)
    @DisplayName("CPL-005: 查询产品等级价格")
    public void testQueryProductPrice() throws Exception {
        mockMvc.perform(get(BASE_URL + "/prices/product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.standardPrice").value(100.00))
                .andExpect(jsonPath("$.data.levelPrices").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("CPL-006: 下单时应用等级价格")
    public void testApplyLevelPrice() throws Exception {
        ObjectNode orderRequest = objectMapper.createObjectNode();
        orderRequest.put("customerId", 1);
        orderRequest.put("customerLevelId", vipLevelId);

        ArrayNode items = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("productId", productId);
        item.put("quantity", 2);
        items.add(item);
        orderRequest.set("items", items);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderRequest.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalAmount").value(170.00)); // 85 * 2
    }

    @Test
    @Order(7)
    @DisplayName("CPL-007: 不同等级价格对比")
    public void testPriceComparison() throws Exception {
        mockMvc.perform(get(BASE_URL + "/comparison")
                .param("productIds", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.comparisons").isArray())
                .andExpect(jsonPath("$.data.comparisons[0].standardPrice").value(100.00))
                .andExpect(jsonPath("$.data.comparisons[0].vipPrice").value(85.00));
    }

    @Test
    @Order(8)
    @DisplayName("CPL-008: 更新等级价格")
    public void testUpdatePrice() throws Exception {
        ObjectNode updateData = objectMapper.createObjectNode();
        updateData.put("productId", productId);
        updateData.put("levelId", vipLevelId);
        updateData.put("price", 80.00);

        mockMvc.perform(put(BASE_URL + "/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateData.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.price").value(80.00));
    }
}
