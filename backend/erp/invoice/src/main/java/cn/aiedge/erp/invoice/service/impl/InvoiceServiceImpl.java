package cn.aiedge.erp.invoice.service.impl;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import cn.aiedge.erp.invoice.repository.InvoiceRepository;
import cn.aiedge.erp.invoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票服务实现类
 */
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Override
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Override
    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Page<Invoice> getInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Override
    public List<Invoice> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByInvoiceStatus(status);
    }

    @Override
    public List<Invoice> getInvoicesByPaymentStatus(PaymentStatus paymentStatus) {
        return invoiceRepository.findByPaymentStatus(paymentStatus);
    }

    @Override
    public List<Invoice> getInvoicesByCustomerId(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Invoice> getInvoicesBySupplierId(Long supplierId) {
        return invoiceRepository.findBySupplierId(supplierId);
    }

    @Override
    public List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByInvoiceDateBetween(startDate, endDate);
    }

    @Override
    public List<Invoice> getInvoicesByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        return invoiceRepository.findByAmountRange(minAmount, maxAmount);
    }

    @Override
    public Invoice createInvoiceFromApplication(InvoiceApplication application, Long issuedBy, String issuedByName) {
        // 创建发票对象
        Invoice invoice = new Invoice();
        
        // 复制申请信息到发票
        invoice.setApplicationId(application.getId());
        invoice.setInvoiceType(application.getInvoiceType());
        invoice.setCustomerId(application.getCustomerId());
        invoice.setCustomerName(application.getCustomerName());
        invoice.setCustomerTaxNumber(application.getCustomerTaxNumber());
        invoice.setCustomerAddress(application.getCustomerAddress());
        invoice.setCustomerPhone(application.getCustomerPhone());
        invoice.setCustomerBankAccount(application.getCustomerBankAccount());
        invoice.setSupplierId(application.getSupplierId());
        invoice.setSupplierName(application.getSupplierName());
        invoice.setSupplierTaxNumber(application.getSupplierTaxNumber());
        invoice.setSupplierAddress(application.getSupplierAddress());
        invoice.setSupplierPhone(application.getSupplierPhone());
        invoice.setSupplierBankAccount(application.getSupplierBankAccount());
        invoice.setCurrencyCode(application.getCurrencyCode());
        invoice.setExchangeRate(application.getExchangeRate());
        invoice.setSubtotalAmount(application.getSubtotalAmount());
        invoice.setTaxAmount(application.getTaxAmount());
        invoice.setDiscountAmount(application.getDiscountAmount());
        invoice.setShippingAmount(application.getShippingAmount());
        invoice.setOtherAmount(application.getOtherAmount());
        invoice.setTotalAmount(application.getTotalAmount());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(application.getExpectedPaymentDate() != null ? 
                          application.getExpectedPaymentDate() : LocalDate.now().plusDays(30));
        invoice.setIssuedBy(issuedBy);
        invoice.setIssuedByName(issuedByName);
        invoice.setIssuedAt(java.time.LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        
        // 生成发票号码
        invoice.setInvoiceNumber(generateInvoiceNumber());
        
        // 保存发票
        return invoiceRepository.save(invoice);
    }

    @Override
    public boolean updateInvoiceStatus(Long invoiceId, InvoiceStatus newStatus, String notes) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.setInvoiceStatus(newStatus);
            if (notes != null && !notes.trim().isEmpty()) {
                invoice.setNotes(notes);
            }
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean updatePaymentStatus(Long invoiceId, PaymentStatus newStatus, String notes) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.setPaymentStatus(newStatus);
            if (notes != null && !notes.trim().isEmpty()) {
                invoice.setNotes(notes);
            }
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean recordPayment(Long invoiceId, BigDecimal amount, String paymentReference, String notes) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.recordPayment(amount);
            if (paymentReference != null && !paymentReference.trim().isEmpty()) {
                invoice.setNotes(invoice.getNotes() + "\n付款参考号: " + paymentReference);
            }
            if (notes != null && !notes.trim().isEmpty()) {
                invoice.setNotes(invoice.getNotes() + "\n" + notes);
            }
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean recordPartialRefund(Long invoiceId, BigDecimal amount, String refundReference, String reason) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            // 记录部分退款逻辑
            invoice.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            if (refundReference != null && !refundReference.trim().isEmpty()) {
                invoice.setNotes(invoice.getNotes() + "\n退款参考号: " + refundReference);
            }
            if (reason != null && !reason.trim().isEmpty()) {
                invoice.setNotes(invoice.getNotes() + "\n退款原因: " + reason);
            }
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean recordFullRefund(Long invoiceId, String refundReference, String reason) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.setPaymentStatus(PaymentStatus.REFUNDED);
            if (refundReference != null && !refundReference.trim().isEmpty()) {
                invoice.setNotes(invoice.getNotes() + "\n退款参考号: " + refundReference);
            }
            if (reason != null && !reason.trim().isEmpty()) {
                invoice.setNotes(invoice.getNotes() + "\n退款原因: " + reason);
            }
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean voidInvoice(Long invoiceId, String reason, Long voidedBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
            invoice.setNotes("作废原因: " + reason);
            invoice.setDeletedBy(voidedBy.toString());
            invoice.setDeletedAt(java.time.LocalDateTime.now());
            invoice.setDeleted(true);
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public Invoice creditInvoice(Long invoiceId, String reason, Long creditedBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice originalInvoice = optionalInvoice.get();
            
            // 创建冲红发票
            Invoice creditInvoice = new Invoice();
            creditInvoice.setOriginalInvoiceId(originalInvoice.getId());
            creditInvoice.setInvoiceType(originalInvoice.getInvoiceType());
            creditInvoice.setCustomerId(originalInvoice.getCustomerId());
            creditInvoice.setCustomerName(originalInvoice.getCustomerName());
            creditInvoice.setCustomerTaxNumber(originalInvoice.getCustomerTaxNumber());
            creditInvoice.setCustomerAddress(originalInvoice.getCustomerAddress());
            creditInvoice.setCustomerPhone(originalInvoice.getCustomerPhone());
            creditInvoice.setCustomerBankAccount(originalInvoice.getCustomerBankAccount());
            creditInvoice.setSupplierId(originalInvoice.getSupplierId());
            creditInvoice.setSupplierName(originalInvoice.getSupplierName());
            creditInvoice.setSupplierTaxNumber(originalInvoice.getSupplierTaxNumber());
            creditInvoice.setSupplierAddress(originalInvoice.getSupplierAddress());
            creditInvoice.setSupplierPhone(originalInvoice.getSupplierPhone());
            creditInvoice.setSupplierBankAccount(originalInvoice.getSupplierBankAccount());
            creditInvoice.setCurrencyCode(originalInvoice.getCurrencyCode());
            creditInvoice.setExchangeRate(originalInvoice.getExchangeRate());
            creditInvoice.setSubtotalAmount(originalInvoice.getSubtotalAmount().negate());
            creditInvoice.setTaxAmount(originalInvoice.getTaxAmount().negate());
            creditInvoice.setDiscountAmount(originalInvoice.getDiscountAmount() != null ? 
                                          originalInvoice.getDiscountAmount().negate() : BigDecimal.ZERO);
            creditInvoice.setShippingAmount(originalInvoice.getShippingAmount() != null ? 
                                          originalInvoice.getShippingAmount().negate() : BigDecimal.ZERO);
            creditInvoice.setOtherAmount(originalInvoice.getOtherAmount() != null ? 
                                       originalInvoice.getOtherAmount().negate() : BigDecimal.ZERO);
            creditInvoice.setTotalAmount(originalInvoice.getTotalAmount().negate());
            creditInvoice.setInvoiceDate(LocalDate.now());
            creditInvoice.setDueDate(originalInvoice.getDueDate());
            creditInvoice.setIssuedBy(creditedBy);
            creditInvoice.setIssuedByName("系统");
            creditInvoice.setIssuedAt(java.time.LocalDateTime.now());
            creditInvoice.setInvoiceStatus(InvoiceStatus.CREDITED);
            creditInvoice.setPaymentStatus(PaymentStatus.PAID);
            creditInvoice.setIsCreditNote(true);
            creditInvoice.setCreditReason(reason);
            creditInvoice.setInvoiceNumber(generateInvoiceNumber());
            
            // 保存冲红发票
            Invoice savedCreditInvoice = invoiceRepository.save(creditInvoice);
            
            // 更新原发票状态
            originalInvoice.setInvoiceStatus(InvoiceStatus.CREDITED);
            originalInvoice.setNotes("已冲红，冲红发票ID: " + savedCreditInvoice.getId());
            invoiceRepository.save(originalInvoice);
            
            return savedCreditInvoice;
        }
        return null;
    }

    @Override
    public boolean sendInvoice(Long invoiceId, String sendMethod, Long sentBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            if (invoice.canSend()) {
                invoice.setSendMethod(sendMethod);
                invoice.setSentBy(sentBy);
                invoice.setSentAt(java.time.LocalDateTime.now());
                invoice.setSendStatus("SENT");
                invoice.setInvoiceStatus(InvoiceStatus.SENT);
                invoiceRepository.save(invoice);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean resendInvoice(Long invoiceId, String sendMethod, Long sentBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.setSendMethod(sendMethod);
            invoice.setSentBy(sentBy);
            invoice.setSentAt(java.time.LocalDateTime.now());
            invoice.setSendStatus("SENT");
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public byte[] downloadInvoiceDocument(Long invoiceId) {
        // 这里应该调用文档生成服务
        // 为了简化，返回空字节数组
        return new byte[0];
    }

    @Override
    public String generateInvoiceNumber() {
        // 生成发票号码的逻辑
        // 格式: INV-YYYYMMDD-XXXXX
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequencePart = String.format("%05d", (int)(Math.random() * 100000));
        return "INV-" + datePart + "-" + sequencePart;
    }

    @Override
    public boolean validateInvoice(Invoice invoice) {
        return invoice.validate();
    }

    @Override
    public boolean checkInvoiceOverdue(Invoice invoice) {
        if (invoice.getDueDate() != null && invoice.getPaymentStatus() != PaymentStatus.PAID) {
            return LocalDate.now().isAfter(invoice.getDueDate());
        }
        return false;
    }

    @Override
    public BigDecimal calculateLateFee(Invoice invoice) {
        return calculateLateFee(invoice, new BigDecimal("0.0005")); // 默认日罚息率0.05%
    }

    @Override
    public BigDecimal calculateLateFee(Invoice invoice, BigDecimal dailyRate) {
        if (checkInvoiceOverdue(invoice)) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
            BigDecimal lateFee = invoice.getUnpaidAmount()
                    .multiply(dailyRate)
                    .multiply(BigDecimal.valueOf(overdueDays));
            return lateFee.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public boolean updateOverdueStatus(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            if (checkInvoiceOverdue(invoice)) {
                invoice.setPaymentStatus(PaymentStatus.OVERDUE);
                invoice.setOverdueDays((int) java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now()));
                invoice.setLateFeeAmount(calculateLateFee(invoice));
                invoiceRepository.save(invoice);
                return true;
            }
        }
        return false;
    }

    @Override
    public int batchUpdateOverdueStatus() {
        return invoiceRepository.batchUpdateOverdueStatus();
    }

    @Override
    public String getInvoiceSummary(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            return String.format("发票[%s] - %s - 金额: %.2f %s - 状态: %s", 
                               invoice.getInvoiceNumber(),
                               invoice.getPartyDisplayName(),
                               invoice.getTotalAmount(),
                               invoice.getCurrencyCode(),
                               invoice.getStatusDisplay());
        }
        return "发票不存在";
    }

    @Override
    public InvoiceStatistics getInvoiceStatistics(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = getInvoicesByDateRange(startDate, endDate);
        InvoiceStatistics statistics = new InvoiceStatistics();
        for (Invoice invoice : invoices) {
            statistics.addInvoice(invoice);
        }
        return statistics;
    }

    @Override
    public List<Invoice> searchInvoices(String keyword) {
        return invoiceRepository.searchInvoices(keyword);
    }

    @Override
    public byte[] exportInvoices(List<Long> invoiceIds, String format) {
        // 导出逻辑
        return new byte[0];
    }

    @Override
    public List<Invoice> batchGenerateInvoices(List<Long> applicationIds, Long issuedBy, String issuedByName) {
        // 批量生成发票逻辑
        return List.of();
    }

    @Override
    public boolean deleteInvoice(Long invoiceId, String deletedBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.softDelete(deletedBy);
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean restoreInvoice(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (optionalInvoice.isPresent()) {
            Invoice invoice = optionalInvoice.get();
            invoice.restore();
            invoiceRepository.save(invoice);
            return true;
        }
        return false;
    }

    @Override
    public boolean permanentlyDeleteInvoice(Long invoiceId) {
        invoiceRepository.deleteById(invoiceId);
        return true;
    }
}