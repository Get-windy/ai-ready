package cn.aiedge.erp.purchase.integration.testdata;

import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 采购合同集成测试数据工厂
 * 提供标准化的测试数据生成方法，确保测试数据的一致性
 */
public class PurchaseContractTestDataFactory {

    private static long idCounter = 1000L;
    
    /**
     * 创建标准询价单测试数据
     */
    public static PurchaseInquiry createStandardInquiry() {
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setId(generateId());
        inquiry.setInquiryNo("INQ-TEST-" + inquiry.getId());
        inquiry.setTitle("测试询价单");
        inquiry.setInquiryType("物资采购");
        inquiry.setStatus(InquiryStatus.PUBLISHED);
        inquiry.setPurchaserId(1L);
        inquiry.setTotalAmount(BigDecimal.valueOf(10000));
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(7));
        inquiry.setPublishDate(LocalDateTime.now());
        inquiry.setCreatedBy(1L);
        inquiry.setCreatedAt(LocalDateTime.now());
        return inquiry;
    }
    
    /**
     * 创建标准供应商报价测试数据
     */
    public static PurchaseSupplierQuote createStandardQuote(Long inquiryId, Long supplierId, String supplierName) {
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setId(generateId());
        quote.setInquiryId(inquiryId);
        quote.setSupplierId(supplierId);
        quote.setSupplierName(supplierName);
        quote.setQuoteNo("QT-TEST-" + quote.getId());
        quote.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote.setTotalAmount(BigDecimal.valueOf(9500 + (supplierId * 100)));
        quote.setPriceScore(BigDecimal.valueOf(85.0 + supplierId));
        quote.setQualityScore(BigDecimal.valueOf(80.0 + supplierId));
        quote.setServiceScore(BigDecimal.valueOf(75.0 + supplierId));
        quote.setTotalScore(BigDecimal.valueOf(80.0 + supplierId));
        quote.setValidUntil(LocalDateTime.now().plusDays(30));
        quote.setCreatedBy(supplierId * 1000L);
        quote.setCreatedAt(LocalDateTime.now());
        return quote;
    }
    
    /**
     * 创建标准报价明细列表
     */
    public static List<PurchaseQuoteItem> createStandardQuoteItems(Long quoteId, int itemCount) {
        List<PurchaseQuoteItem> items = new ArrayList<>();
        for (int i = 1; i <= itemCount; i++) {
            PurchaseQuoteItem item = new PurchaseQuoteItem();
            item.setQuoteId(quoteId);
            item.setInquiryItemId((long) i);
            item.setMaterialName("测试物料" + i);
            item.setSpecification("规格" + i);
            item.setQuantity(BigDecimal.valueOf(100 * i));
            item.setUnitPrice(BigDecimal.valueOf(50 + i));
            item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
            items.add(item);
        }
        return items;
    }
    
    /**
     * 创建标准采购合同测试数据
     */
    public static PurchaseContract createStandardContract(Long inquiryId, Long quoteId, Long supplierId) {
        PurchaseContract contract = new PurchaseContract();
        contract.setId(generateId());
        contract.setContractNo("CON-TEST-" + contract.getId());
        contract.setInquiryId(inquiryId);
        contract.setQuoteId(quoteId);
        contract.setSupplierId(supplierId);
        contract.setSupplierName("测试供应商" + supplierId);
        contract.setTotalAmount(BigDecimal.valueOf(10000));
        contract.setContractStatus(ContractStatus.DRAFT);
        contract.setSignDate(LocalDateTime.now());
        contract.setValidFrom(LocalDateTime.now());
        contract.setValidTo(LocalDateTime.now().plusYears(1));
        contract.setPaymentTerms("货到付款");
        contract.setDeliveryTerms("30天内交付");
        contract.setCreatedBy(1L);
        contract.setCreatedAt(LocalDateTime.now());
        return contract;
    }
    
    /**
     * 创建标准合同明细列表
     */
    public static List<PurchaseContractItem> createStandardContractItems(Long contractId, int itemCount) {
        List<PurchaseContractItem> items = new ArrayList<>();
        for (int i = 1; i <= itemCount; i++) {
            PurchaseContractItem item = new PurchaseContractItem();
            item.setContractId(contractId);
            item.setMaterialName("合同物料" + i);
            item.setSpecification("合同规格" + i);
            item.setQuantity(BigDecimal.valueOf(100 * i));
            item.setUnitPrice(BigDecimal.valueOf(50 + i));
            item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
            item.setDeliveryDate(LocalDateTime.now().plusDays(30 * i));
            items.add(item);
        }
        return items;
    }
    
    /**
     * 创建标准采购订单测试数据
     */
    public static PurchaseOrder createStandardOrder(Long contractId, Long supplierId) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(generateId());
        order.setOrderNo("PO-TEST-" + order.getId());
        order.setContractId(contractId);
        order.setSupplierId(supplierId);
        order.setTotalAmount(BigDecimal.valueOf(10000));
        order.setStatus(OrderStatus.DRAFT);
        order.setRequiredDate(LocalDateTime.now().plusDays(30));
        order.setCreatedBy(1L);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
    
    /**
     * 创建标准订单明细列表
     */
    public static List<PurchaseOrderItem> createStandardOrderItems(Long orderId, int itemCount) {
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (int i = 1; i <= itemCount; i++) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(orderId);
            item.setMaterialName("订单物料" + i);
            item.setSpecification("订单规格" + i);
            item.setQuantity(BigDecimal.valueOf(100 * i));
            item.setUnitPrice(BigDecimal.valueOf(50 + i));
            item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
            item.setExpectedDate(LocalDateTime.now().plusDays(30 * i));
            items.add(item);
        }
        return items;
    }
    
    /**
     * 创建边界条件测试数据 - 最大金额合同
     */
    public static PurchaseContract createMaxAmountContract() {
        PurchaseContract contract = createStandardContract(1000L, 1001L, 1L);
        contract.setContractNo("CON-MAX-001");
        contract.setTotalAmount(new BigDecimal("999999999.99"));
        return contract;
    }
    
    /**
     * 创建边界条件测试数据 - 最小金额合同
     */
    public static PurchaseContract createMinAmountContract() {
        PurchaseContract contract = createStandardContract(1000L, 1001L, 1L);
        contract.setContractNo("CON-MIN-001");
        contract.setTotalAmount(new BigDecimal("0.01"));
        return contract;
    }
    
    /**
     * 创建异常场景测试数据 - 缺失必填字段合同
     */
    public static PurchaseContract createInvalidContract() {
        PurchaseContract contract = new PurchaseContract();
        contract.setId(generateId());
        // 故意缺失必填字段：contractNo, totalAmount, supplierId
        contract.setContractStatus(ContractStatus.DRAFT);
        return contract;
    }
    
    /**
     * 创建并发测试数据 - 多供应商报价列表
     */
    public static List<PurchaseSupplierQuote> createConcurrentQuotes(Long inquiryId, int supplierCount) {
        List<PurchaseSupplierQuote> quotes = new ArrayList<>();
        for (int i = 1; i <= supplierCount; i++) {
            quotes.add(createStandardQuote(inquiryId, (long) i, "并发供应商" + i));
        }
        return quotes;
    }
    
    /**
     * 创建批量性能测试数据
     */
    public static List<PurchaseContract> createBatchContracts(int count) {
        List<PurchaseContract> contracts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            PurchaseContract contract = createStandardContract(
                1000L + i, 
                1001L + i, 
                (long) i
            );
            contract.setContractNo("CON-BATCH-" + i);
            contracts.add(contract);
        }
        return contracts;
    }
    
    /**
     * 创建完整采购流程测试数据集
     */
    public static PurchaseTestDataset createCompletePurchaseDataset() {
        PurchaseTestDataset dataset = new PurchaseTestDataset();
        
        // 创建询价单
        dataset.inquiry = createStandardInquiry();
        
        // 创建多个供应商报价
        dataset.quotes = new ArrayList<>();
        dataset.quoteItemsMap = new HashMap<>();
        
        for (int i = 1; i <= 3; i++) {
            PurchaseSupplierQuote quote = createStandardQuote(
                dataset.inquiry.getId(), 
                (long) i, 
                "供应商" + i
            );
            dataset.quotes.add(quote);
            
            // 为每个报价创建明细
            dataset.quoteItemsMap.put(
                quote.getId(), 
                createStandardQuoteItems(quote.getId(), 5)
            );
        }
        
        // 选择第一个报价生成合同
        PurchaseSupplierQuote selectedQuote = dataset.quotes.get(0);
        dataset.contract = createStandardContract(
            dataset.inquiry.getId(),
            selectedQuote.getId(),
            selectedQuote.getSupplierId()
        );
        
        // 为合同创建明细
        dataset.contractItems = createStandardContractItems(dataset.contract.getId(), 5);
        
        // 基于合同生成订单
        dataset.order = createStandardOrder(
            dataset.contract.getId(),
            dataset.contract.getSupplierId()
        );
        
        // 为订单创建明细
        dataset.orderItems = createStandardOrderItems(dataset.order.getId(), 5);
        
        return dataset;
    }
    
    /**
     * 生成唯一ID
     */
    private static synchronized long generateId() {
        return idCounter++;
    }
    
    /**
     * 测试数据集封装类
     */
    public static class PurchaseTestDataset {
        public PurchaseInquiry inquiry;
        public List<PurchaseSupplierQuote> quotes;
        public Map<Long, List<PurchaseQuoteItem>> quoteItemsMap;
        public PurchaseContract contract;
        public List<PurchaseContractItem> contractItems;
        public PurchaseOrder order;
        public List<PurchaseOrderItem> orderItems;
        
        @Override
        public String toString() {
            return String.format(
                "PurchaseTestDataset[inquiry=%s, quotes=%d, contract=%s, order=%s]",
                inquiry != null ? inquiry.getInquiryNo() : "null",
                quotes != null ? quotes.size() : 0,
                contract != null ? contract.getContractNo() : "null",
                order != null ? order.getOrderNo() : "null"
            );
        }
    }
    
    /**
     * 重置ID计数器（用于测试隔离）
     */
    public static void resetIdCounter() {
        idCounter = 1000L;
    }
    
    /**
     * 计算报价总金额
     */
    public static BigDecimal calculateTotalAmount(List<PurchaseQuoteItem> items) {
        return items.stream()
            .map(PurchaseQuoteItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 计算合同总金额
     */
    public static BigDecimal calculateTotalAmount(List<PurchaseContractItem> items) {
        return items.stream()
            .map(PurchaseContractItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 验证数据一致性 - 合同金额与报价金额
     */
    public static boolean validateContractQuoteConsistency(
        PurchaseContract contract, 
        PurchaseSupplierQuote quote
    ) {
        if (contract == null || quote == null) {
            return false;
        }
        
        return contract.getTotalAmount().compareTo(quote.getTotalAmount()) == 0
            && contract.getSupplierId().equals(quote.getSupplierId())
            && contract.getQuoteId().equals(quote.getId());
    }
    
    /**
     * 验证数据一致性 - 订单金额与合同金额
     */
    public static boolean validateOrderContractConsistency(
        PurchaseOrder order,
        PurchaseContract contract
    ) {
        if (order == null || contract == null) {
            return false;
        }
        
        return order.getTotalAmount().compareTo(contract.getTotalAmount()) == 0
            && order.getSupplierId().equals(contract.getSupplierId())
            && order.getContractId().equals(contract.getId());
    }
}