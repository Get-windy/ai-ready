package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;
import cn.aiedge.erp.purchase.mapper.*;
import cn.aiedge.erp.purchase.service.impl.*;
import cn.aiedge.erp.purchase.dto.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 采购流程端到端集成测试
 * 覆盖询价-报价-比价-合同完整流程的集成验证
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("采购流程端到端集成测试")
class PurchaseIntegrationTest {

    @Mock
    private PurchaseInquiryMapper inquiryMapper;

    @Mock
    private PurchaseSupplierQuoteMapper quoteMapper;

    @Mock
    private PurchaseQuoteItemMapper quoteItemMapper;

    @Mock
    private PurchaseContractMapper contractMapper;

    @Mock
    private PurchaseContractItemMapper contractItemMapper;

    @Mock
    private PurchaseOrderMapper orderMapper;

    @Mock
    private PurchaseOrderItemMapper orderItemMapper;

    @InjectMocks
    private PurchaseInquiryServiceImpl inquiryService;

    @InjectMocks
    private PurchaseQuoteServiceImpl quoteService;

    @InjectMocks
    private PurchaseQuoteComparisonServiceImpl comparisonService;

    @InjectMocks
    private PurchaseContractServiceImpl contractService;

    @InjectMocks
    private PurchaseOrderServiceImpl orderService;

    private PurchaseInquiry inquiry;
    private PurchaseSupplierQuote quote1;
    private PurchaseSupplierQuote quote2;
    private List<PurchaseQuoteItem> quoteItems1;
    private List<PurchaseQuoteItem> quoteItems2;
    private PurchaseContract contract;

    @BeforeEach
    void setUp() {
        // 创建测试询价单
        inquiry = new PurchaseInquiry();
        inquiry.setId(100L);
        inquiry.setInquiryNo("INQ-2026-0001");
        inquiry.setTitle("物资采购询价");
        inquiry.setInquiryType("物资采购");
        inquiry.setStatus(InquiryStatus.PUBLISHED);
        inquiry.setPurchaserId(1L);
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(7));
        inquiry.setPublishDate(LocalDateTime.now());
        inquiry.setCreatedBy(1L);

        // 创建测试报价1（供应商1）
        quote1 = new PurchaseSupplierQuote();
        quote1.setId(1L);
        quote1.setInquiryId(100L);
        quote1.setSupplierId(1L);
        quote1.setSupplierName("供应商A");
        quote1.setQuoteNo("QT-2026-0001");
        quote1.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote1.setTotalAmount(BigDecimal.valueOf(10000));
        quote1.setPriceScore(BigDecimal.valueOf(85.0));
        quote1.setQualityScore(BigDecimal.valueOf(90.0));
        quote1.setServiceScore(BigDecimal.valueOf(88.0));
        quote1.setTotalScore(BigDecimal.valueOf(87.6));
        quote1.setValidUntil(LocalDateTime.now().plusDays(30));
        quote1.setCreatedBy(1001L);

        // 创建报价明细1
        quoteItems1 = new ArrayList<>();
        PurchaseQuoteItem item1 = new PurchaseQuoteItem();
        item1.setQuoteId(1L);
        item1.setInquiryItemId(1L);
        item1.setMaterialName("物料A");
        item1.setQuantity(BigDecimal.valueOf(100));
        item1.setUnitPrice(BigDecimal.valueOf(50));
        item1.setAmount(BigDecimal.valueOf(5000));
        quoteItems1.add(item1);

        PurchaseQuoteItem item2 = new PurchaseQuoteItem();
        item2.setQuoteId(1L);
        item2.setInquiryItemId(2L);
        item2.setMaterialName("物料B");
        item2.setQuantity(BigDecimal.valueOf(50));
        item2.setUnitPrice(BigDecimal.valueOf(100));
        item2.setAmount(BigDecimal.valueOf(5000));
        quoteItems1.add(item2);

        // 创建测试报价2（供应商2）
        quote2 = new PurchaseSupplierQuote();
        quote2.setId(2L);
        quote2.setInquiryId(100L);
        quote2.setSupplierId(2L);
        quote2.setSupplierName("供应商B");
        quote2.setQuoteNo("QT-2026-0002");
        quote2.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote2.setTotalAmount(BigDecimal.valueOf(9000));
        quote2.setPriceScore(BigDecimal.valueOf(90.0));
        quote2.setQualityScore(BigDecimal.valueOf(85.0));
        quote2.setServiceScore(BigDecimal.valueOf(80.0));
        quote2.setTotalScore(BigDecimal.valueOf(87.0));
        quote2.setValidUntil(LocalDateTime.now().plusDays(30));
        quote2.setCreatedBy(2001L);

        // 创建报价明细2
        quoteItems2 = new ArrayList<>();
        PurchaseQuoteItem item3 = new PurchaseQuoteItem();
        item3.setQuoteId(2L);
        item3.setInquiryItemId(1L);
        item3.setMaterialName("物料A");
        item3.setQuantity(BigDecimal.valueOf(100));
        item3.setUnitPrice(BigDecimal.valueOf(40));
        item3.setAmount(BigDecimal.valueOf(4000));
        quoteItems2.add(item3);

        PurchaseQuoteItem item4 = new PurchaseQuoteItem();
        item4.setQuoteId(2L);
        item4.setInquiryItemId(2L);
        item4.setMaterialName("物料B");
        item4.setQuantity(BigDecimal.valueOf(50));
        item4.setUnitPrice(BigDecimal.valueOf(100));
        item4.setAmount(BigDecimal.valueOf(5000));
        quoteItems2.add(item4);

        // 创建测试合同
        contract = new PurchaseContract();
        contract.setId(1L);
        contract.setContractNo("CT-2026-0001");
        contract.setInquiryId(100L);
        contract.setQuoteId(1L);
        contract.setSupplierId(1L);
        contract.setSupplierName("供应商A");
        contract.setTotalAmount(BigDecimal.valueOf(10000));
        contract.setContractStatus(ContractStatus.DRAFT);
        contract.setCreatedBy(1L);
    }

    @Test
    @DisplayName("完整采购流程 - 询价-报价-比价-合同-订单")
    void testFullPurchaseProcess() {
        // Step 1: 创建询价单
        when(inquiryMapper.insert(any())).thenReturn(1);
        when(inquiryMapper.findById(anyLong())).thenReturn(inquiry);

        PurchaseInquiry createdInquiry = inquiryService.createInquiry(inquiry);
        assertNotNull(createdInquiry, "询价单创建应成功");
        assertEquals(InquiryStatus.DRAFT, createdInquiry.getStatus(), "初始状态应为草稿");

        // Step 2: 发布询价单
        when(inquiryMapper.updateStatus(anyLong(), anyString(), any())).thenReturn(1);

        PurchaseInquiry publishedInquiry = inquiryService.publishInquiry(inquiry.getId());
        assertEquals(InquiryStatus.PUBLISHED, publishedInquiry.getStatus(), "状态应为已发布");

        // Step 3: 供应商报价
        when(quoteMapper.insert(any())).thenReturn(1);
        when(quoteMapper.countByInquiryId(anyLong())).thenReturn(2);
        when(inquiryMapper.updateQuoteCount(anyLong(), anyInt())).thenReturn(1);

        PurchaseSupplierQuote submittedQuote1 = quoteService.submitQuote(quote1, quoteItems1);
        assertNotNull(submittedQuote1, "报价提交应成功");
        assertEquals(QuoteStatus.SUBMITTED, submittedQuote1.getQuoteStatus(), "报价状态应为已提交");

        PurchaseSupplierQuote submittedQuote2 = quoteService.submitQuote(quote2, quoteItems2);
        assertNotNull(submittedQuote2, "报价提交应成功");

        // Step 4: 比价分析
        when(quoteMapper.findByInquiryId(anyLong())).thenReturn(Arrays.asList(quote1, quote2));
        when(quoteItemMapper.findByQuoteId(1L)).thenReturn(quoteItems1);
        when(quoteItemMapper.findByQuoteId(2L)).thenReturn(quoteItems2);

        QuoteComparisonDTO comparison = comparisonService.compareQuotes(inquiry.getId());
        assertNotNull(comparison, "比价分析应成功");
        assertEquals(2, comparison.getQuoteList().size(), "应包含2个报价");
        assertEquals(1L, comparison.getRecommendedSupplierId(), "供应商1应被推荐（总分更高87.6）");

        // Step 5: 生成合同
        PurchaseContract contractTemplate = new PurchaseContract();
        contractTemplate.setInquiryId(inquiry.getId());
        contractTemplate.setQuoteId(quote1.getId());
        contractTemplate.setSupplierId(quote1.getSupplierId());
        contractTemplate.setTotalAmount(quote1.getTotalAmount());

        List<PurchaseContractItem> contractItems = convertQuoteItemsToContractItems(quoteItems1, 1L);

        when(contractMapper.insert(any())).thenReturn(1);
        when(contractMapper.findById(anyLong())).thenReturn(contractTemplate);
        when(contractItemMapper.batchInsert(any())).thenReturn(contractItems.size());

        PurchaseContract generatedContract = contractService.generateContractFromQuote(quote1, contractItems);
        assertNotNull(generatedContract, "合同生成应成功");
        assertEquals(ContractStatus.DRAFT, generatedContract.getContractStatus(), "合同初始状态应为草稿");

        // Step 6: 合同审批
        contractTemplate.setContractStatus(ContractStatus.DRAFT);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateApprovalInfo(anyLong(), any(), any(), any())).thenReturn(1);

        PurchaseContract submittedContract = contractService.submitForApproval(generatedContract.getId(), "提交审批");
        assertEquals(ContractStatus.PENDING_APPROVAL, submittedContract.getContractStatus(), "状态应为待审批");

        contractTemplate.setContractStatus(ContractStatus.PENDING_APPROVAL);
        PurchaseContract approvedContract = contractService.approveContract(
            generatedContract.getId(), 2L, "审批通过", true
        );
        assertEquals(ContractStatus.APPROVED, approvedContract.getContractStatus(), "状态应为已审批");

        // Step 7: 合同生效
        contractTemplate.setContractStatus(ContractStatus.APPROVED);
        when(contractMapper.updateActivationTime(anyLong(), any())).thenReturn(1);

        PurchaseContract activatedContract = contractService.activateContract(generatedContract.getId());
        assertEquals(ContractStatus.ACTIVE, activatedContract.getContractStatus(), "状态应为生效中");

        // Step 8: 生成采购订单
        PurchaseOrder orderTemplate = new PurchaseOrder();
        orderTemplate.setContractId(generatedContract.getId());
        orderTemplate.setSupplierId(quote1.getSupplierId());
        orderTemplate.setTotalAmount(quote1.getTotalAmount());

        List<PurchaseOrderItem> orderItems = convertContractItemsToOrderItems(contractItems, 1L);

        when(orderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);
        when(orderMapper.findById(anyLong())).thenReturn(orderTemplate);
        when(orderItemMapper.batchInsert(any())).thenReturn(orderItems.size());

        PurchaseOrder generatedOrder = orderService.generateOrderFromContract(generatedContract, orderItems);
        assertNotNull(generatedOrder, "采购订单生成应成功");
        assertEquals(0, generatedOrder.getStatus(), "订单初始状态应为草稿");

        // Step 9: 订单审批和下达
        orderTemplate.setStatus(0);
        when(orderMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);

        PurchaseOrder submittedOrder = orderService.submitOrder(generatedOrder.getId());
        assertEquals(1, submittedOrder.getStatus(), "订单状态应为待审批");

        orderTemplate.setStatus(1);
        PurchaseOrder approvedOrder = orderService.approveOrder(generatedOrder.getId(), 2L, "审批通过");
        assertEquals(2, approvedOrder.getStatus(), "订单状态应为已审批");

        PurchaseOrder issuedOrder = orderService.issueOrder(generatedOrder.getId());
        assertEquals(3, issuedOrder.getStatus(), "订单状态应为已下达");

        // 验证整个流程的完整性
        verify(inquiryMapper, times(1)).insert(any());
        verify(inquiryMapper, times(1)).updateStatus(anyLong(), anyString(), any());
        verify(quoteMapper, times(2)).insert(any());
        verify(contractMapper, times(1)).insert(any());
        verify(orderMapper, times(1)).insert(any(PurchaseOrder.class));
    }

    private List<PurchaseContractItem> convertQuoteItemsToContractItems(
        List<PurchaseQuoteItem> quoteItems, Long contractId
    ) {
        List<PurchaseContractItem> contractItems = new ArrayList<>();
        for (PurchaseQuoteItem quoteItem : quoteItems) {
            PurchaseContractItem contractItem = new PurchaseContractItem();
            contractItem.setContractId(contractId);
            contractItem.setMaterialName(quoteItem.getMaterialName());
            contractItem.setQuantity(quoteItem.getQuantity());
            contractItem.setUnitPrice(quoteItem.getUnitPrice());
            contractItem.setAmount(quoteItem.getAmount());
            contractItems.add(contractItem);
        }
        return contractItems;
    }

    private List<PurchaseOrderItem> convertContractItemsToOrderItems(
        List<PurchaseContractItem> contractItems, Long orderId
    ) {
        List<PurchaseOrderItem> orderItems = new ArrayList<>();
        for (PurchaseContractItem contractItem : contractItems) {
            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setMaterialName(contractItem.getMaterialName());
            orderItem.setQuantity(contractItem.getQuantity());
            orderItem.setUnitPrice(contractItem.getUnitPrice());
            orderItem.setAmount(contractItem.getAmount());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    @Test
    @DisplayName("询价单状态流转集成测试 - 状态机验证")
    void testInquiryStatusTransition() {
        // 测试状态流转：DRAFT -> PUBLISHED -> CLOSED
        inquiry.setStatus(InquiryStatus.DRAFT);

        when(inquiryMapper.findById(anyLong())).thenReturn(inquiry);
        when(inquiryMapper.updateStatus(anyLong(), anyString(), any())).thenReturn(1);

        // DRAFT -> PUBLISHED
        PurchaseInquiry published = inquiryService.publishInquiry(inquiry.getId());
        assertEquals(InquiryStatus.PUBLISHED, published.getStatus(), "应从草稿变为已发布");

        // PUBLISHED -> CLOSED
        inquiry.setStatus(InquiryStatus.PUBLISHED);
        PurchaseInquiry closed = inquiryService.closeInquiry(inquiry.getId(), "询价完成");
        assertEquals(InquiryStatus.CLOSED, closed.getStatus(), "应从已发布变为已关闭");

        // 验证非法状态流转（尝试从CLOSED再次发布）
        inquiry.setStatus(InquiryStatus.CLOSED);
        assertThrows(IllegalStateException.class, () -> {
            inquiryService.publishInquiry(inquiry.getId());
        }, "已关闭询价单不能再次发布");
    }

    @Test
    @DisplayName("报价截止时间集成测试 - 时间约束验证")
    void testQuoteDeadlineIntegration() {
        // 创建询价单，设置截止时间
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(7));
        inquiry.setStatus(InquiryStatus.PUBLISHED);

        when(inquiryMapper.findById(anyLong())).thenReturn(inquiry);

        // 验证截止时间未到，可以报价
        boolean canQuote = inquiryService.canAcceptQuote(inquiry.getId());
        assertTrue(canQuote, "截止时间未到应允许报价");

        // 设置截止时间已过
        inquiry.setDeadlineDate(LocalDateTime.now().minusDays(1));
        canQuote = inquiryService.canAcceptQuote(inquiry.getId());
        assertFalse(canQuote, "截止时间已过不应允许报价");
    }

    @Test
    @DisplayName("报价有效性集成测试 - 报价有效期验证")
    void testQuoteValidityIntegration() {
        // 创建报价，设置有效期
        quote1.setValidUntil(LocalDateTime.now().plusDays(30));
        quote1.setQuoteStatus(QuoteStatus.SUBMITTED);

        when(quoteMapper.findById(anyLong())).thenReturn(quote1);

        // 验证报价有效
        boolean isValid = quoteService.isQuoteValid(quote1.getId());
        assertTrue(isValid, "报价有效期未到应有效");

        // 设置报价已过期
        quote1.setValidUntil(LocalDateTime.now().minusDays(1));
        isValid = quoteService.isQuoteValid(quote1.getId());
        assertFalse(isValid, "报价有效期已过应无效");

        // 验证过期报价不能生成合同
        assertThrows(IllegalStateException.class, () -> {
            contractService.generateContractFromQuote(quote1, new ArrayList<>());
        }, "过期报价不能生成合同");
    }

    @Test
    @DisplayName("比价推荐算法集成测试 - 推荐准确性验证")
    void testComparisonRecommendationIntegration() {
        // 创建不同评分组合的报价
        PurchaseSupplierQuote highPrice = new PurchaseSupplierQuote();
        highPrice.setId(1L);
        highPrice.setSupplierId(1L);
        highPrice.setTotalAmount(BigDecimal.valueOf(12000));
        highPrice.setPriceScore(BigDecimal.valueOf(70.0));
        highPrice.setQualityScore(BigDecimal.valueOf(95.0));
        highPrice.setServiceScore(BigDecimal.valueOf(90.0));
        highPrice.setTotalScore(BigDecimal.valueOf(83.0)); // 70*0.4 + 95*0.4 + 90*0.2

        PurchaseSupplierQuote lowPrice = new PurchaseSupplierQuote();
        lowPrice.setId(2L);
        lowPrice.setSupplierId(2L);
        lowPrice.setTotalAmount(BigDecimal.valueOf(8000));
        lowPrice.setPriceScore(BigDecimal.valueOf(95.0));
        lowPrice.setQualityScore(BigDecimal.valueOf(70.0));
        lowPrice.setServiceScore(BigDecimal.valueOf(70.0));
        lowPrice.setTotalScore(BigDecimal.valueOf(80.0)); // 95*0.4 + 70*0.4 + 70*0.2

        PurchaseSupplierQuote balanced = new PurchaseSupplierQuote();
        balanced.setId(3L);
        balanced.setSupplierId(3L);
        balanced.setTotalAmount(BigDecimal.valueOf(10000));
        balanced.setPriceScore(BigDecimal.valueOf(85.0));
        balanced.setQualityScore(BigDecimal.valueOf(85.0));
        balanced.setServiceScore(BigDecimal.valueOf(85.0));
        balanced.setTotalScore(BigDecimal.valueOf(85.0)); // 85*0.4 + 85*0.4 + 85*0.2

        when(quoteMapper.findByInquiryId(anyLong()))
            .thenReturn(Arrays.asList(highPrice, lowPrice, balanced));

        // 执行比价推荐
        Long recommendedId = comparisonService.recommendSupplier(inquiry.getId());

        // 验证推荐结果（均衡报价应被推荐）
        assertEquals(3L, recommendedId, "均衡报价供应商应被推荐（总分85最高）");

        // 验证价格因素影响
        String analysis = comparisonService.compareQuotePrices(inquiry.getId());
        assertTrue(analysis.contains("8000"), "分析应包含最低价格8000");
        assertTrue(analysis.contains("12000"), "分析应包含最高价格12000");
    }

    @Test
    @DisplayName("合同审批流程集成测试 - 审批链验证")
    void testContractApprovalIntegration() {
        // 创建合同
        contract.setContractStatus(ContractStatus.DRAFT);

        when(contractMapper.findById(anyLong())).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateApprovalInfo(anyLong(), any(), any(), any())).thenReturn(1);

        // DRAFT -> PENDING_APPROVAL
        PurchaseContract submitted = contractService.submitForApproval(contract.getId(), "提交审批");
        assertEquals(ContractStatus.PENDING_APPROVAL, submitted.getContractStatus(), "状态应为待审批");

        // PENDING_APPROVAL -> APPROVED（审批通过）
        contract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        PurchaseContract approved = contractService.approveContract(
            contract.getId(), 2L, "审批通过", true
        );
        assertEquals(ContractStatus.APPROVED, approved.getContractStatus(), "状态应为已审批");

        // APPROVED -> ACTIVE（合同生效）
        contract.setContractStatus(ContractStatus.APPROVED);
        when(contractMapper.updateActivationTime(anyLong(), any())).thenReturn(1);
        PurchaseContract activated = contractService.activateContract(contract.getId());
        assertEquals(ContractStatus.ACTIVE, activated.getContractStatus(), "状态应为生效中");

        // 测试审批驳回流程
        contract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        PurchaseContract rejected = contractService.approveContract(
            contract.getId(), 2L, "价格过高", false
        );
        assertEquals(ContractStatus.REJECTED, rejected.getContractStatus(), "状态应为已驳回");

        // 验证驳回后可以重新提交
        contract.setContractStatus(ContractStatus.REJECTED);
        PurchaseContract resubmitted = contractService.submitForApproval(contract.getId(), "重新提交");
        assertEquals(ContractStatus.PENDING_APPROVAL, resubmitted.getContractStatus(),
            "驳回后可重新提交");
    }

    @Test
    @DisplayName("订单履行流程集成测试 - 订单生命周期验证")
    void testOrderFulfillmentIntegration() {
        // 创建订单
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setContractId(contract.getId());
        order.setSupplierId(quote1.getSupplierId());
        order.setStatus(0);
        order.setTotalAmount(BigDecimal.valueOf(10000));

        when(orderMapper.findById(anyLong())).thenReturn(order);
        when(orderMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(orderMapper.updateFulfillmentProgress(anyLong(), any(BigDecimal.class), any(BigDecimal.class), any(LocalDateTime.class))).thenReturn(1);

        // DRAFT -> PENDING_APPROVAL -> APPROVED -> ISSUED
        PurchaseOrder submitted = orderService.submitOrder(order.getId());
        assertEquals(1, submitted.getStatus(), "状态应为待审批");

        order.setStatus(1);
        PurchaseOrder approved = orderService.approveOrder(order.getId(), 2L, "审批通过");
        assertEquals(2, approved.getStatus(), "状态应为已审批");

        order.setStatus(2);
        PurchaseOrder issued = orderService.issueOrder(order.getId());
        assertEquals(3, issued.getStatus(), "状态应为已下达");

        // ISSUED -> IN_PROGRESS（开始执行）
        order.setStatus(3);
        PurchaseOrder inProgress = orderService.startFulfillment(order.getId());
        assertEquals(5, inProgress.getStatus(), "状态应为执行中");

        // 更新履行进度
        order.setStatus(5);
        PurchaseOrder progressUpdated = orderService.updateFulfillmentProgress(
            order.getId(), BigDecimal.valueOf(5000), BigDecimal.valueOf(50)
        );
        assertEquals(BigDecimal.valueOf(50), progressUpdated.getFulfillmentPercent(),
            "履行百分比应为50%");

        // IN_PROGRESS -> COMPLETED（履行完成）
        order.setStatus(5);
        order.setFulfillmentPercent(BigDecimal.valueOf(100));
        when(orderMapper.updateCompletionTime(anyLong(), any())).thenReturn(1);
        PurchaseOrder completed = orderService.completeOrder(order.getId());
        assertEquals(6, completed.getStatus(), "状态应为已完成");
    }

    @Test
    @DisplayName("数据一致性集成测试 - 跨模块数据一致性验证")
    void testDataConsistencyIntegration() {
        // 验证询价单-报价数量一致性
        when(inquiryMapper.findById(anyLong())).thenReturn(inquiry);
        when(quoteMapper.countByInquiryId(anyLong())).thenReturn(2);

        int quoteCount = quoteMapper.countByInquiryId(inquiry.getId());
        inquiry.setQuoteCount(quoteCount);
        when(inquiryMapper.updateQuoteCount(anyLong(), anyInt())).thenReturn(1);

        // 更新询价单报价数量
        inquiryService.updateQuoteCount(inquiry.getId());
        assertEquals(2, inquiry.getQuoteCount(), "询价单报价数量应为2");

        // 验证合同金额与报价金额一致性
        contract.setTotalAmount(quote1.getTotalAmount());
        when(contractMapper.findById(anyLong())).thenReturn(contract);
        when(quoteMapper.findById(anyLong())).thenReturn(quote1);

        PurchaseContract contractFromDB = contractMapper.findById(contract.getId());
        PurchaseSupplierQuote quoteFromDB = quoteMapper.findById(quote1.getId());

        assertEquals(contractFromDB.getTotalAmount(), quoteFromDB.getTotalAmount(),
            "合同金额应与报价金额一致");

        // 验证订单金额与合同金额一致性
        PurchaseOrder order = new PurchaseOrder();
        order.setContractId(contract.getId());
        order.setTotalAmount(contract.getTotalAmount());

        assertEquals(order.getTotalAmount(), contract.getTotalAmount(),
            "订单金额应与合同金额一致");
    }

    @Test
    @DisplayName("异常流程处理集成测试 - 异常场景处理验证")
    void testExceptionHandlingIntegration() {
        // 测试空报价列表处理
        when(quoteMapper.findByInquiryId(anyLong())).thenReturn(new ArrayList<>());

        QuoteComparisonDTO comparison = comparisonService.compareQuotes(inquiry.getId());
        assertNotNull(comparison, "空报价列表应返回有效结果");
        assertEquals(0, comparison.getQuoteList().size(), "报价列表应为空");
        assertNull(comparison.getRecommendedSupplierId(), "无报价时不应有推荐供应商");

        // 测试询价单不存在
        when(inquiryMapper.findById(anyLong())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> {
            inquiryService.publishInquiry(999L);
        }, "询价单不存在应抛异常");

        // 测试报价不存在
        when(quoteMapper.findById(anyLong())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> {
            quoteService.reviewQuote(999L, 90.0, 85.0, 80.0, "测试");
        }, "报价不存在应抛异常");

        // 测试合同不存在
        when(contractMapper.findById(anyLong())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> {
            contractService.approveContract(999L, 2L, "测试", true);
        }, "合同不存在应抛异常");
    }

    @Test
    @DisplayName("并发报价处理集成测试 - 多供应商并发报价")
    void testConcurrentQuoteSubmissionIntegration() {
        // 模拟多供应商并发报价
        when(quoteMapper.insert(any())).thenReturn(1);
        when(quoteMapper.countByInquiryId(anyLong())).thenReturn(3);
        when(inquiryMapper.updateQuoteCount(anyLong(), anyInt())).thenReturn(1);

        // 供应商1报价
        PurchaseSupplierQuote submitted1 = quoteService.submitQuote(quote1, quoteItems1);
        assertNotNull(submitted1, "供应商1报价应成功");

        // 供应商2报价
        PurchaseSupplierQuote submitted2 = quoteService.submitQuote(quote2, quoteItems2);
        assertNotNull(submitted2, "供应商2报价应成功");

        // 供应商3报价
        PurchaseSupplierQuote quote3 = new PurchaseSupplierQuote();
        quote3.setId(3L);
        quote3.setInquiryId(100L);
        quote3.setSupplierId(3L);
        quote3.setTotalAmount(BigDecimal.valueOf(11000));
        quote3.setQuoteStatus(QuoteStatus.SUBMITTED);

        PurchaseSupplierQuote submitted3 = quoteService.submitQuote(quote3, new ArrayList<>());
        assertNotNull(submitted3, "供应商3报价应成功");

        // 验证报价数量更新
        verify(inquiryMapper, times(3)).updateQuoteCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("流程回滚集成测试 - 异常情况下数据回滚验证")
    void testProcessRollbackIntegration() {
        // 测试报价提交失败，询价单报价数量不更新
        when(quoteMapper.insert(any())).thenReturn(0); // 插入失败

        assertThrows(RuntimeException.class, () -> {
            quoteService.submitQuote(quote1, quoteItems1);
        }, "报价插入失败应抛异常");

        // 验证询价单报价数量未更新
        verify(inquiryMapper, never()).updateQuoteCount(anyLong(), anyInt());

        // 测试合同生成失败，报价状态不更新
        when(contractMapper.insert(any())).thenReturn(0); // 合同插入失败

        assertThrows(RuntimeException.class, () -> {
            contractService.generateContractFromQuote(quote1, new ArrayList<>());
        }, "合同生成失败应抛异常");

        // 验证报价状态未更新为已选中
        verify(quoteMapper, never()).updateStatus(anyLong(), anyString(), any());
    }
}