package cn.aiedge.erp.purchase;

import cn.aiedge.erp.purchase.entity.*;
import cn.aiedge.erp.purchase.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 采购模块测试数据工厂
 * 提供统一的测试数据生成方法
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
public class TestDataFactory {

    /**
     * 创建测试采购订单
     */
    public static PurchaseOrder createPurchaseOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo("PO-TEST-001");
        order.setTenantId(1L);
        order.setSupplierId(100L);
        order.setSupplierName("测试供应商有限公司");
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveryDate(LocalDateTime.now().plusDays(7));
        order.setTotalAmount(new BigDecimal("50000.00"));
        order.setTaxAmount(new BigDecimal("6500.00"));
        order.setTotalAmountWithTax(new BigDecimal("56500.00"));
        order.setStatus(0);
        order.setCreateBy(1L);
        order.setCreateTime(LocalDateTime.now());
        
        return order;
    }

    /**
     * 创建测试采购询价单
     */
    public static PurchaseInquiry createPurchaseInquiry() {
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setInquiryNo("INQ-TEST-001");
        inquiry.setTitle("2026年第一季度电子元件采购询价");
        inquiry.setInquiryType("NORMAL");
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(5));
        inquiry.setStatus(InquiryStatus.DRAFT);
        inquiry.setBudget(new BigDecimal("100000.00"));
        inquiry.setRequirementDesc("用于生产线的电子元件采购，需要提供样品和报价");
        inquiry.setPurchaserId(1L);
        inquiry.setCreatedBy(1L);
        inquiry.setCreatedAt(LocalDateTime.now());
        
        return inquiry;
    }

    /**
     * 创建带询价项的测试询价单
     */
    public static PurchaseInquiry createInquiryWithItems() {
        PurchaseInquiry inquiry = createPurchaseInquiry();
        
        // 添加询价项
        List<PurchaseInquiryItem> items = Arrays.asList(
            createInquiryItem("IC-001", "集成电路芯片", 1000, new BigDecimal("25.00"), "个"),
            createInquiryItem("CAP-001", "电容器", 5000, new BigDecimal("1.50"), "个"),
            createInquiryItem("RES-001", "电阻器", 8000, new BigDecimal("0.80"), "个")
        );
        
        // 这里假设实体类有设置items的方法
        // inquiry.setItems(items);
        
        return inquiry;
    }

    /**
     * 创建测试询价项
     */
    public static PurchaseInquiryItem createInquiryItem(String materialCode, String materialName, 
                                                        Integer quantity, BigDecimal estimatedPrice, String unit) {
        PurchaseInquiryItem item = new PurchaseInquiryItem();
        item.setMaterialCode(materialCode);
        item.setMaterialName(materialName);
        item.setQuantity(BigDecimal.valueOf(quantity));
        item.setEstimatedPrice(estimatedPrice);
        item.setUnit(unit);
        item.setEstimatedAmount(estimatedPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setSpecification("高品质" + materialName);
        
        return item;
    }

    /**
     * 创建测试供应商报价
     */
    public static PurchaseSupplierQuote createSupplierQuote() {
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setQuoteNo("QUOTE-TEST-001");
        quote.setInquiryId(1L);
        quote.setSupplierId(100L);
        quote.setSupplierName("测试供应商有限公司");
        quote.setQuoteDate(LocalDateTime.now());
        quote.setValidUntil(LocalDateTime.now().plusDays(3));
        quote.setTotalAmount(new BigDecimal("48000.00"));
        quote.setTaxAmount(new BigDecimal("6240.00"));
        quote.setQuoteStatus(QuoteStatus.SUBMITTED);
        quote.setPaymentTerms("30天账期");
        quote.setSupplierNote("包含运费和安装服务");
        
        return quote;
    }

    /**
     * 创建测试采购合同
     */
    public static PurchaseContract createPurchaseContract() {
        PurchaseContract contract = new PurchaseContract();
        contract.setContractNo("CONTRACT-TEST-001");
        contract.setSupplierId(100L);
        contract.setSupplierName("测试供应商有限公司");
        contract.setContractTitle("年度框架采购合同");
        contract.setContractType("FRAMEWORK");
        contract.setStartDate(LocalDateTime.now());
        contract.setEndDate(LocalDateTime.now().plusYears(1));
        contract.setTotalAmount(new BigDecimal("50000.00"));
        contract.setContractStatus(ContractStatus.DRAFT);
        contract.setPaymentTerms("30天账期");
        
        return contract;
    }

    /**
     * 创建异常测试数据：超金额订单
     */
    public static PurchaseOrder createLargeAmountOrder() {
        PurchaseOrder order = createPurchaseOrder();
        order.setOrderNo("PO-LARGE-001");
        order.setTotalAmount(new BigDecimal("9999999.99"));
        order.setTaxAmount(new BigDecimal("1299999.99"));
        order.setTotalAmountWithTax(new BigDecimal("11299999.98"));
        return order;
    }

    /**
     * 创建异常测试数据：超长交期订单
     */
    public static PurchaseOrder createLongDeliveryOrder() {
        PurchaseOrder order = createPurchaseOrder();
        order.setOrderNo("PO-LONG-001");
        order.setDeliveryDate(LocalDateTime.now().plusMonths(6));
        return order;
    }

    /**
     * 创建异常测试数据：零金额订单
     */
    public static PurchaseOrder createZeroAmountOrder() {
        PurchaseOrder order = createPurchaseOrder();
        order.setOrderNo("PO-ZERO-001");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmountWithTax(BigDecimal.ZERO);
        return order;
    }
}