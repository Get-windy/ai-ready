package com.ai.test.automation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
 * 往来单位管理模块自动化测试
 * 任务: task_1776058469824_m232ghev3
 * 
 * 测试覆盖:
 * 1. 往来单位创建（客户/供应商/物流商/配套商）
 * 2. 往来单位统一查询（带业务类型过滤）
 * 3. 往来单位信息修改
 * 4. 往来单位状态变更
 * 5. 往来单位批量导入/导出
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestWarehouseUnitManagement {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdUnitId;
    private static final String BASE_URL = "/api/warehouse-unit";

    /**
     * 测试数据准备
     */
    private ObjectNode createUnitData(String unitName, String businessType, 
                                       String contactPerson, String contactPhone) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("unitName", unitName);
        data.put("businessType", businessType);
        data.put("contactPerson", contactPerson);
        data.put("contactPhone", contactPhone);
        data.put("address", "北京市朝阳区测试路" + System.currentTimeMillis() % 1000 + "号");
        data.put("status", "ACTIVE");
        return data;
    }

    // ==================== 创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("WUM-001: 创建客户类型往来单位-正向")
    public void testCreateCustomerUnit() throws Exception {
        ObjectNode unitData = createUnitData(
            "测试客户公司-" + System.currentTimeMillis(),
            "客户",
            "张三",
            "13800138000"
        );
        unitData.put("creditLevel", "A");
        unitData.put("paymentTerms", "月结30天");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.unitName").value(unitData.get("unitName").asText()))
                .andExpect(jsonPath("$.data.businessType").value("客户"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
        createdUnitId = responseJson.get("data").get("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("WUM-002: 创建供应商类型往来单位-正向")
    public void testCreateSupplierUnit() throws Exception {
        ObjectNode unitData = createUnitData(
            "测试供应商公司-" + System.currentTimeMillis(),
            "供应商",
            "李四",
            "13900139000"
        );
        unitData.put("taxNumber", "91110000123456789X");
        unitData.put("bankName", "中国工商银行");
        unitData.put("bankAccount", "6222021234567890123");

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.businessType").value("供应商"));
    }

    @Test
    @Order(3)
    @DisplayName("WUM-003: 创建物流商类型往来单位-正向")
    public void testCreateLogisticsUnit() throws Exception {
        ObjectNode unitData = createUnitData(
            "测试物流公司-" + System.currentTimeMillis(),
            "物流商",
            "王五",
            "13700137000"
        );
        
        ArrayNode logisticsScope = objectMapper.createArrayNode();
        logisticsScope.add("北京");
        logisticsScope.add("上海");
        logisticsScope.add("广州");
        unitData.set("logisticsScope", logisticsScope);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.businessType").value("物流商"));
    }

    @Test
    @Order(4)
    @DisplayName("WUM-004: 创建配套商类型往来单位-正向")
    public void testCreateAccessoryUnit() throws Exception {
        ObjectNode unitData = createUnitData(
            "测试配套商公司-" + System.currentTimeMillis(),
            "配套商",
            "赵六",
            "13600136000"
        );
        
        ArrayNode productTypes = objectMapper.createArrayNode();
        productTypes.add("包装材料");
        productTypes.add("标签");
        unitData.set("productTypes", productTypes);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.businessType").value("配套商"));
    }

    @Test
    @Order(5)
    @DisplayName("WUM-005: 创建往来单位-单位名称为空-反向")
    public void testCreateUnitWithEmptyName() throws Exception {
        ObjectNode unitData = createUnitData("", "客户", "张三", "13800138000");

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(6)
    @DisplayName("WUM-006: 创建往来单位-单位名称重复-反向")
    public void testCreateUnitWithDuplicateName() throws Exception {
        String unitName = "重复测试公司-" + System.currentTimeMillis();
        ObjectNode unitData = createUnitData(unitName, "客户", "张三", "13800138000");
        
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(unitData.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    // ==================== 查询测试 ====================

    @Test
    @Order(7)
    @DisplayName("WUM-007: 统一查询往来单位-不带过滤")
    public void testQueryAllUnits() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("WUM-008: 统一查询往来单位-按客户类型过滤")
    public void testQueryByCustomerType() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .param("page", "1")
                .param("size", "10")
                .param("businessType", "客户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[*].businessType").value(everyItem(equalTo("客户"))));
    }

    @ParameterizedTest
    @CsvSource({"客户", "供应商", "物流商", "配套商"})
    @Order(9)
    @DisplayName("WUM-009: 统一查询往来单位-按各种业务类型过滤")
    public void testQueryByVariousTypes(String businessType) throws Exception {
        mockMvc.perform(get(BASE_URL)
                .param("page", "1")
                .param("size", "10")
                .param("businessType", businessType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(10)
    @DisplayName("WUM-010: 修改往来单位信息-正向")
    public void testUpdateUnit() throws Exception {
        ObjectNode updateData = objectMapper.createObjectNode();
        updateData.put("id", createdUnitId);
        updateData.put("contactPerson", "张三丰");
        updateData.put("contactPhone", "13800138111");

        mockMvc.perform(put(BASE_URL + "/" + createdUnitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateData.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.contactPerson").value("张三丰"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "DISABLED", "SUSPENDED"})
    @Order(11)
    @DisplayName("WUM-011: 变更往来单位状态")
    public void testChangeStatus(String status) throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + createdUnitId + "/status")
                .param("status", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(status));
    }

    @Test
    @Order(12)
    @DisplayName("WUM-012: 批量导入往来单位")
    public void testBatchImport() throws Exception {
        ArrayNode importData = objectMapper.createArrayNode();
        for (int i = 0; i < 5; i++) {
            ObjectNode unit = createUnitData(
                "批量导入公司" + i + "-" + System.currentTimeMillis(),
                "客户",
                "联系人" + i,
                "1380000000" + i
            );
            importData.add(unit);
        }

        mockMvc.perform(post(BASE_URL + "/batch-import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(importData.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successCount").value(5));
    }

    @Test
    @Order(13)
    @DisplayName("WUM-013: 导出往来单位")
    public void testExport() throws Exception {
        mockMvc.perform(get(BASE_URL + "/export")
                .param("businessType", "客户"))
                .andExpect(status().isOk());
    }
}
