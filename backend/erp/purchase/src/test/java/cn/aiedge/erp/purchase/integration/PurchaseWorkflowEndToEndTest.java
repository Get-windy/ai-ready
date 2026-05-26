package cn.aiedge.erp.purchase.integration;

import cn.aiedge.erp.purchase.controller.PurchaseInquiryController;
import cn.aiedge.erp.purchase.controller.PurchaseQuoteController;
import cn.aiedge.erp.purchase.controller.PurchaseContractController;
import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;
import cn.aiedge.erp.purchase.dto.*;
import cn.aiedge.erp.purchase.service.PurchaseInquiryService;
import cn.aiedge.erp.purchase.service.PurchaseQuoteService;
import cn.aiedge.erp.purchase.service.PurchaseContractService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 采购工作流端到端集成测试
 * 测试完整的采购流程：询价 -> 报价 -> 比价 -> 决策 -> 合同
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("采购工作流端到端集成测试")
class PurchaseWorkflowEndToEndTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PurchaseInquiryService inquiryService;

    @Autowired
    private PurchaseQuoteService quoteService;

    @Autowired
    private PurchaseContractService contractService;

    @Autowired
    private PurchaseInquiryController inquiryController;

    @Autowired
    private PurchaseQuoteController quoteController;

    @Autowired
    private PurchaseContractController contractController;

    private PurchaseInquiry testInquiry;
    private Long inquiryId;

    @BeforeEach
    void setUp() {
        // 创建测试询价单
        testInquiry = TestDataFactory.createInquiryWithItems();
        testInquiry = inquiryService.createInquiry(testInquiry);
        inquiryId = testInquiry.getId();
    }

    @Test
    @DisplayName("完整采购流程：创建询价 -> 发布 -> 报价 -> 比较 -> 决策 -> 合同")
    void testCompletePurchaseWorkflow() throws Exception {
        // 阶段1: 发布询价单
        testPublishInquiry();
        
        // 阶段2: 供应商报价
        Long quoteId1 = testSupplierQuoteSubmission("SUP-001");
        Long quoteId2 = testSupplierQuoteSubmission("SUP-002");
        
        // 阶段3: 报价比较分析
        testQuoteComparison();
        
        // 阶段4: 采购决策
        testPurchaseDecision(quoteId1);
        
        // 阶段5: 生成采购合同
        testPurchaseContractCreation(quoteId1);
        
        // 阶段6: 验证业务流程完整性
        verifyBusinessProcessIntegrity();
    }

    @Test
    @DisplayName("多供应商报价分析与最优选择测试")
    void testMultiSupplierQuoteAnalysis() throws Exception {
        // 发布询价单
        publishInquiryViaApi();
        
        // 多个供应商提交报价
        Long cheapQuoteId = submitQuoteViaApi("SUP-001", new BigDecimal("42000.00"));
        Long expensiveQuoteId = submitQuoteViaApi("SUP-002", new BigDecimal("48000.00"));
        Long balancedQuoteId = submitQuoteViaApi("SUP-003", new BigDecimal("45000.00"));
        
        // 获取报价比较结果
        QuoteComparisonDTO comparison = getQuoteComparisonViaApi();
        
        // 验证分析结果
        assertThat(comparison).isNotNull();
        assertThat(comparison.getQuotes()).hasSize(3);
        assertThat(comparison.getRecommendedSupplier()).isEqualTo("SUP-001");
        assertThat(comparison.getTotalSavings()).isGreaterThan(BigDecimal.ZERO);
        
        // 验证报价历史记录
        List<PurchaseSupplierQuote> quotes = quoteService.getQuotesByInquiry(inquiryId);
        assertThat(quotes).hasSize(3);
    }

    @Test
    @DisplayName("异常流程测试：报价超时和重新报价")
    void testQuoteTimeoutAndResubmission() throws Exception {
        // 发布询价单
        publishInquiryViaApi();
        
        // 供应商提交报价
        Long quoteId = submitQuoteViaApi("SUP-001", new BigDecimal("50000.00"));
        
        // 模拟报价过期
        expireQuote(quoteId);
        
        // 尝试使用过期报价进行决策
        testExpiredQuoteDecision(quoteId);
        
        // 供应商重新报价
        Long newQuoteId = resubmitQuoteViaApi("SUP-001", new BigDecimal("48000.00"));
        
        // 验证新报价有效
        PurchaseSupplierQuote newQuote = quoteService.getQuoteById(newQuoteId);
        assertThat(newQuote.getStatus()).isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(newQuote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("48000.00"));
    }

    @Test
    @DisplayName("采购合同生命周期管理测试")
    void testPurchaseContractLifecycle() throws Exception {
        // 完成基本采购流程
        publishInquiryViaApi();
        Long quoteId = submitQuoteViaApi("SUP-001", new BigDecimal("45000.00"));
        
        // 生成采购合同
        PurchaseContract contract = createContractViaApi(quoteId);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        
        // 审批合同
        testContractApproval(contract.getId());
        
        // 激活合同
        testContractActivation(contract.getId());
        
        // 执行合同（创建采购订单）
        testContractExecution(contract.getId());
        
        // 完成合同
        testContractCompletion(contract.getId());
    }

    // ============ 私有辅助方法 ============

    private void testPublishInquiry() throws Exception {
        mockMvc.perform(post("/api/erp/purchase/inquiry/{id}/publish", inquiryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        
        PurchaseInquiry published = inquiryService.getInquiryById(inquiryId);
        assertThat(published.getStatus()).isEqualTo(InquiryStatus.PUBLISHED);
    }

    private Long testSupplierQuoteSubmission(String supplierCode) throws Exception {
        PurchaseSupplierQuote quote = TestDataFactory.createSupplierQuote(inquiryId, supplierCode);
        
        mockMvc.perform(post("/api/erp/purchase/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quote)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierCode").value(supplierCode))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        
        List<PurchaseSupplierQuote> quotes = quoteService.getQuotesByInquiry(inquiryId);
        return quotes.stream()
                .filter(q -> q.getSupplierCode().equals(supplierCode))
                .findFirst()
                .map(PurchaseSupplierQuote::getId)
                .orElseThrow();
    }

    private void testQuoteComparison() throws Exception {
        mockMvc.perform(get("/api/erp/purchase/inquiry/{id}/quote-comparison", inquiryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiryId").value(inquiryId))
                .andExpect(jsonPath("$.quotes").isArray());
    }

    private void testPurchaseDecision(Long quoteId) throws Exception {
        PurchaseDecisionDTO decision = TestDataFactory.createPurchaseDecision(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isCreated());
        
        // 验证询价单状态已更新
        PurchaseInquiry inquiry = inquiryService.getInquiryById(inquiryId);
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.DECISION_MADE);
    }

    private void testPurchaseContractCreation(Long quoteId) throws Exception {
        PurchaseContract contract = TestDataFactory.createPurchaseContract(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contract)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inquiryId").value(inquiryId))
                .andExpect(jsonPath("$.quoteId").value(quoteId));
    }

    private void verifyBusinessProcessIntegrity() {
        // 验证所有阶段数据完整性
        PurchaseInquiry finalInquiry = inquiryService.getInquiryById(inquiryId);
        List<PurchaseSupplierQuote> quotes = quoteService.getQuotesByInquiry(inquiryId);
        List<PurchaseContract> contracts = contractService.getContractsByInquiry(inquiryId);
        
        assertThat(finalInquiry).isNotNull();
        assertThat(finalInquiry.getStatus()).isIn(InquiryStatus.CONTRACT_CREATED, InquiryStatus.COMPLETED);
        assertThat(quotes).isNotEmpty();
        assertThat(contracts).isNotEmpty();
        
        // 验证金额计算正确性
        BigDecimal inquiryBudget = finalInquiry.getBudget();
        BigDecimal totalQuoteAmount = quotes.stream()
                .map(PurchaseSupplierQuote::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(totalQuoteAmount).isLessThanOrEqualTo(inquiryBudget);
    }

    private void publishInquiryViaApi() throws Exception {
        mockMvc.perform(post("/api/erp/purchase/inquiry/{id}/publish", inquiryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private Long submitQuoteViaApi(String supplierCode, BigDecimal totalAmount) throws Exception {
        PurchaseSupplierQuote quote = TestDataFactory.createSupplierQuote(inquiryId, supplierCode);
        quote.setTotalAmount(totalAmount);
        
        String response = mockMvc.perform(post("/api/erp/purchase/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quote)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        return objectMapper.readValue(response, PurchaseSupplierQuote.class).getId();
    }

    private QuoteComparisonDTO getQuoteComparisonViaApi() throws Exception {
        String response = mockMvc.perform(get("/api/erp/purchase/inquiry/{id}/quote-comparison", inquiryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        return objectMapper.readValue(response, QuoteComparisonDTO.class);
    }

    private void expireQuote(Long quoteId) {
        // 这里需要调用服务层方法来模拟报价过期
        // 实际实现可能需要修改数据库中的valid_until字段
        System.out.println("模拟报价过期: quoteId=" + quoteId);
    }

    private void testExpiredQuoteDecision(Long quoteId) throws Exception {
        PurchaseDecisionDTO decision = TestDataFactory.createPurchaseDecision(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isBadRequest()); // 期望返回错误，因为报价已过期
    }

    private Long resubmitQuoteViaApi(String supplierCode, BigDecimal totalAmount) throws Exception {
        return submitQuoteViaApi(supplierCode, totalAmount);
    }

    private PurchaseContract createContractViaApi(Long quoteId) throws Exception {
        PurchaseContract contract = TestDataFactory.createPurchaseContract(inquiryId, quoteId);
        
        String response = mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contract)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        return objectMapper.readValue(response, PurchaseContract.class);
    }

    private void testContractApproval(Long contractId) throws Exception {
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/approve", contractId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private void testContractActivation(Long contractId) throws Exception {
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/activate", contractId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private void testContractExecution(Long contractId) throws Exception {
        PurchaseOrderDTO order = TestDataFactory.createPurchaseOrder(contractId);
        
        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated());
    }

    private void testContractCompletion(Long contractId) throws Exception {
        mockMvc.perform(post("/api/erp/purchase/contract/{id}/complete", contractId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}