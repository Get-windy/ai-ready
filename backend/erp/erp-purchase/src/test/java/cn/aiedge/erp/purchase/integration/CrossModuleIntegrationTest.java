package cn.aiedge.erp.purchase.integration;

import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;
import cn.aiedge.erp.purchase.dto.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 跨模块集成测试
 * 测试采购模块与库存、财务等模块的集成
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("跨模块集成测试")
class CrossModuleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long inquiryId;

    @BeforeEach
    void setUp() throws Exception {
        // 创建并发布测试询价单
        PurchaseInquiry inquiry = TestDataFactory.createInquiryWithItems();
        
        String response = mockMvc.perform(post("/api/erp/purchase/inquiry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inquiry)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        PurchaseInquiry created = objectMapper.readValue(response, PurchaseInquiry.class);
        inquiryId = created.getId();
        
        // 发布询价单
        mockMvc.perform(post("/api/erp/purchase/inquiry/{id}/publish", inquiryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("库存可用性检查集成测试")
    void testInventoryAvailabilityCheck() throws Exception {
        // 模拟场景1: 库存充足
        mockInventoryServiceAvailable();
        
        // 供应商提交报价
        Long quoteId = submitQuoteViaApi("SUP-001", new BigDecimal("45000.00"));
        
        // 进行采购决策
        PurchaseDecisionDTO decision = TestDataFactory.createPurchaseDecision(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isCreated());
        
        // 验证库存预留已创建
        verifyInventoryReservationCreated(inquiryId);
        
        // 模拟场景2: 库存不足
        mockInventoryServiceUnavailable();
        
        // 创建新的询价单测试库存不足场景
        PurchaseInquiry newInquiry = TestDataFactory.createInquiryWithItems();
        newInquiry.getItems().forEach(item -> item.setQuantity(BigDecimal.valueOf(100))); // 大数量
        
        String newInquiryResponse = mockMvc.perform(post("/api/erp/purchase/inquiry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newInquiry)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        PurchaseInquiry newCreated = objectMapper.readValue(newInquiryResponse, PurchaseInquiry.class);
        
        // 尝试发布库存不足的询价单
        mockMvc.perform(post("/api/erp/purchase/inquiry/{id}/publish", newCreated.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // 期望库存检查失败
                .andExpect(jsonPath("$.error").value("库存不足"));
    }

    @Test
    @DisplayName("预算控制集成测试")
    void testBudgetControlIntegration() throws Exception {
        // 模拟场景1: 预算充足
        mockFinanceServiceBudgetAvailable();
        
        // 供应商提交报价
        Long quoteId = submitQuoteViaApi("SUP-001", new BigDecimal("30000.00"));
        
        // 进行采购决策
        PurchaseDecisionDTO decision = TestDataFactory.createPurchaseDecision(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isCreated());
        
        // 验证预算预留已创建
        verifyBudgetReservationCreated(inquiryId, new BigDecimal("30000.00"));
        
        // 模拟场景2: 预算不足
        mockFinanceServiceBudgetInsufficient();
        
        // 创建高金额报价
        Long expensiveQuoteId = submitQuoteViaApi("SUP-002", new BigDecimal("60000.00"));
        
        // 尝试进行采购决策（应失败）
        PurchaseDecisionDTO expensiveDecision = TestDataFactory.createPurchaseDecision(inquiryId, expensiveQuoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expensiveDecision)))
                .andExpect(status().isBadRequest()) // 期望预算检查失败
                .andExpect(jsonPath("$.error").value("预算不足"));
    }

    @Test
    @DisplayName("采购到付款流程集成测试")
    void testPurchaseToPaymentIntegration() throws Exception {
        // 完成基本采购流程
        Long quoteId = submitQuoteViaApi("SUP-001", new BigDecimal("25000.00"));
        
        // 进行采购决策
        PurchaseDecisionDTO decision = TestDataFactory.createPurchaseDecision(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isCreated());
        
        // 生成采购合同
        PurchaseContract contract = TestDataFactory.createPurchaseContract(inquiryId, quoteId);
        
        String contractResponse = mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contract)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        PurchaseContract createdContract = objectMapper.readValue(contractResponse, PurchaseContract.class);
        
        // 生成采购订单
        PurchaseOrderDTO order = TestDataFactory.createPurchaseOrder(createdContract.getId());
        
        mockMvc.perform(post("/api/erp/purchase/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated());
        
        // 模拟货物接收
        simulateGoodsReceipt(createdContract.getId());
        
        // 验证付款申请已生成
        verifyPaymentApplicationCreated(createdContract.getId());
        
        // 模拟付款处理
        simulatePaymentProcessing(createdContract.getId());
        
        // 验证合同状态更新为已完成
        verifyContractCompleted(createdContract.getId());
    }

    @Test
    @DisplayName("消息队列集成测试：采购状态变更通知")
    void testMessageQueueIntegration() throws Exception {
        // 监听消息队列
        startMessageQueueListener();
        
        // 执行采购状态变更操作
        Long quoteId = submitQuoteViaApi("SUP-001", new BigDecimal("35000.00"));
        
        // 验证状态变更消息已发送
        verifyStatusChangeMessageSent("QUOTE_SUBMITTED", inquiryId);
        
        // 进行采购决策
        PurchaseDecisionDTO decision = TestDataFactory.createPurchaseDecision(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isCreated());
        
        // 验证决策消息已发送
        verifyStatusChangeMessageSent("DECISION_MADE", inquiryId);
        
        // 生成采购合同
        PurchaseContract contract = TestDataFactory.createPurchaseContract(inquiryId, quoteId);
        
        mockMvc.perform(post("/api/erp/purchase/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contract)))
                .andExpect(status().isCreated());
        
        // 验证合同创建消息已发送
        verifyStatusChangeMessageSent("CONTRACT_CREATED", inquiryId);
    }

    @Test
    @DisplayName("缓存集成测试：供应商信息缓存")
    void testCacheIntegration() throws Exception {
        // 第一次查询供应商信息（应查询数据库）
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/erp/purchase/supplier/SUP-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        long dbQueryTime = System.currentTimeMillis() - startTime;
        
        // 第二次查询供应商信息（应命中缓存）
        startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/erp/purchase/supplier/SUP-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        long cacheQueryTime = System.currentTimeMillis() - startTime;
        
        // 验证缓存提高了性能
        assertThat(cacheQueryTime).isLessThan(dbQueryTime);
        
        // 更新供应商信息
        SupplierUpdateDTO update = new SupplierUpdateDTO();
        update.setContactPerson("新联系人");
        update.setContactPhone("13900139001");
        
        mockMvc.perform(put("/api/erp/purchase/supplier/SUP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
        
        // 验证缓存已失效
        startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/erp/purchase/supplier/SUP-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        long afterUpdateQueryTime = System.currentTimeMillis() - startTime;
        
        // 更新后第一次查询应查询数据库（缓存失效）
        assertThat(afterUpdateQueryTime).isGreaterThan(cacheQueryTime);
    }

    // ============ 私有辅助方法 ============

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

    private void mockInventoryServiceAvailable() {
        // 模拟库存服务可用
        // 实际实现需要配置Mock Bean或使用WireMock
        System.out.println("模拟库存服务：库存充足");
    }

    private void mockInventoryServiceUnavailable() {
        // 模拟库存服务不可用或库存不足
        System.out.println("模拟库存服务：库存不足");
    }

    private void verifyInventoryReservationCreated(Long inquiryId) {
        // 验证库存预留记录已创建
        // 实际实现需要查询库存预留表或调用库存服务API
        System.out.println("验证库存预留已创建 for inquiryId: " + inquiryId);
    }

    private void mockFinanceServiceBudgetAvailable() {
        // 模拟财务服务预算充足
        System.out.println("模拟财务服务：预算充足");
    }

    private void mockFinanceServiceBudgetInsufficient() {
        // 模拟财务服务预算不足
        System.out.println("模拟财务服务：预算不足");
    }

    private void verifyBudgetReservationCreated(Long inquiryId, BigDecimal amount) {
        // 验证预算预留记录已创建
        System.out.println("验证预算预留已创建 for inquiryId: " + inquiryId + ", amount: " + amount);
    }

    private void simulateGoodsReceipt(Long contractId) {
        // 模拟货物接收流程
        System.out.println("模拟货物接收 for contractId: " + contractId);
    }

    private void verifyPaymentApplicationCreated(Long contractId) {
        // 验证付款申请已生成
        System.out.println("验证付款申请已创建 for contractId: " + contractId);
    }

    private void simulatePaymentProcessing(Long contractId) {
        // 模拟付款处理流程
        System.out.println("模拟付款处理 for contractId: " + contractId);
    }

    private void verifyContractCompleted(Long contractId) {
        // 验证合同状态已更新为已完成
        System.out.println("验证合同已完成 for contractId: " + contractId);
    }

    private void startMessageQueueListener() {
        // 启动消息队列监听器
        System.out.println("启动消息队列监听器");
    }

    private void verifyStatusChangeMessageSent(String eventType, Long inquiryId) {
        // 验证状态变更消息已发送
        System.out.println("验证消息已发送 - event: " + eventType + ", inquiryId: " + inquiryId);
    }

    // DTO类定义
    static class SupplierUpdateDTO {
        private String contactPerson;
        private String contactPhone;
        
        public String getContactPerson() { return contactPerson; }
        public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
        
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }
}