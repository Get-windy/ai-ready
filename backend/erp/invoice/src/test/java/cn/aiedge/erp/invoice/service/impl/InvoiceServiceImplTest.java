package cn.aiedge.erp.invoice.service.impl;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import cn.aiedge.erp.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InvoiceServiceImplTest {

    @Autowired
    private InvoiceServiceImpl invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    public void testCreateAndRetrieveInvoice() {
        // 创建发票
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("TEST-001");
        invoice.setCustomerId(1L);
        invoice.setCustomerName("Test Customer");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setSubtotalAmount(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("100.00"));
        invoice.setTotalAmount(new BigDecimal("1100.00"));
        invoice.setIssuedBy(1L);
        invoice.setIssuedByName("Test User");
        invoice.setIssuedAt(java.time.LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        // 保存发票
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 验证发票是否正确保存
        assertNotNull(savedInvoice.getId());

        // 通过ID检索发票
        Optional<Invoice> retrievedInvoice = invoiceService.getInvoiceById(savedInvoice.getId());
        assertTrue(retrievedInvoice.isPresent());
        assertEquals("TEST-001", retrievedInvoice.get().getInvoiceNumber());
        assertEquals("Test Customer", retrievedInvoice.get().getCustomerName());
        assertEquals(new BigDecimal("1100.00"), retrievedInvoice.get().getTotalAmount());
    }

    @Test
    public void testUpdateInvoiceStatus() {
        // 创建发票
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("TEST-002");
        invoice.setCustomerId(1L);
        invoice.setCustomerName("Test Customer 2");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setSubtotalAmount(new BigDecimal("2000.00"));
        invoice.setTaxAmount(new BigDecimal("200.00"));
        invoice.setTotalAmount(new BigDecimal("2200.00"));
        invoice.setIssuedBy(1L);
        invoice.setIssuedByName("Test User");
        invoice.setIssuedAt(java.time.LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 更新发票状态
        boolean result = invoiceService.updateInvoiceStatus(savedInvoice.getId(), InvoiceStatus.SENT, "Test update");
        assertTrue(result);

        // 验证状态已更新
        Optional<Invoice> updatedInvoice = invoiceService.getInvoiceById(savedInvoice.getId());
        assertTrue(updatedInvoice.isPresent());
        assertEquals(InvoiceStatus.SENT, updatedInvoice.get().getInvoiceStatus());
    }

    @Test
    public void testRecordPayment() {
        // 创建发票
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("TEST-003");
        invoice.setCustomerId(1L);
        invoice.setCustomerName("Test Customer 3");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setSubtotalAmount(new BigDecimal("3000.00"));
        invoice.setTaxAmount(new BigDecimal("300.00"));
        invoice.setTotalAmount(new BigDecimal("3300.00"));
        invoice.setIssuedBy(1L);
        invoice.setIssuedByName("Test User");
        invoice.setIssuedAt(java.time.LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 记录付款
        boolean result = invoiceService.recordPayment(savedInvoice.getId(), new BigDecimal("3300.00"), "PAY-001", "Full payment");
        assertTrue(result);

        // 验证付款状态已更新
        Optional<Invoice> paidInvoice = invoiceService.getInvoiceById(savedInvoice.getId());
        assertTrue(paidInvoice.isPresent());
        assertEquals(PaymentStatus.PAID, paidInvoice.get().getPaymentStatus());
        assertEquals(new BigDecimal("3300.00"), paidInvoice.get().getPaidAmount());
        assertEquals(new BigDecimal("0.00"), paidInvoice.get().getUnpaidAmount());
    }
}