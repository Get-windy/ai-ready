package cn.aiedge.erp.purchase.integration;

import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;
import cn.aiedge.erp.purchase.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 集成测试数据工厂
 * 提供可复用的测试数据创建方法
 */
public class TestDataFactory {

    /**
     * 创建标准采购询价单
     */
    public static PurchaseInquiry createStandardInquiry() {
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setTitle("2024年度IT设备采购询价单");
        inquiry.setRequirementDesc("采购服务器、办公电脑等IT设备");
        inquiry.setDepartmentId(1L);
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(30));
        inquiry.setStatus(InquiryStatus.DRAFT);
        inquiry.setCreatedBy(1L);
        inquiry.setCreatedAt(LocalDateTime.now());
        
        return inquiry;
    }

    /**
     * 创建包含询价项的询价单
     */
    public static PurchaseInquiry createInquiryWithItems() {
        PurchaseInquiry inquiry = createStandardInquiry();
        
        PurchaseInquiryItem item1 = new PurchaseInquiryItem();
        item1.setMaterialCode("PROD-001");
        item1.setMaterialName("服务器X1");
        item1.setQuantity(BigDecimal.valueOf(2));
        item1.setUnit("台");
        item1.setEstimatedPrice(new BigDecimal("8000.00"));
        item1.setSpecification("配置：32核/128GB/2TB SSD");
        item1.setDeliveryRequirement("交货日期：" + LocalDateTime.now().plusDays(15));
        
        PurchaseInquiryItem item2 = new PurchaseInquiryItem();
        item2.setMaterialCode("PROD-002");
        item2.setMaterialName("办公电脑");
        item2.setQuantity(BigDecimal.valueOf(10));
        item2.setUnit("台");
        item2.setEstimatedPrice(new BigDecimal("4500.00"));
        item2.setSpecification("配置：i7/16GB/512GB SSD");
        item2.setDeliveryRequirement("交货日期：" + LocalDateTime.now().plusDays(20));
        
        inquiry.setItems(Arrays.asList(item1, item2));
        
        return inquiry;
    }

    /**
     * 创建供应商报价
     */
    public static PurchaseSupplierQuote createSupplierQuote(Long inquiryId, Long supplierId) {
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setInquiryId(inquiryId);
        quote.setSupplierId(supplierId);
        quote.setSupplierCode("SUP-" + String.format("%03d", supplierId));
        quote.setSupplierName("供应商-" + supplierId);
        quote.setQuoteDate(LocalDateTime.now());
        quote.setValidUntil(LocalDateTime.now().plusDays(7));
        quote.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote.setTotalAmount(new BigDecimal("0.00"));
        
        return quote;
    }
    
    /**
     * 重载方法：接受供应商编码字符串
     */
    public static PurchaseSupplierQuote createSupplierQuote(Long inquiryId, String supplierCode) {
        // 从供应商编码中提取数字部分作为 supplierId
        Long supplierId = Long.parseLong(supplierCode.replace("SUP-", ""));
        PurchaseSupplierQuote quote = createSupplierQuote(inquiryId, supplierId);
        quote.setSupplierCode(supplierCode);
        quote.setSupplierName("供应商-" + supplierCode);
        return quote;
    }

    /**
     * 创建报价项
     */
    public static PurchaseQuoteItem createQuoteItem(Long quoteId, String materialCode, BigDecimal unitPrice) {
        PurchaseQuoteItem item = new PurchaseQuoteItem();
        item.setQuoteId(quoteId);
        item.setInquiryItemId(1L);
        item.setMaterialName("产品-" + materialCode);
        item.setSpecification("规格说明");
        item.setUnit("台");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(unitPrice);
        item.setAmount(unitPrice);
        item.setTaxRate(BigDecimal.valueOf(13.00));
        item.setTaxAmount(unitPrice.multiply(BigDecimal.valueOf(0.13)));
        
        return item;
    }

    /**
     * 创建采购合同
     */
    public static PurchaseContract createPurchaseContract(Long inquiryId, Long quoteId) {
        PurchaseContract contract = new PurchaseContract();
        contract.setInquiryId(inquiryId);
        contract.setQuoteId(quoteId);
        contract.setContractNo("CONTRACT-" + System.currentTimeMillis());
        contract.setSupplierId(1L);
        contract.setSupplierName("供应商A");
        contract.setContractTitle("采购合同");
        contract.setContractType("标准合同");
        contract.setTotalAmount(new BigDecimal("25000.00"));
        contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setStartDate(LocalDateTime.now());
        contract.setEndDate(LocalDateTime.now().plusYears(1));
        
        return contract;
    }

    /**
     * 创建合同项
     */
    public static PurchaseContractItem createContractItem(Long contractId, String materialCode) {
        PurchaseContractItem item = new PurchaseContractItem();
        item.setContractId(contractId);
        item.setMaterialName("产品-" + materialCode);
        item.setSpecification("规格说明");
        item.setUnit("台");
        item.setQuantity(BigDecimal.valueOf(5));
        item.setUnitPrice(new BigDecimal("5000.00"));
        item.setAmount(new BigDecimal("25000.00"));
        item.setTaxRate(BigDecimal.valueOf(13.00));
        item.setLeadTime(30);
        item.setDeliveryLocation("北京市");
        
        return item;
    }

    /**
     * 创建报价比较DTO
     */
    public static QuoteComparisonDTO createQuoteComparison(Long inquiryId) {
        QuoteComparisonDTO comparison = new QuoteComparisonDTO();
        comparison.setInquiryId(inquiryId);
        comparison.setComparisonAnalysis("比价分析结果");
        
        QuoteComparisonItemDTO item1 = new QuoteComparisonItemDTO();
        item1.setSupplierId(1L);
        item1.setSupplierName("供应商A");
        item1.setTotalAmount(new BigDecimal("45000.00"));
        item1.setPriceScore(new BigDecimal("90"));
        item1.setQualityScore(new BigDecimal("85"));
        item1.setServiceScore(new BigDecimal("90"));
        item1.setTotalScore(new BigDecimal("88.33"));
        item1.setRank(1);
        
        QuoteComparisonItemDTO item2 = new QuoteComparisonItemDTO();
        item2.setSupplierId(2L);
        item2.setSupplierName("供应商B");
        item2.setTotalAmount(new BigDecimal("42000.00"));
        item2.setPriceScore(new BigDecimal("95"));
        item2.setQualityScore(new BigDecimal("80"));
        item2.setServiceScore(new BigDecimal("85"));
        item2.setTotalScore(new BigDecimal("86.67"));
        item2.setRank(2);
        
        comparison.setQuoteList(Arrays.asList(item1, item2));
        comparison.setRecommendedSupplierId(1L);
        comparison.setPriceVariance(6.67);
        comparison.setQualityVariance(5.0);
        comparison.setServiceVariance(5.0);
        
        return comparison;
    }

    /**
     * 创建采购决策DTO
     */
    public static PurchaseDecisionDTO createPurchaseDecision(Long inquiryId, Long quoteId) {
        PurchaseDecisionDTO decision = new PurchaseDecisionDTO();
        decision.setInquiryId(inquiryId);
        decision.setSelectedQuoteId(quoteId);
        decision.setDecisionMaker("采购经理");
        decision.setDecisionDate(LocalDateTime.now());
        decision.setApprovalStatus(ApprovalStatus.APPROVED);
        decision.setComments("该供应商报价合理，服务质量优秀");
        decision.setNextSteps("生成采购合同并通知供应商");
        
        return decision;
    }

    /**
     * 创建采购订单DTO
     */
    public static PurchaseOrderDTO createPurchaseOrder(Long contractId) {
        PurchaseOrderDTO order = new PurchaseOrderDTO();
        order.setContractId(contractId);
        order.setOrderNo("PO-" + System.currentTimeMillis());
        order.setOrderDate(LocalDateTime.now());
        order.setSupplierCode("SUP-001");
        order.setSupplierName("供应商A");
        order.setTotalAmount(new BigDecimal("25000.00"));
        order.setStatus(OrderStatus.DRAFT);
        
        return order;
    }
}
