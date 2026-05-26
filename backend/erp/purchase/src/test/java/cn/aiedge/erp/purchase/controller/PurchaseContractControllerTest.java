package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.service.PurchaseContractService;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 采购合同控制器测试
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
@WebMvcTest(PurchaseContractController.class)
class PurchaseContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseContractService purchaseContractService;

    private PurchaseContract testContract;

    @BeforeEach
    void setUp() {
        testContract = new PurchaseContract();
        testContract.setId(1L);
        testContract.setContractCode("PC-20260505-001");
        testContract.setTenantId(1L);
        testContract.setSupplierId(100L);
        testContract.setSupplierName("测试供应商有限公司");
        testContract.setOrderId(1L);
        testContract.setOrderCode("PO-20260505-001");
        testContract.setContractAmount(new BigDecimal("50000.00"));
        testContract.setStartDate(LocalDate.now());
        testContract.setEndDate(LocalDate.now().plusYears(1));
        testContract.setStatus(1); // 1-草稿
        testContract.setSignDate(LocalDateTime.now());
        testContract.setRemarks("年度采购框架合同");
    }

    @Test
    @DisplayName("采购合同创建测试")
    void testCreatePurchaseContract() throws Exception {
        // 准备测试数据
        PurchaseContract newContract = new PurchaseContract();
        newContract.setSupplierId(100L);
        newContract.setOrderId(1L);
        newContract.setContractAmount(new BigDecimal("30000.00"));
        newContract.setRemarks("测试创建合同");

        // 模拟服务层返回
        when(purchaseContractService.createContract(any(PurchaseContract.class))).thenReturn(1L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data").value(1L));
    }

    @Test
    @DisplayName("获取采购合同详情测试")
    void testGetPurchaseContractById() throws Exception {
        // 模拟服务层返回
        when(purchaseContractService.getContractById(1L)).thenReturn(testContract);

        // 执行测试
        mockMvc.perform(get("/api/erp/purchase/contract/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.contractCode").value("PC-20260505-001"))
                .andExpect(jsonPath("$.data.supplierName").value("测试供应商有限公司"));
    }

    @Test
    @DisplayName("更新采购合同测试")
    void testUpdatePurchaseContract() throws Exception {
        // 准备更新数据
        PurchaseContract updateContract = new PurchaseContract();
        updateContract.setRemarks("更新后的合同备注");

        // 模拟服务层
        when(purchaseContractService.updateContract(any(PurchaseContract.class))).thenReturn(true);

        // 执行测试
        mockMvc.perform(put("/api/erp/purchase/contract/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    @Test
    @DisplayName("提交合同审批测试")
    void testSubmitContractForApproval() throws Exception {
        // 模拟服务层
        when(purchaseContractService.submitForApproval(1L)).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/submit", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("提交审批成功"));
    }

    @Test
    @DisplayName("审批采购合同测试")
    void testApprovePurchaseContract() throws Exception {
        // 模拟服务层
        when(purchaseContractService.approveContract(1L, "同意签署")).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/approve", 1L)
                .param("comment", "同意签署"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("审批通过"));
    }

    @Test
    @DisplayName("签署采购合同测试")
    void testSignPurchaseContract() throws Exception {
        // 模拟服务层
        when(purchaseContractService.signContract(1L, "张三", "测试公司")).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/sign", 1L)
                .param("signer", "张三")
                .param("company", "测试公司"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("合同签署成功"));
    }

    @Test
    @DisplayName("终止采购合同测试")
    void testTerminatePurchaseContract() throws Exception {
        // 模拟服务层
        when(purchaseContractService.terminateContract(1L, "双方协商一致终止")).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/terminate", 1L)
                .param("reason", "双方协商一致终止"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("合同终止成功"));
    }

    @Test
    @DisplayName("查询采购合同列表测试")
    void testGetPurchaseContractList() throws Exception {
        // 执行测试
        mockMvc.perform(get("/api/erp/purchase/contract")
                .param("page", "1")
                .param("size", "10")
                .param("status", "1")
                .param("supplierId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询合同统计信息测试")
    void testGetContractStatistics() throws Exception {
        // 执行测试
        mockMvc.perform(get("/api/erp/purchase/contract/statistics")
                .param("year", "2026")
                .param("supplierId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("合同续签测试")
    void testRenewPurchaseContract() throws Exception {
        // 准备续签数据
        PurchaseContract renewContract = new PurchaseContract();
        renewContract.setEndDate(LocalDate.now().plusYears(2));

        // 模拟服务层
        when(purchaseContractService.renewContract(1L, any(PurchaseContract.class))).thenReturn(2L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/renew", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renewContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("合同续签成功"));
    }

    @Test
    @DisplayName("合同变更测试")
    void testModifyPurchaseContract() throws Exception {
        // 准备变更数据
        PurchaseContract modifyContract = new PurchaseContract();
        modifyContract.setContractAmount(new BigDecimal("55000.00"));
        modifyContract.setRemarks("合同金额变更");

        // 模拟服务层
        when(purchaseContractService.modifyContract(1L, any(PurchaseContract.class))).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/modify", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifyContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("合同变更成功"));
    }

    @Test
    @DisplayName("异常测试：创建合同时订单不存在")
    void testCreateContractWithInvalidOrder() throws Exception {
        // 准备测试数据
        PurchaseContract invalidContract = new PurchaseContract();
        invalidContract.setOrderId(99999L); // 不存在的订单
        
        // 模拟服务层抛出异常
        when(purchaseContractService.createContract(any(PurchaseContract.class)))
                .thenThrow(new RuntimeException("采购订单不存在"));

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidContract)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("异常测试：签署未审批的合同")
    void testSignUnapprovedContract() throws Exception {
        // 模拟服务层抛出异常
        when(purchaseContractService.signContract(1L, "张三", "测试公司"))
                .thenThrow(new RuntimeException("合同未审批，不能签署"));

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/sign", 1L)
                .param("signer", "张三")
                .param("company", "测试公司"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("边界测试：合同金额为零")
    void testCreateContractWithZeroAmount() throws Exception {
        // 准备测试数据 - 零金额
        PurchaseContract zeroAmountContract = new PurchaseContract();
        zeroAmountContract.setSupplierId(100L);
        zeroAmountContract.setOrderId(1L);
        zeroAmountContract.setContractAmount(BigDecimal.ZERO);
        
        // 模拟服务层
        when(purchaseContractService.createContract(any(PurchaseContract.class))).thenReturn(3L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zeroAmountContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("边界测试：超长合同期限")
    void testCreateContractWithLongTerm() throws Exception {
        // 准备测试数据 - 10年合同
        PurchaseContract longTermContract = new PurchaseContract();
        longTermContract.setSupplierId(100L);
        longTermContract.setOrderId(1L);
        longTermContract.setContractAmount(new BigDecimal("100000.00"));
        longTermContract.setStartDate(LocalDate.now());
        longTermContract.setEndDate(LocalDate.now().plusYears(10));
        
        // 模拟服务层
        when(purchaseContractService.createContract(any(PurchaseContract.class))).thenReturn(4L);

        // 执行测试
        mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longTermContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("性能测试：批量查询合同列表")
    void testBatchQueryContractList() throws Exception {
        // 执行测试 - 查询大量数据
        mockMvc.perform(get("/api/erp/purchase/contract")
                .param("page", "1")
                .param("size", "100") // 较大分页
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}