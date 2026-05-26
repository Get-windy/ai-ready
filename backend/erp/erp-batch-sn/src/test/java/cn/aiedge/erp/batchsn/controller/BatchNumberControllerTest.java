package cn.aiedge.erp.batchsn.controller;

import cn.aiedge.erp.batchsn.controller.dto.CreateBatchRequest;
import cn.aiedge.erp.batchsn.controller.response.ApiResponse;
import cn.aiedge.erp.batchsn.controller.util.ApiCacheManager;
import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.service.BatchNumberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 批次号控制器单元测试
 * 
 * @author team-member
 * @date 2026-05-05
 */
@WebMvcTest(BatchNumberController.class)
@DisplayName("批次号控制器单元测试")
class BatchNumberControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private BatchNumberService batchNumberService;
    
    @MockBean
    private ApiCacheManager cacheManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private BatchNumber testBatch;
    private CreateBatchRequest createRequest;
    
    @BeforeEach
    void setUp() {
        testBatch = new BatchNumber();
        testBatch.setId(1L);
        testBatch.setBatchNo("B202605050001");
        testBatch.setProductCode("P001");
        testBatch.setProductName("测试产品");
        testBatch.setBatchStatus("ACTIVE");
        testBatch.setTotalQuantity(new BigDecimal("100.00"));
        testBatch.setAvailableQuantity(new BigDecimal("100.00"));
        testBatch.setReservedQuantity(BigDecimal.ZERO);
        testBatch.setProductionDate(LocalDate.now());
        testBatch.setExpirationDate(LocalDate.now().plusYears(1));
        testBatch.setWarehouseId(1L);
        testBatch.setWarehouseName("主仓库");
        
        createRequest = new CreateBatchRequest();
        createRequest.setBatchNo("B202605050001");
        createRequest.setProductCode("P001");
        createRequest.setProductName("测试产品");
        createRequest.setBatchStatus("ACTIVE");
        createRequest.setTotalQuantity(new BigDecimal("100.00"));
        createRequest.setAvailableQuantity(new BigDecimal("100.00"));
        createRequest.setReservedQuantity(BigDecimal.ZERO);
        createRequest.setProductionDate(LocalDate.now());
        createRequest.setExpirationDate(LocalDate.now().plusYears(1));
        createRequest.setWarehouseId(1L);
        createRequest.setWarehouseName("主仓库");
    }
    
    @Test
    @DisplayName("创建批次 - 成功")
    void createBatch_Success() throws Exception {
        // 设置
        when(batchNumberService.createBatch(any(BatchNumber.class))).thenReturn(testBatch);
        when(cacheManager.clearAllListCache()).thenReturn(true);
        
        // 执行 & 验证
        mockMvc.perform(post("/api/erp/batch-sn/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("批次创建成功"))
                .andExpect(jsonPath("$.data.id").value(1L));
        
        verify(batchNumberService, times(1)).createBatch(any(BatchNumber.class));
        verify(cacheManager, times(1)).clearAllListCache();
    }
    
    @Test
    @DisplayName("创建批次 - 参数验证失败")
    void createBatch_ValidationError() throws Exception {
        // 设置：创建无效的请求（缺少必需字段）
        createRequest.setBatchNo(null);
        createRequest.setProductCode(null);
        
        // 执行 & 验证
        mockMvc.perform(post("/api/erp/batch-sn/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        
        verify(batchNumberService, never()).createBatch(any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("查询批次详情 - 成功")
    void getBatch_Success() throws Exception {
        // 设置
        when(cacheManager.getBatchCache(1L)).thenReturn(null);
        when(batchNumberService.getBatchById(1L)).thenReturn(testBatch);
        when(cacheManager.setBatchCache(eq(1L), any(BatchNumber.class))).thenReturn(true);
        
        // 执行 & 验证
        mockMvc.perform(get("/api/erp/batch-sn/batches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.batchNo").value("B202605050001"));
        
        verify(cacheManager, times(1)).getBatchCache(1L);
        verify(batchNumberService, times(1)).getBatchById(1L);
        verify(cacheManager, times(1)).setBatchCache(eq(1L), any(BatchNumber.class));
    }
    
    @Test
    @DisplayName("查询批次详情 - 批次不存在")
    void getBatch_NotFound() throws Exception {
        // 设置
        when(cacheManager.getBatchCache(999L)).thenReturn(null);
        when(batchNumberService.getBatchById(999L)).thenReturn(null);
        
        // 执行 & 验证
        mockMvc.perform(get("/api/erp/batch-sn/batches/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("批次未找到"));
        
        verify(batchNumberService, times(1)).getBatchById(999L);
    }
    
    @Test
    @DisplayName("查询批次详情 - 从缓存获取")
    void getBatch_FromCache() throws Exception {
        // 设置：缓存中有数据
        when(cacheManager.getBatchCache(1L)).thenReturn(testBatch);
        
        // 执行 & 验证
        mockMvc.perform(get("/api/erp/batch-sn/batches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1L));
        
        verify(cacheManager, times(1)).getBatchCache(1L);
        verify(batchNumberService, never()).getBatchById(anyLong());
    }
    
    @Test
    @DisplayName("查询批次列表 - 成功")
    void listBatches_Success() throws Exception {
        // 设置
        List<BatchNumber> batches = Arrays.asList(testBatch);
        when(cacheManager.generateQueryHash(any())).thenReturn("query_hash_123");
        when(cacheManager.getListCache("query_hash_123")).thenReturn(null);
        when(batchNumberService.listBatches(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(batches);
        when(cacheManager.setListCache(eq("query_hash_123"), anyList())).thenReturn(true);
        
        // 执行 & 验证
        mockMvc.perform(get("/api/erp/batch-sn/batches")
                .param("batchNo", "B2026")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1L));
        
        verify(batchNumberService, times(1)).listBatches(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt());
    }
    
    @Test
    @DisplayName("批次出库 - 成功")
    void outbound_Success() throws Exception {
        // 设置
        BatchNumber updatedBatch = testBatch;
        updatedBatch.setTotalQuantity(new BigDecimal("50.00"));
        updatedBatch.setAvailableQuantity(new BigDecimal("50.00"));
        
        when(batchNumberService.outbound(anyLong(), any(), anyLong(), anyLong())).thenReturn(updatedBatch);
        when(cacheManager.evictBatchCache(1L)).thenReturn(true);
        
        // 执行 & 验证
        mockMvc.perform(post("/api/erp/batch-sn/batches/outbound")
                .param("batchId", "1")
                .param("quantity", "50")
                .param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalQuantity").value(50.0));
        
        verify(batchNumberService, times(1)).outbound(1L, new BigDecimal("50.00"), 1L, null);
        verify(cacheManager, times(1)).evictBatchCache(1L);
    }
    
    @Test
    @DisplayName("批次出库 - 数量无效")
    void outbound_InvalidQuantity() throws Exception {
        // 执行 & 验证：数量为0或负数
        mockMvc.perform(post("/api/erp/batch-sn/batches/outbound")
                .param("batchId", "1")
                .param("quantity", "0")
                .param("warehouseId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        
        verify(batchNumberService, never()).outbound(anyLong(), any(), anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("批次质检 - 成功")
    void qualityInspection_Success() throws Exception {
        // 设置
        BatchNumber inspectedBatch = testBatch;
        inspectedBatch.setQualityStatus("PASS");
        inspectedBatch.setQualityInspectorId("insp001");
        inspectedBatch.setQualityInspectorName("质检员张三");
        
        when(batchNumberService.qualityInspection(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(inspectedBatch);
        
        // 执行 & 验证
        mockMvc.perform(post("/api/erp/batch-sn/batches/1/quality-inspection")
                .param("status", "PASS")
                .param("inspectorId", "insp001")
                .param("inspectorName", "质检员张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.qualityStatus").value("PASS"));
        
        verify(batchNumberService, times(1)).qualityInspection(1L, "PASS", "insp001", "质检员张三");
    }
    
    @Test
    @DisplayName("验证批次号唯一性 - 存在")
    void validateBatchNo_Exists() throws Exception {
        // 设置
        when(batchNumberService.batchNoExists("B202605050001")).thenReturn(true);
        
        // 执行 & 验证：批次号存在，应该返回false（表示不唯一）
        mockMvc.perform(get("/api/erp/batch-sn/batches/validate-batch-no")
                .param("batchNo", "B202605050001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
        
        verify(batchNumberService, times(1)).batchNoExists("B202605050001");
    }
    
    @Test
    @DisplayName("验证批次号唯一性 - 不存在")
    void validateBatchNo_NotExists() throws Exception {
        // 设置
        when(batchNumberService.batchNoExists("NEWBATCH001")).thenReturn(false);
        
        // 执行 & 验证：批次号不存在，应该返回true（表示唯一）
        mockMvc.perform(get("/api/erp/batch-sn/batches/validate-batch-no")
                .param("batchNo", "NEWBATCH001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
        
        verify(batchNumberService, times(1)).batchNoExists("NEWBATCH001");
    }
}