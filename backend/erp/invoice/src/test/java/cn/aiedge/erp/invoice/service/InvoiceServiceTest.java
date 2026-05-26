package cn.aiedge.erp.invoice.service;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.InvoiceType;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import cn.aiedge.erp.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 发票服务测试类
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
    
    @Mock
    private InvoiceRepository invoiceRepository;
    
    @InjectMocks
    private InvoiceService invoiceService;
    
    private Invoice testInvoice;
    private InvoiceApplication testApplication;
    
    @BeforeEach
    void setUp() {
        // 创建测试发票申请
        testApplication = new InvoiceApplication();
        testApplication.setId(1L);
        testApplication.setApplicationNumber("APP-2026-001");
        testApplication.setInvoiceType(InvoiceType.SALES_INVOICE);
        testApplication.setStatus(InvoiceStatus.APPROVED);
        testApplication.setCustomerId(1001L);
        testApplication.setCustomerName("测试客户");
        testApplication.setSubtotalAmount(new BigDecimal("1000.00"));
        testApplication.setTaxAmount(new BigDecimal("130.00"));
        testApplication.setTotalAmount(new BigDecimal("1130.00"));
        testApplication.setApplicantId(2001L);
        testApplication.setApplicationDate(LocalDate.now());
        
        // 创建测试发票
        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setInvoiceNumber("INV-2026-001");
        testInvoice.setApplicationId(1L);
        testInvoice.setInvoiceType(InvoiceType.SALES_INVOICE);
        testInvoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        testInvoice.setPaymentStatus(PaymentStatus.PENDING);
        testInvoice.setInvoiceDate(LocalDate.now());
        testInvoice.setDueDate(LocalDate.now().plusDays(30));
        testInvoice.setCustomerId(1001L);
        testInvoice.setCustomerName("测试客户");
        testInvoice.setCustomerTaxNumber("91310101MA1FL0P123");
        testInvoice.setSubtotalAmount(new BigDecimal("1000.00"));
        testInvoice.setTaxAmount(new BigDecimal("130.00"));
        testInvoice.setTotalAmount(new BigDecimal("1130.00"));
        testInvoice.setUnpaidAmount(new BigDecimal("1130.00"));
        testInvoice.setIssuedBy(3001L);
        testInvoice.setIssuedByName("系统管理员");
        testInvoice.setIssuedAt(LocalDateTime.now());
        testInvoice.setDocumentPath("/invoices/INV-2026-001.pdf");
        testInvoice.setDocumentFormat("PDF");
        testInvoice.setQrcodeData("INV-2026-001|2026-04-29|1130.00|91310101MA1FL0P123");
    }
    
    @Test
    void testCreateInvoiceFromApplication_Success() {
        // 准备
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        
        // 执行
        Invoice result = invoiceService.createInvoiceFromApplication(testApplication, 3001L, "系统管理员");
        
        // 验证
        assertNotNull(result);
        assertEquals("INV-2026-001", result.getInvoiceNumber());
        assertEquals(InvoiceStatus.GENERATED, result.getInvoiceStatus());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        assertEquals(new BigDecimal("1130.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("1130.00"), result.getUnpaidAmount());
        assertNotNull(result.getIssuedAt());
        
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }
    
    @Test
    void testCreateInvoiceFromApplication_InvalidApplication() {
        // 准备 - 创建无效的申请（状态不是已批准）
        testApplication.setStatus(InvoiceStatus.DRAFT);
        
        // 执行和验证
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.createInvoiceFromApplication(testApplication, 3001L, "系统管理员");
        });
        
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
    
    @Test
    void testGetInvoiceById_Found() {
        // 准备
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        
        // 执行
        Optional<Invoice> result = invoiceService.getInvoiceById(1L);
        
        // 验证
        assertTrue(result.isPresent());
        assertEquals("INV-2026-001", result.get().getInvoiceNumber());
        assertEquals(InvoiceStatus.GENERATED, result.get().getInvoiceStatus());
        
        verify(invoiceRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetInvoiceById_NotFound() {
        // 准备
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
        
        // 执行
        Optional<Invoice> result = invoiceService.getInvoiceById(999L);
        
        // 验证
        assertFalse(result.isPresent());
        
        verify(invoiceRepository, times(1)).findById(999L);
    }
    
    @Test
    void testUpdateInvoiceStatus_Success() {
        // 准备
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        
        // 执行
        boolean result = invoiceService.updateInvoiceStatus(1L, InvoiceStatus.SENT, "已发送给客户");
        
        // 验证
        assertTrue(result);
        assertEquals(InvoiceStatus.SENT, testInvoice.getInvoiceStatus());
        
        verify(invoiceRepository, times(1)).findById(1L);
        verify(invoiceRepository, times(1)).save(testInvoice);
    }
    
    @Test
    void testUpdateInvoiceStatus_InvalidTransition() {
        // 准备 - 设置发票为已支付状态
        testInvoice.setInvoiceStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        
        // 执行和验证 - 尝试从已支付状态转移到已发送状态（无效）
        assertThrows(IllegalStateException.class, () -> {
            invoiceService.updateInvoiceStatus(1L, InvoiceStatus.SENT, "尝试无效状态转移");
        });
        
        verify(invoiceRepository, times(1)).findById(1L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
    
    @Test
    void testRecordPayment_Success() {
        // 准备
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        
        // 执行
        boolean result = invoiceService.recordPayment(1L, new BigDecimal("500.00"), "BANK-001", "部分付款");
        
        // 验证
        assertTrue(result);
        assertEquals(new BigDecimal("500.00"), testInvoice.getPaidAmount());
        assertEquals(new BigDecimal("630.00"), testInvoice.getUnpaidAmount());
        assertEquals(PaymentStatus.PARTIALLY_PAID, testInvoice.getPaymentStatus());
        
        verify(invoiceRepository, times(1)).findById(1L);
        verify(invoiceRepository, times(1)).save(testInvoice);
    }
    
    @Test
    void testRecordPayment_FullPayment() {
        // 准备
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        
        // 执行
        boolean result = invoiceService.recordPayment(1L, new BigDecimal("1130.00"), "BANK-002", "全额付款");
        
        // 验证
        assertTrue(result);
        assertEquals(new BigDecimal("1130.00"), testInvoice.getPaidAmount());
        assertEquals(BigDecimal.ZERO, testInvoice.getUnpaidAmount());
        assertEquals(PaymentStatus.PAID, testInvoice.getPaymentStatus());
        
        verify(invoiceRepository, times(1)).findById(1L);
        verify(invoiceRepository, times(1)).save(testInvoice);
    }
    
    @Test
    void testRecordPayment_Overpayment() {
        // 准备
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        
        // 执行和验证 - 尝试超额支付
        assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.recordPayment(1L, new BigDecimal("2000.00"), "BANK-003", "超额付款");
        });
        
        verify(invoiceRepository, times(1)).findById(1L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
    
    @Test
    void testCalculateLateFee_NoOverdue() {
        // 准备 - 发票未逾期
        testInvoice.setDueDate(LocalDate.now().plusDays(10));
        
        // 执行
        BigDecimal lateFee = invoiceService.calculateLateFee(testInvoice);
        
        // 验证
        assertEquals(BigDecimal.ZERO, lateFee);
    }
    
    @Test
    void testCalculateLateFee_Overdue() {
        // 准备 - 发票逾期30天
        testInvoice.setDueDate(LocalDate.now().minusDays(30));
        testInvoice.setOverdueDays(30);
        testInvoice.setPaymentStatus(PaymentStatus.OVERDUE);
        
        // 执行 - 假设日罚息率为0.0005
        BigDecimal lateFee = invoiceService.calculateLateFee(testInvoice, new BigDecimal("0.0005"));
        
        // 验证 - 1130 * 0.0005 * 30 = 16.95
        assertEquals(new BigDecimal("16.95"), lateFee.setScale(2, BigDecimal.ROUND_HALF_UP));
    }
    
    @Test
    void testValidateInvoice_Valid() {
        // 执行
        boolean isValid = invoiceService.validateInvoice(testInvoice);
        
        // 验证
        assertTrue(isValid);
    }
    
    @Test
    void testValidateInvoice_InvalidMissingFields() {
        // 准备 - 创建无效发票（缺少必要字段）
        testInvoice.setInvoiceNumber(null);
        testInvoice.setCustomerName(null);
        
        // 执行
        boolean isValid = invoiceService.validateInvoice(testInvoice);
        
        // 验证
        assertFalse(isValid);
    }
    
    @Test
    void testValidateInvoice_InvalidAmounts() {
        // 准备 - 创建无效发票（金额为负）
        testInvoice.setTotalAmount(new BigDecimal("-100.00"));
        
        // 执行
        boolean isValid = invoiceService.validateInvoice(testInvoice);
        
        // 验证
        assertFalse(isValid);
    }
    
    @Test
    void testGenerateInvoiceNumber() {
        // 准备
        when(invoiceRepository.countByInvoiceDate(any(LocalDate.class))).thenReturn(5L);
        
        // 执行
        String invoiceNumber = invoiceService.generateInvoiceNumber();
        
        // 验证
        assertNotNull(invoiceNumber);
        assertTrue(invoiceNumber.startsWith("INV-"));
        assertTrue(invoiceNumber.contains("2026"));
        
        verify(invoiceRepository, times(1)).countByInvoiceDate(any(LocalDate.class));
    }
    
    @Test
    void testCheckInvoiceOverdue_NotOverdue() {
        // 准备 - 发票未到期
        testInvoice.setDueDate(LocalDate.now().plusDays(10));
        
        // 执行
        boolean isOverdue = invoiceService.checkInvoiceOverdue(testInvoice);
        
        // 验证
        assertFalse(isOverdue);
        assertEquals(0, testInvoice.getOverdueDays());
        assertNotEquals(PaymentStatus.OVERDUE, testInvoice.getPaymentStatus());
    }
    
    @Test
    void testCheckInvoiceOverdue_Overdue() {
        // 准备 - 发票已逾期
        testInvoice.setDueDate(LocalDate.now().minusDays(5));
        testInvoice.setPaymentStatus(PaymentStatus.PENDING);
        
        // 执行
        boolean isOverdue = invoiceService.checkInvoiceOverdue(testInvoice);
        
        // 验证
        assertTrue(isOverdue);
        assertEquals(5, testInvoice.getOverdueDays());
        assertEquals(PaymentStatus.OVERDUE, testInvoice.getPaymentStatus());
    }
    
    @Test
    void testGetInvoiceSummary() {
        // 准备
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        
        // 执行
        String summary = invoiceService.getInvoiceSummary(1L);
        
        // 验证
        assertNotNull(summary);
        assertTrue(summary.contains("INV-2026-001"));
        assertTrue(summary.contains("测试客户"));
        assertTrue(summary.contains("1130.00"));
        
        verify(invoiceRepository, times(1)).findById(1L);
    }
}