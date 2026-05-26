package cn.aiedge.erp.batchsn.controller;

import cn.aiedge.erp.batchsn.entity.SerialNumber;
import cn.aiedge.erp.batchsn.service.SerialNumberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 序列号管理控制器单元测试
 *
 * @author team-member
 * @date 2026-05-05
 */
@WebMvcTest(SerialNumberController.class)
@DisplayName("序列号管理控制器测试")
class SerialNumberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SerialNumberService serialNumberService;

    private SerialNumber mockSerial;

    @BeforeEach
    void setUp() {
        // 创建模拟序列号对象
        mockSerial = new SerialNumber();
        mockSerial.setId(1L);
        mockSerial.setSerialNo("SN202405050001");
        mockSerial.setProductId(1001L);
        mockSerial.setProductCode("PROD-001");
        mockSerial.setProductName("测试产品");
        mockSerial.setSpecification("标准规格");
        mockSerial.setUnit("台");
        mockSerial.setBatchNo("BATCH-001");
        mockSerial.setSerialStatus("AVAILABLE");
        mockSerial.setStage("WAREHOUSE");
        mockSerial.setTotalQuantity(BigDecimal.ONE);
        mockSerial.setAvailableQuantity(BigDecimal.ONE);
        mockSerial.setWarehouseId(101L);
        mockSerial.setWarehouseName("北京仓库");
        mockSerial.setLocationId(201L);
        mockSerial.setQualityStatus("PASSED");
        mockSerial.setWarrantyStartDate(LocalDate.now());
        mockSerial.setWarrantyEndDate(LocalDate.now().plusYears(2));
        mockSerial.setCreatedByName("测试用户");
        mockSerial.setCreatedAt(LocalDateTime.now());
        mockSerial.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("基础CRUD操作测试")
    class BasicCrudTests {

        @Test
        @DisplayName("创建序列号 - 成功")
        void createSerial_success() throws Exception {
            // 模拟服务层返回
            Mockito.when(serialNumberService.createSerial(any(SerialNumber.class)))
                    .thenReturn(mockSerial);

            // 执行请求
            mockMvc.perform(post("/api/erp/batch-sn/serials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mockSerial)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.serialNo").value("SN202405050001"))
                    .andExpect(jsonPath("$.productCode").value("PROD-001"))
                    .andExpect(jsonPath("$.serialStatus").value("AVAILABLE"));
        }

        @Test
        @DisplayName("创建序列号 - 无效参数")
        void createSerial_invalidParameters() throws Exception {
            // 创建无效的序列号对象
            SerialNumber invalidSerial = new SerialNumber();
            invalidSerial.setSerialNo(null); // 必填字段为空

            mockMvc.perform(post("/api/erp/batch-sn/serials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidSerial)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("查询序列号详情 - 成功")
        void getSerial_success() throws Exception {
            Mockito.when(serialNumberService.getSerialById(1L))
                    .thenReturn(mockSerial);

            mockMvc.perform(get("/api/erp/batch-sn/serials/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.serialNo").value("SN202405050001"))
                    .andExpect(jsonPath("$.warehouseName").value("北京仓库"));
        }

        @Test
        @DisplayName("查询序列号详情 - 不存在")
        void getSerial_notFound() throws Exception {
            Mockito.when(serialNumberService.getSerialById(999L))
                    .thenReturn(null);

            mockMvc.perform(get("/api/erp/batch-sn/serials/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("查询序列号列表 - 成功")
        void listSerials_success() throws Exception {
            List<SerialNumber> serials = Arrays.asList(mockSerial);
            Mockito.when(serialNumberService.listSerials(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                    .thenReturn(serials);

            mockMvc.perform(get("/api/erp/batch-sn/serials")
                            .param("serialNo", "SN2024")
                            .param("productCode", "PROD-001")
                            .param("status", "AVAILABLE")
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].serialNo").value("SN202405050001"))
                    .andExpect(jsonPath("$[0].productCode").value("PROD-001"));
        }

        @Test
        @DisplayName("更新序列号信息 - 成功")
        void updateSerial_success() throws Exception {
            SerialNumber updatedSerial = mockSerial;
            updatedSerial.setWarehouseName("上海仓库");
            updatedSerial.setQualityStatus("PASSED");

            Mockito.when(serialNumberService.updateSerial(eq(1L), any(SerialNumber.class)))
                    .thenReturn(updatedSerial);

            mockMvc.perform(put("/api/erp/batch-sn/serials/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedSerial)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseName").value("上海仓库"))
                    .andExpect(jsonPath("$.qualityStatus").value("PASSED"));
        }
    }

    @Nested
    @DisplayName("业务操作测试")
    class BusinessOperationTests {

        @Test
        @DisplayName("序列号入库 - 成功")
        void inbound_success() throws Exception {
            SerialNumber inboundSerial = mockSerial;
            inboundSerial.setWarehouseName("广州仓库");
            inboundSerial.setStage("WAREHOUSE");

            Mockito.when(serialNumberService.inbound(any(SerialNumber.class), anyLong(), anyString(), anyLong()))
                    .thenReturn(inboundSerial);

            mockMvc.perform(post("/api/erp/batch-sn/serials/inbound")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inboundSerial))
                            .param("warehouseId", "102")
                            .param("warehouseName", "广州仓库")
                            .param("locationId", "202"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.warehouseName").value("广州仓库"))
                    .andExpect(jsonPath("$.stage").value("WAREHOUSE"));
        }

        @Test
        @DisplayName("序列号出库 - 成功")
        void outbound_success() throws Exception {
            SerialNumber outboundSerial = mockSerial;
            outboundSerial.setStage("SOLD");

            Mockito.when(serialNumberService.outbound(anyLong(), anyLong(), anyString(), anyLong(), anyLong()))
                    .thenReturn(outboundSerial);

            mockMvc.perform(post("/api/erp/batch-sn/serials/outbound")
                            .param("serialId", "1")
                            .param("saleOrderId", "5001")
                            .param("saleOrderNo", "SO202405050001")
                            .param("warehouseId", "101")
                            .param("locationId", "201"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stage").value("SOLD"));
        }

        @Test
        @DisplayName("更新序列号状态 - 成功")
        void updateStatus_success() throws Exception {
            SerialNumber statusUpdatedSerial = mockSerial;
            statusUpdatedSerial.setSerialStatus("MAINTAINED");
            statusUpdatedSerial.setStage("MAINTENANCE");

            Mockito.when(serialNumberService.updateStatus(eq(1L), eq("MAINTAINED"), eq("MAINTENANCE")))
                    .thenReturn(statusUpdatedSerial);

            mockMvc.perform(patch("/api/erp/batch-sn/serials/1/status")
                            .param("status", "MAINTAINED")
                            .param("stage", "MAINTENANCE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serialStatus").value("MAINTAINED"))
                    .andExpect(jsonPath("$.stage").value("MAINTENANCE"));
        }

        @Test
        @DisplayName("更新序列号状态 - 无效状态")
        void updateStatus_invalidStatus() throws Exception {
            // 模拟服务层抛出异常
            Mockito.when(serialNumberService.updateStatus(eq(1L), eq("INVALID_STATUS"), eq("INVALID_STAGE")))
                    .thenThrow(new IllegalArgumentException("无效的状态值"));

            mockMvc.perform(patch("/api/erp/batch-sn/serials/1/status")
                            .param("status", "INVALID_STATUS")
                            .param("stage", "INVALID_STAGE"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("查询与验证测试")
    class QueryAndValidationTests {

        @Test
        @DisplayName("查询质保即将到期序列号 - 成功")
        void getWarrantyExpiring_success() throws Exception {
            List<SerialNumber> serials = Arrays.asList(mockSerial);
            Mockito.when(serialNumberService.getWarrantyExpiring(30))
                    .thenReturn(serials);

            mockMvc.perform(get("/api/erp/batch-sn/serials/warranty-warning")
                            .param("warningDays", "30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].serialNo").value("SN202405050001"))
                    .andExpect(jsonPath("$[0].warrantyEndDate").exists());
        }

        @Test
        @DisplayName("查询序列号完整流转历史 - 成功")
        void getFullHistory_success() throws Exception {
            List<SerialNumber> history = Arrays.asList(mockSerial);
            Mockito.when(serialNumberService.getFullHistory(1L))
                    .thenReturn(history);

            mockMvc.perform(get("/api/erp/batch-sn/serials/1/full-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].serialNo").value("SN202405050001"))
                    .andExpect(jsonPath("$[0].stage").value("WAREHOUSE"));
        }

        @Test
        @DisplayName("验证序列号唯一性 - 不存在")
        void validateSerialNo_notExists() throws Exception {
            Mockito.when(serialNumberService.serialNoExists("NEW-SN-001"))
                    .thenReturn(false);

            mockMvc.perform(get("/api/erp/batch-sn/serials/validate-serial-no")
                            .param("serialNo", "NEW-SN-001"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("验证序列号唯一性 - 已存在")
        void validateSerialNo_exists() throws Exception {
            Mockito.when(serialNumberService.serialNoExists("EXISTING-SN"))
                    .thenReturn(true);

            mockMvc.perform(get("/api/erp/batch-sn/serials/validate-serial-no")
                            .param("serialNo", "EXISTING-SN"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("验证序列号唯一性 - 空序列号")
        void validateSerialNo_empty() throws Exception {
            mockMvc.perform(get("/api/erp/batch-sn/serials/validate-serial-no")
                            .param("serialNo", ""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("API路径与参数测试")
    class ApiPathAndParameterTests {

        @Test
        @DisplayName("列表查询 - 参数组合")
        void listSerials_parameterCombinations() throws Exception {
            List<SerialNumber> serials = Arrays.asList(mockSerial);
            Mockito.when(serialNumberService.listSerials(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                    .thenReturn(serials);

            // 测试不同参数组合
            mockMvc.perform(get("/api/erp/batch-sn/serials")
                            .param("serialNo", "SN")
                            .param("batchNo", "BATCH-001"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/erp/batch-sn/serials")
                            .param("productCode", "PROD-001")
                            .param("status", "AVAILABLE"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/erp/batch-sn/serials")
                            .param("page", "2")
                            .param("size", "50"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("查询质保即将到期 - 默认参数")
        void getWarrantyExpiring_defaultParameter() throws Exception {
            List<SerialNumber> serials = Arrays.asList(mockSerial);
            Mockito.when(serialNumberService.getWarrantyExpiring(30)) // 默认30天
                    .thenReturn(serials);

            mockMvc.perform(get("/api/erp/batch-sn/serials/warranty-warning"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("验证序列号唯一性 - 特殊字符")
        void validateSerialNo_specialCharacters() throws Exception {
            Mockito.when(serialNumberService.serialNoExists("SN-2024-05-05-001"))
                    .thenReturn(false);

            mockMvc.perform(get("/api/erp/batch-sn/serials/validate-serial-no")
                            .param("serialNo", "SN-2024-05-05-001"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("错误与异常测试")
    class ErrorAndExceptionTests {

        @Test
        @DisplayName("无效请求路径 - 404")
        void invalidPath_404() throws Exception {
            mockMvc.perform(get("/api/erp/batch-sn/invalid-path"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("非法JSON格式 - 400")
        void invalidJson_400() throws Exception {
            mockMvc.perform(post("/api/erp/batch-sn/serials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("缺失必需参数 - 400")
        void missingRequiredParameter() throws Exception {
            mockMvc.perform(post("/api/erp/batch-sn/serials/inbound")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mockSerial))
                            .param("warehouseId", "101"))
                    // 缺少warehouseName和locationId参数
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("类型转换错误 - 400")
        void typeConversionError() throws Exception {
            mockMvc.perform(get("/api/erp/batch-sn/serials")
                            .param("page", "invalid-number")
                            .param("size", "not-a-number"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("服务层异常处理")
        void serviceLayerException() throws Exception {
            Mockito.when(serialNumberService.getSerialById(999L))
                    .thenThrow(new RuntimeException("数据库连接失败"));

            mockMvc.perform(get("/api/erp/batch-sn/serials/999"))
                    .andExpect(status().isInternalServerError());
        }
    }
}