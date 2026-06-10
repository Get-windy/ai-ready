package cn.aiedge.erp.invoice.service.impl;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.entity.PaymentRecord;
import cn.aiedge.erp.invoice.model.enums.MatchingStatus;
import cn.aiedge.erp.invoice.model.enums.MatchingType;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import cn.aiedge.erp.invoice.repository.InvoiceRepository;
import cn.aiedge.erp.invoice.repository.PaymentRecordRepository;
import cn.aiedge.erp.invoice.service.InvoiceMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 发票匹配服务实现类
 */
import cn.aiedge.erp.invoice.model.dto.MatchingStatistics;
@Slf4j
@Service
@Transactional
public class InvoiceMatchingServiceImpl implements InvoiceMatchingService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Override
    public boolean autoMatchInvoiceToPayment(Long invoiceId, Long paymentRecordId, MatchingType matchingType) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        Optional<PaymentRecord> optionalPaymentRecord = paymentRecordRepository.findById(paymentRecordId);
        
        if (!optionalInvoice.isPresent() || !optionalPaymentRecord.isPresent()) {
            return false;
        }
        
        Invoice invoice = optionalInvoice.get();
        PaymentRecord paymentRecord = optionalPaymentRecord.get();
        
        // 检查是否可以匹配
        if (!canMatchInvoice(invoice) || !paymentRecord.canMatch()) {
            return false;
        }
        
        // 检查金额是否匹配
        BigDecimal matchingAmount = paymentRecord.getUnmatchedAmount();
        BigDecimal invoiceUnmatchedAmount = getUnmatchedAmount(invoiceId);
        
        if (matchingAmount.compareTo(BigDecimal.ZERO) <= 0 || 
            invoiceUnmatchedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // 确定实际匹配金额
        BigDecimal actualMatchingAmount = matchingAmount.min(invoiceUnmatchedAmount);
        
        // 执行匹配
        return performMatching(invoice, paymentRecord, actualMatchingAmount, 
                               null, "系统自动匹配", matchingType);
    }

    @Override
    public boolean manualMatchInvoiceToPayment(Long invoiceId, Long paymentRecordId, BigDecimal matchingAmount, 
                                              Long matchedBy, String notes) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        Optional<PaymentRecord> optionalPaymentRecord = paymentRecordRepository.findById(paymentRecordId);
        
        if (!optionalInvoice.isPresent() || !optionalPaymentRecord.isPresent()) {
            return false;
        }
        
        Invoice invoice = optionalInvoice.get();
        PaymentRecord paymentRecord = optionalPaymentRecord.get();
        
        // 验证匹配金额
        if (!validateMatchingAmount(invoiceId, matchingAmount)) {
            return false;
        }
        
        // 检查付款记录是否有足够的未匹配金额
        if (paymentRecord.getUnmatchedAmount().compareTo(matchingAmount) < 0) {
            return false;
        }
        
        // 执行匹配
        return performMatching(invoice, paymentRecord, matchingAmount, 
                               matchedBy, notes, MatchingType.MANUAL);
    }

    @Override
    public boolean matchInvoiceToPurchaseOrder(Long invoiceId, Long purchaseOrderId, Long matchedBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (!optionalInvoice.isPresent()) {
            return false;
        }
        
        Invoice invoice = optionalInvoice.get();
        // 这里应该调用采购订单服务进行匹配
        // 简化实现：标记发票为已匹配采购订单
        invoice.setMatchingBatchNumber("PO-" + purchaseOrderId);
        invoice.setMatchingNotes("匹配到采购订单: " + purchaseOrderId);
        invoice.setMatchedBy(matchedBy);
        invoice.setMatchedAt(LocalDateTime.now());
        invoice.setMatchedDate(LocalDate.now());
        
        invoiceRepository.save(invoice);
        return true;
    }

    @Override
    public boolean matchInvoiceToSalesOrder(Long invoiceId, Long salesOrderId, Long matchedBy) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (!optionalInvoice.isPresent()) {
            return false;
        }
        
        Invoice invoice = optionalInvoice.get();
        // 这里应该调用销售订单服务进行匹配
        // 简化实现：标记发票为已匹配销售订单
        invoice.setMatchingBatchNumber("SO-" + salesOrderId);
        invoice.setMatchingNotes("匹配到销售订单: " + salesOrderId);
        invoice.setMatchedBy(matchedBy);
        invoice.setMatchedAt(LocalDateTime.now());
        invoice.setMatchedDate(LocalDate.now());
        
        invoiceRepository.save(invoice);
        return true;
    }

    @Override
    public boolean partialMatchInvoice(Long invoiceId, Long paymentRecordId, BigDecimal partialAmount, 
                                       Long matchedBy, String notes) {
        return manualMatchInvoiceToPayment(invoiceId, paymentRecordId, partialAmount, matchedBy, notes);
    }

    @Override
    public boolean unmatchInvoice(Long invoiceId, Long paymentRecordId, Long cancelledBy, String reason) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        Optional<PaymentRecord> optionalPaymentRecord = paymentRecordRepository.findById(paymentRecordId);
        
        if (!optionalInvoice.isPresent() || !optionalPaymentRecord.isPresent()) {
            return false;
        }
        
        Invoice invoice = optionalInvoice.get();
        PaymentRecord paymentRecord = optionalPaymentRecord.get();
        
        // 取消匹配
        paymentRecord.cancelMatching(reason);
        paymentRecordRepository.save(paymentRecord);
        
        // 重新计算发票的匹配状态
        recalculateMatchingStatus(invoiceId);
        
        return true;
    }

    @Override
    public MatchingStatus getInvoiceMatchingStatus(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (!optionalInvoice.isPresent()) {
            return MatchingStatus.MATCHING_ERROR;
        }
        
        Invoice invoice = optionalInvoice.get();
        return invoice.getMatchingStatus();
    }

    @Override
    public List<PaymentRecord> getInvoiceMatchingDetails(Long invoiceId) {
        return paymentRecordRepository.findByInvoiceId(invoiceId);
    }

    @Override
    public List<Invoice> getUnmatchedInvoices(Long customerId, Long supplierId, LocalDate startDate, LocalDate endDate) {
        if (customerId != null) {
            return invoiceRepository.findUnmatchedByCustomerId(customerId, startDate, endDate);
        } else if (supplierId != null) {
            return invoiceRepository.findUnmatchedBySupplierId(supplierId, startDate, endDate);
        } else {
            return invoiceRepository.findUnmatchedInvoices(startDate, endDate);
        }
    }

    @Override
    public List<Invoice> getPartiallyMatchedInvoices() {
        return invoiceRepository.findByMatchingStatus(MatchingStatus.PARTIALLY_MATCHED);
    }

    @Override
    public List<Invoice> getFullyMatchedInvoices() {
        return invoiceRepository.findByMatchingStatus(MatchingStatus.FULLY_MATCHED);
    }

    @Override
    public boolean canMatchInvoice(Invoice invoice) {
        if (invoice == null || invoice.getDeleted()) {
            return false;
        }
        
        // 检查发票状态
        if (!invoice.canSend()) {
            return false;
        }
        
        // 检查付款状态
        PaymentStatus paymentStatus = invoice.getPaymentStatus();
        if (paymentStatus == PaymentStatus.CANCELLED || 
            paymentStatus == PaymentStatus.REFUNDED ||
            paymentStatus == PaymentStatus.VOIDED) {
            return false;
        }
        
        // 检查匹配状态
        MatchingStatus matchingStatus = invoice.getMatchingStatus();
        if (matchingStatus == MatchingStatus.MATCHING_CANCELLED ||
            matchingStatus == MatchingStatus.MATCHING_ERROR) {
            return false;
        }
        
        // 检查是否有未匹配金额
        return getUnmatchedAmount(invoice.getId()).compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean validateMatchingAmount(Long invoiceId, BigDecimal matchingAmount) {
        if (matchingAmount == null || matchingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        BigDecimal unmatchedAmount = getUnmatchedAmount(invoiceId);
        return matchingAmount.compareTo(unmatchedAmount) <= 0;
    }

    @Override
    public BigDecimal getMatchedAmount(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (!optionalInvoice.isPresent()) {
            return BigDecimal.ZERO;
        }
        
        Invoice invoice = optionalInvoice.get();
        return invoice.getMatchedAmount() != null ? invoice.getMatchedAmount() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getUnmatchedAmount(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (!optionalInvoice.isPresent()) {
            return BigDecimal.ZERO;
        }
        
        Invoice invoice = optionalInvoice.get();
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal matchedAmount = invoice.getMatchedAmount() != null ? invoice.getMatchedAmount() : BigDecimal.ZERO;
        
        return totalAmount.subtract(matchedAmount).max(BigDecimal.ZERO);
    }

    @Override
    public int batchMatchInvoices(List<Long> invoiceIds, Long paymentRecordId, Long matchedBy) {
        int matchedCount = 0;
        
        Optional<PaymentRecord> optionalPaymentRecord = paymentRecordRepository.findById(paymentRecordId);
        if (!optionalPaymentRecord.isPresent()) {
            return 0;
        }
        
        PaymentRecord paymentRecord = optionalPaymentRecord.get();
        BigDecimal paymentUnmatchedAmount = paymentRecord.getUnmatchedAmount();
        
        if (paymentUnmatchedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        for (Long invoiceId : invoiceIds) {
            if (paymentUnmatchedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal invoiceUnmatchedAmount = getUnmatchedAmount(invoiceId);
            if (invoiceUnmatchedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            BigDecimal matchingAmount = invoiceUnmatchedAmount.min(paymentUnmatchedAmount);
            
            if (manualMatchInvoiceToPayment(invoiceId, paymentRecordId, matchingAmount, 
                                           matchedBy, "批量匹配")) {
                matchedCount++;
                paymentUnmatchedAmount = paymentUnmatchedAmount.subtract(matchingAmount);
            }
        }
        
        return matchedCount;
    }

    @Override
    public int autoMatchDueInvoices() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        
        // 查找到期未匹配的发票
        List<Invoice> dueInvoices = invoiceRepository.findDueInvoicesForAutoMatching(thirtyDaysAgo, today);
        
        int matchedCount = 0;
        for (Invoice invoice : dueInvoices) {
            // 查找合适的付款记录进行自动匹配
            List<PaymentRecord> availablePayments = paymentRecordRepository.findAvailableForAutoMatching(
                invoice.getCustomerId(), invoice.getSupplierId(), invoice.getCurrencyCode());
            
            for (PaymentRecord payment : availablePayments) {
                if (autoMatchInvoiceToPayment(invoice.getId(), payment.getId(), MatchingType.AUTO)) {
                    matchedCount++;
                    break;
                }
            }
        }
        
        return matchedCount;
    }

    @Override
    public MatchingStatus recalculateMatchingStatus(Long invoiceId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);
        if (!optionalInvoice.isPresent()) {
            return MatchingStatus.MATCHING_ERROR;
        }
        
        Invoice invoice = optionalInvoice.get();
        
        BigDecimal matchedAmount = getMatchedAmount(invoiceId);
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        
        MatchingStatus newStatus;
        if (matchedAmount.compareTo(BigDecimal.ZERO) == 0) {
            newStatus = MatchingStatus.UNMATCHED;
        } else if (matchedAmount.compareTo(totalAmount) >= 0) {
            newStatus = MatchingStatus.FULLY_MATCHED;
        } else {
            newStatus = MatchingStatus.PARTIALLY_MATCHED;
        }
        
        // 更新发票的匹配状态
        invoice.setMatchingStatus(newStatus);
        invoice.setMatchedAmount(matchedAmount);
        invoiceRepository.save(invoice);
        
        return newStatus;
    }

    @Override
    public String generateMatchingReport(LocalDate startDate, LocalDate endDate) {
        MatchingStatistics statistics = getMatchingStatistics(startDate, endDate);
        
        StringBuilder report = new StringBuilder();
        report.append("发票匹配报告\n");
        report.append("========================================\n");
        report.append(String.format("报告期间: %s 至 %s\n", startDate, endDate));
        report.append(String.format("总发票数: %d\n", statistics.getTotalInvoices()));
        report.append(String.format("未匹配发票: %d (%.2f%%)\n", 
            statistics.getUnmatchedCount(),
            statistics.getTotalInvoices() > 0 ? 
                (double)statistics.getUnmatchedCount() / statistics.getTotalInvoices() * 100 : 0));
        report.append(String.format("部分匹配发票: %d (%.2f%%)\n", 
            statistics.getPartiallyMatchedCount(),
            statistics.getTotalInvoices() > 0 ? 
                (double)statistics.getPartiallyMatchedCount() / statistics.getTotalInvoices() * 100 : 0));
        report.append(String.format("完全匹配发票: %d (%.2f%%)\n", 
            statistics.getFullyMatchedCount(),
            statistics.getTotalInvoices() > 0 ? 
                (double)statistics.getFullyMatchedCount() / statistics.getTotalInvoices() * 100 : 0));
        report.append(String.format("未匹配金额: %.2f\n", statistics.getUnmatchedAmount()));
        report.append(String.format("部分匹配金额: %.2f\n", statistics.getPartiallyMatchedAmount()));
        report.append(String.format("完全匹配金额: %.2f\n", statistics.getFullyMatchedAmount()));
        report.append(String.format("自动匹配次数: %d\n", statistics.getAutoMatches()));
        report.append(String.format("手动匹配次数: %d\n", statistics.getManualMatches()));
        
        return report.toString();
    }

    @Override
    public int cleanupOldMatchingRecords(int olderThanDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(olderThanDays);
        return paymentRecordRepository.deleteOldRecords(cutoffDate);
    }

    @Override
    public MatchingStatistics getMatchingStatistics(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = invoiceRepository.findByInvoiceDateBetween(startDate, endDate);
        
        MatchingStatistics statistics = new MatchingStatistics();
        for (Invoice invoice : invoices) {
            statistics.addInvoice(invoice);
        }
        
        // 获取匹配次数统计
        int autoMatches = paymentRecordRepository.countAutoMatches(startDate, endDate);
        int manualMatches = paymentRecordRepository.countManualMatches(startDate, endDate);
        statistics.setAutoMatches(autoMatches);
        statistics.setManualMatches(manualMatches);
        
        return statistics;
    }
    
    /**
     * 执行匹配操作
     */
    private boolean performMatching(Invoice invoice, PaymentRecord paymentRecord, BigDecimal matchingAmount,
                                   Long matchedBy, String notes, MatchingType matchingType) {
        try {
            // 更新付款记录
            paymentRecord.markAsMatched(matchedBy, "系统", matchingType, matchingAmount);
            if (notes != null && !notes.trim().isEmpty()) {
                paymentRecord.setMatchingNotes(notes);
            }
            paymentRecordRepository.save(paymentRecord);
            
            // 更新发票
            BigDecimal newMatchedAmount = invoice.getMatchedAmount() != null ? 
                invoice.getMatchedAmount().add(matchingAmount) : matchingAmount;
            invoice.setMatchedAmount(newMatchedAmount);
            
            // 重新计算匹配状态
            BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            MatchingStatus newMatchingStatus;
            if (newMatchedAmount.compareTo(totalAmount) >= 0) {
                newMatchingStatus = MatchingStatus.FULLY_MATCHED;
            } else if (newMatchedAmount.compareTo(BigDecimal.ZERO) > 0) {
                newMatchingStatus = MatchingStatus.PARTIALLY_MATCHED;
            } else {
                newMatchingStatus = MatchingStatus.UNMATCHED;
            }
            
            invoice.setMatchingStatus(newMatchingStatus);
            invoice.setMatchedDate(LocalDate.now());
            invoice.setMatchedAt(LocalDateTime.now());
            invoice.setMatchedBy(matchedBy);
            invoice.setMatchedByName("系统");
            invoice.setMatchingBatchNumber(paymentRecord.getBatchNumber());
            invoice.setMatchingNotes(notes);
            
            invoiceRepository.save(invoice);
            
            return true;
        } catch (Exception e) {
            // 记录错误日志
            log.error("匹配操作失败: " + e.getMessage());
            return false;
        }
    }
}