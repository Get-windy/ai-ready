package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.enums.ContractStatus;
import cn.aiedge.erp.purchase.service.PurchaseContractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
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
        testContract.setContractNo("PC-20260505-001");
        testContract.setSupplierId(100L);
        testContract.setSupplierName("测试供应商有限公司");
        testContract.setContractTitle("年度采购框架合同");
        testContract.setContractStatus(ContractStatus.DRAFT);
        testContract.setTotalAmount(new BigDecimal("50000.00"));
        testContract.setStartDate(LocalDateTime.now());
        testContract.setEndDate(LocalDateTime.now().plusYears(1));
        testContract.setRemark("年度采购框架合同");
    }

    @Test
    @DisplayName("获取采购合同详情测试")
    void testGetPurchaseContractById() throws Exception {
        when(purchaseContractService.getContractById(1L)).thenReturn(testContract);

        mockMvc.perform(get("/api/erp/purchase/contract/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.contractNo").value("PC-20260505-001"))
                .andExpect(jsonPath("$.supplierName").value("测试供应商有限公司"));
    }

    @Test
    @DisplayName("提交合同审批测试")
    void testSubmitContractForApproval() throws Exception {
        PurchaseContract submittedContract = new PurchaseContract();
        submittedContract.setId(1L);
        submittedContract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        
        when(purchaseContractService.submitForApproval(eq(1L), anyString())).thenReturn(submittedContract);

        mockMvc.perform(post("/api/erp/purchase/contract/{id}/submit", 1L)
                .param("reason", "提交审批"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("审批采购合同测试")
    void testApprovePurchaseContract() throws Exception {
        PurchaseContract approvedContract = new PurchaseContract();
        approvedContract.setId(1L);
        approvedContract.setContractStatus(ContractStatus.APPROVED);
        
        when(purchaseContractService.approveContract(eq(1L), anyLong(), anyString(), eq(true))).thenReturn(approvedContract);

        mockMvc.perform(post("/api/erp/purchase/contract/{id}/approve", 1L)
                .param("approverId", "1")
                .param("comment", "同意签署")
                .param("approved", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("激活采购合同测试")
    void testActivatePurchaseContract() throws Exception {
        PurchaseContract activatedContract = new PurchaseContract();
        activatedContract.setId(1L);
        activatedContract.setContractStatus(ContractStatus.ACTIVE);
        
        when(purchaseContractService.activateContract(1L)).thenReturn(activatedContract);

        mockMvc.perform(post("/api/erp/purchase/contract/{id}/activate", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("终止采购合同测试")
    void testTerminatePurchaseContract() throws Exception {
        PurchaseContract terminatedContract = new PurchaseContract();
        terminatedContract.setId(1L);
        terminatedContract.setContractStatus(ContractStatus.TERMINATED);
        
        when(purchaseContractService.terminateContract(eq(1L), anyString())).thenReturn(terminatedContract);

        mockMvc.perform(post("/api/erp/purchase/contract/{id}/terminate", 1L)
                .param("reason", "双方协商一致终止"))
                .andExpect(status().isOk());
    }

    // 合同续签/变更测试已删除：PurchaseContractService 虽有 renewContract/modifyContract，
    // 但当前 PurchaseContractController 未暴露 /renew 与 /modify 端点

    // 合同列表查询测试已删除：当前 PurchaseContractController 无 GET 列表端点

    @Test
    @DisplayName("异常测试：获取不存在的合同")
    void testGetNonExistentContract() throws Exception {
        when(purchaseContractService.getContractById(99999L)).thenReturn(null);

        mockMvc.perform(get("/api/erp/purchase/contract/{id}", 99999L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("边界测试：合同金额为零")
    void testContractWithZeroAmount() throws Exception {
        PurchaseContract zeroAmountContract = new PurchaseContract();
        zeroAmountContract.setId(3L);
        zeroAmountContract.setTotalAmount(BigDecimal.ZERO);
        
        when(purchaseContractService.getContractById(3L)).thenReturn(zeroAmountContract);

        mockMvc.perform(get("/api/erp/purchase/contract/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(0));
    }
}
