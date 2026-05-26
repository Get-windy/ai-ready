package cn.aiedge.erp.testing;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.entity.SerialNumber;
import cn.aiedge.erp.batchsn.enums.BatchStatusEnum;
import cn.aiedge.erp.batchsn.enums.SnStatusEnum;
import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.enums.InquiryStatus;
import cn.aiedge.erp.purchase.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * P0核心ERP功能测试数据生成工具
 * 
 * 为批次/序列号管理、采购询价/报价管理模块生成测试数据
 * 
 * @author team-member
 * @date 2026-04-29
 */
public class TestDataGenerator {

    private static int batchCounter = 0;
    private static int serialCounter = 0;
    private static int inquiryCounter = 0;
    private static int quoteCounter = 0;

    /**
     * 生成批次号测试数据
     */
    public static BatchNumber generateBatchNumber() {
        batchCounter++;
        BatchNumber batch = new BatchNumber();
        batch.setBatchNo("B" + System.currentTimeMillis() + String.format("%04d", batchCounter));
        batch.setProductCode("P" + String.format("%03d", (batchCounter % 100) + 1));
        batch.setProductName("测试产品-" + batchCounter);
        batch.setQuantity(new BigDecimal(100 + batchCounter * 10));
        batch.setStatus(BatchStatusEnum.ACTIVE);
        batch.setSourceType("PURCHASE");
        batch.setSourceId((long) batchCounter);
        batch.setWarehouseId(1L);
        batch.setWarehouseName("主仓库");
        batch.setLocationId(1L);
        batch.setProductionDate(LocalDateTime.now().minusDays(30));
        batch.setExpiryDate(LocalDateTime.now().plusDays(180));
        batch.setCreateTime(LocalDateTime.now());
        return batch;
    }

    /**
     * 生成批次号列表
     */
    public static List<BatchNumber> generateBatchNumbers(int count) {
        List<BatchNumber> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateBatchNumber());
        }
        return list;
    }

    /**
     * 生成特定状态的批次号
     */
    public static BatchNumber generateBatchNumberWithStatus(BatchStatusEnum status) {
        BatchNumber batch = generateBatchNumber();
        batch.setStatus(status);
        if (status == BatchStatusEnum.EXPIRED) {
            batch.setExpiryDate(LocalDateTime.now().minusDays(10));
        }
        return batch;
    }

    /**
     * 生成序列号测试数据
     */
    public static SerialNumber generateSerialNumber() {
        serialCounter++;
        SerialNumber serial = new SerialNumber();
        serial.setSerialNo("SN" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        serial.setProductCode("P" + String.format("%03d", (serialCounter % 100) + 1));
        serial.setProductName("测试产品-" + serialCounter);
        serial.setStatus(SnStatusEnum.AVAILABLE);
        serial.setBatchNo("B20260429001");
        serial.setWarehouseId(1L);
        serial.setWarehouseName("主仓库");
        serial.setLocationId(1L);
        serial.setWarrantyStartDate(LocalDateTime.now());
        serial.setWarrantyEndDate(LocalDateTime.now().plusYears(1));
        serial.setCreateTime(LocalDateTime.now());
        return serial;
    }

    /**
     * 生成序列号列表
     */
    public static List<SerialNumber> generateSerialNumbers(int count) {
        List<SerialNumber> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateSerialNumber());
        }
        return list;
    }

    /**
     * 生成询价单测试数据
     */
    public static PurchaseInquiry generateInquiry() {
        inquiryCounter++;
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setInquiryNo("INQ" + System.currentTimeMillis() + String.format("%04d", inquiryCounter));
        inquiry.setTitle("采购询价-" + inquiryCounter);
        inquiry.setDescription("测试用询价单描述");
        inquiry.setStatus(InquiryStatus.DRAFT);
        inquiry.setPurchaserId(100L + inquiryCounter);
        inquiry.setPurchaserName("采购员" + inquiryCounter);
        inquiry.setDepartmentId(10L);
        inquiry.setDepartmentName("采购部");
        inquiry.setCurrency("CNY");
        inquiry.setDeadline(LocalDateTime.now().plusDays(7));
        inquiry.setCreateTime(LocalDateTime.now());
        return inquiry;
    }

    /**
     * 生成询价单列表
     */
    public static List<PurchaseInquiry> generateInquiries(int count) {
        List<PurchaseInquiry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateInquiry());
        }
        return list;
    }

    /**
     * 生成报价测试数据
     */
    public static PurchaseSupplierQuote generateQuote(Long inquiryId, Long supplierId) {
        quoteCounter++;
        PurchaseSupplierQuote quote = new PurchaseSupplierQuote();
        quote.setQuoteNo("QUO" + System.currentTimeMillis() + String.format("%04d", quoteCounter));
        quote.setInquiryId(inquiryId);
        quote.setSupplierId(supplierId);
        quote.setSupplierName("供应商-" + supplierId);
        quote.setTotalAmount(new BigDecimal(10000 + quoteCounter * 1000));
        quote.setTaxAmount(new BigDecimal(1000 + quoteCounter * 100));
        quote.setTotalWithTax(quote.getTotalAmount().add(quote.getTaxAmount()));
        quote.setStatus(QuoteStatus.SUBMITTED);
        quote.setCurrency("CNY");
        quote.setValidUntil(LocalDateTime.now().plusDays(30));
        quote.setCreateTime(LocalDateTime.now());
        return quote;
    }

    /**
     * 生成报价列表
     */
    public static List<PurchaseSupplierQuote> generateQuotes(int count, Long inquiryId) {
        List<PurchaseSupplierQuote> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(generateQuote(inquiryId, 50L + i));
        }
        return list;
    }

    /**
     * 生成临期批次（用于预警测试）
     */
    public static List<BatchNumber> generateExpiringBatches(int count, int daysToExpiry) {
        List<BatchNumber> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BatchNumber batch = generateBatchNumber();
            batch.setExpiryDate(LocalDateTime.now().plusDays(daysToExpiry));
            list.add(batch);
        }
        return list;
    }

    /**
     * 生成质保即将到期序列号
     */
    public static List<SerialNumber> generateWarrantyExpiringSerials(int count, int daysToExpiry) {
        List<SerialNumber> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SerialNumber serial = generateSerialNumber();
            serial.setWarrantyEndDate(LocalDateTime.now().plusDays(daysToExpiry));
            list.add(serial);
        }
        return list;
    }

    public static void main(String[] args) {
        System.out.println("=== P0核心ERP功能测试数据生成工具 ===");
        System.out.println();
        
        System.out.println("1. 批次号测试数据:");
        BatchNumber batch = generateBatchNumber();
        System.out.println("   批次号: " + batch.getBatchNo());
        System.out.println("   产品编码: " + batch.getProductCode());
        System.out.println("   数量: " + batch.getQuantity());
        System.out.println();
        
        System.out.println("2. 序列号测试数据:");
        SerialNumber serial = generateSerialNumber();
        System.out.println("   序列号: " + serial.getSerialNo());
        System.out.println("   状态: " + serial.getStatus());
        System.out.println();
        
        System.out.println("3. 询价单测试数据:");
        PurchaseInquiry inquiry = generateInquiry();
        System.out.println("   询价单号: " + inquiry.getInquiryNo());
        System.out.println("   标题: " + inquiry.getTitle());
        System.out.println();
        
        System.out.println("4. 报价测试数据:");
        PurchaseSupplierQuote quote = generateQuote(1L, 50L);
        System.out.println("   报价单号: " + quote.getQuoteNo());
        System.out.println("   金额: " + quote.getTotalAmount());
        System.out.println();
        
        System.out.println("5. 批量生成测试:");
        List<BatchNumber> batches = generateBatchNumbers(5);
        System.out.println("   生成批次数量: " + batches.size());
        List<SerialNumber> serials = generateSerialNumbers(5);
        System.out.println("   生成序列号数量: " + serials.size());
        List<PurchaseInquiry> inquiries = generateInquiries(3);
        System.out.println("   生成询价单数量: " + inquiries.size());
        List<PurchaseSupplierQuote> quotes = generateQuotes(3, 1L);
        System.out.println("   生成报价数量: " + quotes.size());
        System.out.println();
        
        System.out.println("=== 测试数据生成完成 ===");
    }
}
