package cn.aiedge.erp.invoice.controller;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import cn.aiedge.erp.invoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票管理控制器
 * 提供发票相关的REST API接口
 */
@RestController
@RequestMapping("/api/erp/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    /**
     * 根据ID获取发票详情
     *
     * @param id 发票ID
     * @return 发票对象
     */
    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        Optional<Invoice> invoice = invoiceService.getInvoiceById(id);
        return invoice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据发票号码获取发票详情
     *
     * @param invoiceNumber 发票号码
     * @return 发票对象
     */
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<Invoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        Optional<Invoice> invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return invoice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有发票列表
     *
     * @return 发票列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    /**
     * 分页获取发票列表
     *
     * @param pageable 分页参数
     * @return 发票分页
     */
    @GetMapping("/page")
    public ResponseEntity<Page<Invoice>> getInvoicesPage(Pageable pageable) {
        Page<Invoice> invoices = invoiceService.getInvoices(pageable);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 根据状态获取发票列表
     *
     * @param status 发票状态
     * @return 发票列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Invoice>> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        List<Invoice> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 根据付款状态获取发票列表
     *
     * @param paymentStatus 付款状态
     * @return 发票列表
     */
    @GetMapping("/payment-status/{paymentStatus}")
    public ResponseEntity<List<Invoice>> getInvoicesByPaymentStatus(@PathVariable PaymentStatus paymentStatus) {
        List<Invoice> invoices = invoiceService.getInvoicesByPaymentStatus(paymentStatus);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 根据客户ID获取发票列表
     *
     * @param customerId 客户ID
     * @return 发票列表
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomerId(@PathVariable Long customerId) {
        List<Invoice> invoices = invoiceService.getInvoicesByCustomerId(customerId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 根据供应商ID获取发票列表
     *
     * @param supplierId 供应商ID
     * @return 发票列表
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<Invoice>> getInvoicesBySupplierId(@PathVariable Long supplierId) {
        List<Invoice> invoices = invoiceService.getInvoicesBySupplierId(supplierId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 根据日期范围获取发票列表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 发票列表
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Invoice>> getInvoicesByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Invoice> invoices = invoiceService.getInvoicesByDateRange(startDate, endDate);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 根据金额范围获取发票列表
     *
     * @param minAmount 最小金额
     * @param maxAmount 最大金额
     * @return 发票列表
     */
    @GetMapping("/amount-range")
    public ResponseEntity<List<Invoice>> getInvoicesByAmountRange(
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount) {
        List<Invoice> invoices = invoiceService.getInvoicesByAmountRange(minAmount, maxAmount);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 创建发票（从发票申请）
     *
     * @param application 发票申请
     * @param issuedBy 开票人ID
     * @param issuedByName 开票人姓名
     * @return 创建的发票
     */
    @PostMapping("/create-from-application")
    public ResponseEntity<Invoice> createInvoiceFromApplication(
            @Valid @RequestBody InvoiceApplication application,
            @RequestParam Long issuedBy,
            @RequestParam String issuedByName) {
        Invoice invoice = invoiceService.createInvoiceFromApplication(application, issuedBy, issuedByName);
        return ResponseEntity.ok(invoice);
    }

    /**
     * 更新发票状态
     *
     * @param invoiceId 发票ID
     * @param newStatus 新状态
     * @param notes 状态变更说明
     * @return 操作结果
     */
    @PutMapping("/{invoiceId}/status")
    public ResponseEntity<Boolean> updateInvoiceStatus(
            @PathVariable Long invoiceId,
            @RequestParam InvoiceStatus newStatus,
            @RequestParam(required = false) String notes) {
        boolean result = invoiceService.updateInvoiceStatus(invoiceId, newStatus, notes);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新付款状态
     *
     * @param invoiceId 发票ID
     * @param newStatus 新付款状态
     * @param notes 状态变更说明
     * @return 操作结果
     */
    @PutMapping("/{invoiceId}/payment-status")
    public ResponseEntity<Boolean> updatePaymentStatus(
            @PathVariable Long invoiceId,
            @RequestParam PaymentStatus newStatus,
            @RequestParam(required = false) String notes) {
        boolean result = invoiceService.updatePaymentStatus(invoiceId, newStatus, notes);
        return ResponseEntity.ok(result);
    }

    /**
     * 记录付款
     *
     * @param invoiceId 发票ID
     * @param amount 付款金额
     * @param paymentReference 付款参考号
     * @param notes 付款说明
     * @return 操作结果
     */
    @PostMapping("/{invoiceId}/payment")
    public ResponseEntity<Boolean> recordPayment(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentReference,
            @RequestParam(required = false) String notes) {
        boolean result = invoiceService.recordPayment(invoiceId, amount, paymentReference, notes);
        return ResponseEntity.ok(result);
    }

    /**
     * 记录部分退款
     *
     * @param invoiceId 发票ID
     * @param amount 退款金额
     * @param refundReference 退款参考号
     * @param reason 退款原因
     * @return 操作结果
     */
    @PostMapping("/{invoiceId}/partial-refund")
    public ResponseEntity<Boolean> recordPartialRefund(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String refundReference,
            @RequestParam String reason) {
        boolean result = invoiceService.recordPartialRefund(invoiceId, amount, refundReference, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * 记录全额退款
     *
     * @param invoiceId 发票ID
     * @param refundReference 退款参考号
     * @param reason 退款原因
     * @return 操作结果
     */
    @PostMapping("/{invoiceId}/full-refund")
    public ResponseEntity<Boolean> recordFullRefund(
            @PathVariable Long invoiceId,
            @RequestParam(required = false) String refundReference,
            @RequestParam String reason) {
        boolean result = invoiceService.recordFullRefund(invoiceId, refundReference, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * 作废发票
     *
     * @param invoiceId 发票ID
     * @param reason 作废原因
     * @param voidedBy 作废人ID
     * @return 操作结果
     */
    @PostMapping("/{invoiceId}/void")
    public ResponseEntity<Boolean> voidInvoice(
            @PathVariable Long invoiceId,
            @RequestParam String reason,
            @RequestParam Long voidedBy) {
        boolean result = invoiceService.voidInvoice(invoiceId, reason, voidedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 冲红发票
     *
     * @param invoiceId 发票ID
     * @param reason 冲红原因
     * @param creditedBy 冲红人ID
     * @return 冲红后的新发票
     */
    @PostMapping("/{invoiceId}/credit")
    public ResponseEntity<Invoice> creditInvoice(
            @PathVariable Long invoiceId,
            @RequestParam String reason,
            @RequestParam Long creditedBy) {
        Invoice invoice = invoiceService.creditInvoice(invoiceId, reason, creditedBy);
        return ResponseEntity.ok(invoice);
    }

    /**
     * 发送发票
     *
     * @param invoiceId 发票ID
     * @param sendMethod 发送方式（EMAIL/SMS/POSTAL/PORTAL）
     * @param sentBy 发送人ID
     * @return 操作结果
     */
    @PostMapping("/{invoiceId}/send")
    public ResponseEntity<Boolean> sendInvoice(
            @PathVariable Long invoiceId,
            @RequestParam String sendMethod,
            @RequestParam Long sentBy) {
        boolean result = invoiceService.sendInvoice(invoiceId, sendMethod, sentBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 重新发送发票
     *
     * @param invoiceId 发票ID
     * @param sendMethod 发送方式
     * @param sentBy 发送人ID
     * @return 操作结果
     */
    @PostMapping("/{invoiceId}/resend")
    public ResponseEntity<Boolean> resendInvoice(
            @PathVariable Long invoiceId,
            @RequestParam String sendMethod,
            @RequestParam Long sentBy) {
        boolean result = invoiceService.resendInvoice(invoiceId, sendMethod, sentBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 下载发票文档
     *
     * @param invoiceId 发票ID
     * @return 文档字节数组
     */
    @GetMapping("/{invoiceId}/download")
    public ResponseEntity<byte[]> downloadInvoiceDocument(@PathVariable Long invoiceId) {
        byte[] document = invoiceService.downloadInvoiceDocument(invoiceId);
        if (document != null && document.length > 0) {
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=invoice_" + invoiceId + ".pdf")
                    .body(document);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 搜索发票
     *
     * @param keyword 关键词
     * @return 发票列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<Invoice>> searchInvoices(@RequestParam String keyword) {
        List<Invoice> invoices = invoiceService.searchInvoices(keyword);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 获取发票统计信息
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<InvoiceService.InvoiceStatistics> getInvoiceStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        InvoiceService.InvoiceStatistics statistics = invoiceService.getInvoiceStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 批量生成发票
     *
     * @param applicationIds 申请ID列表
     * @param issuedBy 开票人ID
     * @param issuedByName 开票人姓名
     * @return 生成的发票列表
     */
    @PostMapping("/batch-generate")
    public ResponseEntity<List<Invoice>> batchGenerateInvoices(
            @RequestBody List<Long> applicationIds,
            @RequestParam Long issuedBy,
            @RequestParam String issuedByName) {
        List<Invoice> invoices = invoiceService.batchGenerateInvoices(applicationIds, issuedBy, issuedByName);
        return ResponseEntity.ok(invoices);
    }
}