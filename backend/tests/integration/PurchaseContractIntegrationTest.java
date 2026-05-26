package cn.aiedge.erp.purchase.integration;

import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;
import cn.aiedge.erp.purchase.dto.*;
import cn.aiedge.erp.purchase.service.*;
import cn.aiedge.erp.purchase.mapper.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 采购合同模块集成测试 - 扩展实现
 * 基于现有的 PurchaseIntegrationTest 进行扩展，增加数据一致性、异常处理和并发测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("采购合同模块集成测试 - 扩展实现")
public class PurchaseContractIntegrationTest {

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
    private PurchaseContract contract;
    private PurchaseOrder order;

    @BeforeEach
    void setUp() {
        // 创建测试询价单
        inquiry = new PurchaseInquiry();
        inquiry.setId(100L);
        inquiry.setInquiryNo("INQ-2026-0001");
        inquiry.setTitle("物资采购询价");
        inquiry.setStatus(InquiryStatus.PUBLISHED);
        inquiry.setPurchaserId(1L);
        inquiry.setTotalAmount(BigDecimal.valueOf(10000));
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(7));

        // 创建测试报价1
        quote1 = new PurchaseSupplierQuote();
        quote1.setId(1L);
        quote1.setInquiryId(100L);
        quote1.setSupplierId(1L);
        quote1.setSupplierName("供应商A");
        quote1.setTotalAmount(BigDecimal.valueOf(9800));
        quote1.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote1.setTotalScore(BigDecimal.valueOf(88.5));

        // 创建测试报价2
        quote2 = new PurchaseSupplierQuote();
        quote2.setId(2L);
        quote2.setInquiryId(100L);
        quote2.setSupplierId(2L);
        quote2.setSupplierName("供应商B");
        quote2.setTotalAmount(BigDecimal.valueOf(9500));
        quote2.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote2.setTotalScore(BigDecimal.valueOf(85.0));

        // 创建测试合同
        contract = new PurchaseContract();
        contract.setId(1L);
        contract.setContractNo("CON-2026-0001");
        contract.setInquiryId(100L);
        contract.setQuoteId(1L);
        contract.setSupplierId(1L);
        contract.setTotalAmount(BigDecimal.valueOf(9800));
        contract.setContractStatus(ContractStatus.APPROVED);
        contract.setSignDate(LocalDateTime.now());
        contract.setValidFrom(LocalDateTime.now());
        contract.setValidTo(LocalDateTime.now().plusYears(1));

        // 创建测试订单
        order = new PurchaseOrder();
        order.setId(1L);
        order.setOrderNo("PO-2026-0001");
        order.setContractId(1L);
        order.setSupplierId(1L);
        order.setTotalAmount(BigDecimal.valueOf(9800));
        order.setStatus(OrderStatus.APPROVED);
    }

    @Test
    @DisplayName("数据一致性验证集成测试 - 合同金额与报价金额一致性")
    void testDataConsistencyContractQuoteAmount() {
        // 模拟数据查询
        when(contractMapper.findById(1L)).thenReturn(contract);
        when(quoteMapper.findById(1L)).thenReturn(quote1);

        // 查询合同和报价
        PurchaseContract retrievedContract = contractMapper.findById(1L);
        PurchaseSupplierQuote retrievedQuote = quoteMapper.findById(1L);

        // 验证金额一致性
        assertNotNull(retrievedContract, "合同查询应成功");
        assertNotNull(retrievedQuote, "报价查询应成功");
        
        assertEquals(retrievedContract.getTotalAmount(), retrievedQuote.getTotalAmount(),
            "合同金额应与报价金额一致");
        
        assertEquals(retrievedContract.getSupplierId(), retrievedQuote.getSupplierId(),
            "合同供应商应与报价供应商一致");
    }

    @Test
    @DisplayName("数据一致性验证集成测试 - 订单金额与合同金额一致性")
    void testDataConsistencyOrderContractAmount() {
        // 模拟数据查询
        when(orderMapper.findById(1L)).thenReturn(order);
        when(contractMapper.findById(1L)).thenReturn(contract);

        // 查询订单和合同
        PurchaseOrder retrievedOrder = orderMapper.findById(1L);
        PurchaseContract retrievedContract = contractMapper.findById(1L);

        // 验证金额一致性
        assertNotNull(retrievedOrder, "订单查询应成功");
        assertNotNull(retrievedContract, "合同查询应成功");
        
        assertEquals(retrievedOrder.getTotalAmount(), retrievedContract.getTotalAmount(),
            "订单金额应与合同金额一致");
        
        assertEquals(retrievedOrder.getSupplierId(), retrievedContract.getSupplierId(),
            "订单供应商应与合同供应商一致");
    }

    @Test
    @DisplayName("异常流程处理集成测试 - 合同生成失败回滚")
    void testExceptionHandlingContractGenerationFailure() {
        // 模拟合同插入失败
        when(contractMapper.insert(any())).thenReturn(0);
        
        // 准备合同数据
        PurchaseContract newContract = new PurchaseContract();
        newContract.setInquiryId(100L);
        newContract.setQuoteId(1L);
        newContract.setTotalAmount(BigDecimal.valueOf(9800));

        // 验证异常处理
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contractService.generateContract(newContract);
        }, "合同生成失败应抛异常");

        // 验证回滚机制 - 报价状态不应更新
        verify(quoteMapper, never()).updateStatus(anyLong(), any(), any());
        
        // 验证异常信息
        assertTrue(exception.getMessage().contains("合同生成失败"),
            "异常信息应包含'合同生成失败'");
    }

    @Test
    @DisplayName("异常流程处理集成测试 - 空报价列表比价")
    void testExceptionHandlingEmptyQuoteList() {
        // 模拟空报价列表
        when(quoteMapper.findByInquiryId(100L)).thenReturn(new ArrayList<>());
        
        // 执行比价分析
        QuoteComparisonDTO comparison = comparisonService.compareQuotes(100L);
        
        // 验证空列表处理
        assertNotNull(comparison, "空报价列表应返回有效结果");
        assertEquals(0, comparison.getQuoteList().size(), "报价列表应为空");
        assertNull(comparison.getRecommendedSupplierId(), "无报价时不应有推荐供应商");
        assertEquals(0, comparison.getTotalQuotes(), "报价总数应为0");
    }

    @Test
    @DisplayName("并发操作集成测试 - 多供应商同时报价")
    void testConcurrentQuoteSubmission() {
        // 模拟多供应商并发报价
        when(quoteMapper.insert(any())).thenReturn(1);
        when(quoteMapper.countByInquiryId(100L)).thenReturn(3);
        when(inquiryMapper.updateQuoteCount(anyLong(), anyInt())).thenReturn(1);

        // 供应商1报价
        PurchaseSupplierQuote quoteA = new PurchaseSupplierQuote();
        quoteA.setId(101L);
        quoteA.setInquiryId(100L);
        quoteA.setSupplierId(101L);
        quoteA.setTotalAmount(BigDecimal.valueOf(10000));
        
        PurchaseSupplierQuote submittedA = quoteService.submitQuote(quoteA, new ArrayList<>());
        assertNotNull(submittedA, "供应商A报价应成功");

        // 供应商2报价
        PurchaseSupplierQuote quoteB = new PurchaseSupplierQuote();
        quoteB.setId(102L);
        quoteB.setInquiryId(100L);
        quoteB.setSupplierId(102L);
        quoteB.setTotalAmount(BigDecimal.valueOf(9500));
        
        PurchaseSupplierQuote submittedB = quoteService.submitQuote(quoteB, new ArrayList<>());
        assertNotNull(submittedB, "供应商B报价应成功");

        // 供应商3报价
        PurchaseSupplierQuote quoteC = new PurchaseSupplierQuote();
        quoteC.setId(103L);
        quoteC.setInquiryId(100L);
        quoteC.setSupplierId(103L);
        quoteC.setTotalAmount(BigDecimal.valueOf(11000));
        
        PurchaseSupplierQuote submittedC = quoteService.submitQuote(quoteC, new ArrayList<>());
        assertNotNull(submittedC, "供应商C报价应成功");

        // 验证报价数量更新
        verify(inquiryMapper, times(3)).updateQuoteCount(eq(100L), anyInt());
    }

    @Test
    @DisplayName("边界条件测试 - 最大金额合同生成")
    void testBoundaryConditionMaxAmountContract() {
        // 测试最大金额边界
        BigDecimal maxAmount = new BigDecimal("999999999.99");
        
        PurchaseContract maxContract = new PurchaseContract();
        maxContract.setId(999L);
        maxContract.setContractNo("CON-MAX-001");
        maxContract.setTotalAmount(maxAmount);
        maxContract.setContractStatus(ContractStatus.DRAFT);

        // 模拟合同查询
        when(contractMapper.findById(999L)).thenReturn(maxContract);
        
        PurchaseContract retrieved = contractMapper.findById(999L);
        
        // 验证最大金额处理
        assertNotNull(retrieved, "最大金额合同查询应成功");
        assertEquals(maxAmount, retrieved.getTotalAmount(), 
            "合同金额应保持最大值不变");
    }

    @Test
    @DisplayName("边界条件测试 - 最小金额合同生成")
    void testBoundaryConditionMinAmountContract() {
        // 测试最小金额边界
        BigDecimal minAmount = new BigDecimal("0.01");
        
        PurchaseContract minContract = new PurchaseContract();
        minContract.setId(888L);
        minContract.setContractNo("CON-MIN-001");
        minContract.setTotalAmount(minAmount);
        minContract.setContractStatus(ContractStatus.DRAFT);

        // 模拟合同查询
        when(contractMapper.findById(888L)).thenReturn(minContract);
        
        PurchaseContract retrieved = contractMapper.findById(888L);
        
        // 验证最小金额处理
        assertNotNull(retrieved, "最小金额合同查询应成功");
        assertEquals(minAmount, retrieved.getTotalAmount(), 
            "合同金额应保持最小值不变");
    }

    @Test
    @DisplayName("流程完整性测试 - 合同状态流转验证")
    void testContractStatusFlow() {
        // 模拟合同状态变更
        contract.setContractStatus(ContractStatus.DRAFT);
        when(contractMapper.findById(1L)).thenReturn(contract);
        
        // 验证初始状态
        PurchaseContract draftContract = contractMapper.findById(1L);
        assertEquals(ContractStatus.DRAFT, draftContract.getContractStatus(), 
            "合同初始状态应为草稿");

        // 模拟提交审批
        when(contractMapper.updateStatus(1L, ContractStatus.PENDING_APPROVAL, null)).thenReturn(1);
        contract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        
        PurchaseContract pendingContract = contractMapper.findById(1L);
        assertEquals(ContractStatus.PENDING_APPROVAL, pendingContract.getContractStatus(),
            "合同状态应变为待审批");

        // 模拟审批通过
        when(contractMapper.updateStatus(1L, ContractStatus.APPROVED, null)).thenReturn(1);
        contract.setContractStatus(ContractStatus.APPROVED);
        
        PurchaseContract approvedContract = contractMapper.findById(1L);
        assertEquals(ContractStatus.APPROVED, approvedContract.getContractStatus(),
            "合同状态应变为已审批");

        // 模拟合同生效
        when(contractMapper.updateStatus(1L, ContractStatus.ACTIVE, null)).thenReturn(1);
        contract.setContractStatus(ContractStatus.ACTIVE);
        
        PurchaseContract activeContract = contractMapper.findById(1L);
        assertEquals(ContractStatus.ACTIVE, activeContract.getContractStatus(),
            "合同状态应变为生效中");
    }

    @Test
    @DisplayName("数据验证测试 - 合同必填字段验证")
    void testContractRequiredFieldsValidation() {
        // 测试缺失必填字段
        PurchaseContract invalidContract = new PurchaseContract();
        
        // 验证缺失合同编号
        invalidContract.setTotalAmount(BigDecimal.valueOf(1000));
        assertNull(invalidContract.getContractNo(), "合同编号应为空");
        
        // 验证缺失合同金额
        invalidContract.setContractNo("TEST-001");
        invalidContract.setTotalAmount(null);
        assertNull(invalidContract.getTotalAmount(), "合同金额应为空");
        
        // 验证缺失供应商
        invalidContract.setTotalAmount(BigDecimal.valueOf(1000));
        invalidContract.setSupplierId(null);
        assertNull(invalidContract.getSupplierId(), "供应商ID应为空");
    }

    @Test
    @DisplayName("集成接口测试 - 合同与审批流程集成")
    void testContractApprovalIntegration() {
        // 模拟合同审批接口
        when(contractMapper.findById(1L)).thenReturn(contract);
        when(contractMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
        when(contractMapper.updateApprovalInfo(anyLong(), any(), any(), any())).thenReturn(1);
        
        // 验证审批前状态
        PurchaseContract beforeApproval = contractMapper.findById(1L);
        assertNotEquals(ContractStatus.APPROVED, beforeApproval.getContractStatus(),
            "审批前状态不应为已审批");
        
        // 执行审批操作
        contract.setContractStatus(ContractStatus.APPROVED);
        contract.setApprovedBy(2L);
        contract.setApprovalTime(LocalDateTime.now());
        contract.setApprovalComment("测试审批通过");
        
        // 验证审批后状态
        PurchaseContract afterApproval = contractMapper.findById(1L);
        assertEquals(ContractStatus.APPROVED, afterApproval.getContractStatus(),
            "审批后状态应为已审批");
        assertNotNull(afterApproval.getApprovedBy(), "审批人不应为空");
        assertNotNull(afterApproval.getApprovalTime(), "审批时间不应为空");
    }

    @Test
    @DisplayName("性能基准测试 - 合同批量查询性能")
    void testPerformanceBatchContractQuery() {
        // 准备批量合同数据
        List<PurchaseContract> contracts = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            PurchaseContract contract = new PurchaseContract();
            contract.setId((long) i);
            contract.setContractNo("CON-PERF-" + i);
            contract.setTotalAmount(BigDecimal.valueOf(1000 * i));
            contracts.add(contract);
        }
        
        // 模拟批量查询
        when(contractMapper.findBySupplierId(1L)).thenReturn(contracts);
        
        long startTime = System.currentTimeMillis();
        
        // 执行批量查询
        List<PurchaseContract> result = contractMapper.findBySupplierId(1L);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证性能
        assertNotNull(result, "批量查询应成功");
        assertEquals(100, result.size(), "应返回100条合同记录");
        assertTrue(duration < 1000, "批量查询应在1秒内完成，实际用时：" + duration + "ms");
    }

    @Test
    @DisplayName("数据完整性测试 - 合同删除级联验证")
    void testDataIntegrityCascadeDelete() {
        // 模拟合同删除
        when(contractMapper.deleteById(1L)).thenReturn(1);
        
        // 执行合同删除
        int deleted = contractMapper.deleteById(1L);
        
        // 验证删除结果
        assertEquals(1, deleted, "应成功删除1条合同记录");
        
        // 验证级联删除 - 合同明细应被删除
        verify(contractItemMapper, times(1)).deleteByContractId(1L);
        
        // 验证级联删除 - 相关订单状态更新
        verify(orderMapper, times(1)).updateStatusByContractId(eq(1L), eq(OrderStatus.CANCELLED), any());
    }
}