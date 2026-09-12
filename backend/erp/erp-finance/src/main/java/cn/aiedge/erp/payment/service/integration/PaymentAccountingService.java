package cn.aiedge.erp.payment.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.finance.service.ReceivableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * 收付款模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建收付款凭证，
 * 并调用 ReceivableService/PayableService 核销应收/应付
 *
 * 事务语义：凭证创建（网关方法已声明 REQUIRES_NEW）与核销（此处 TransactionTemplate
 * REQUIRES_NEW）均在独立事务中提交，失败只回滚自身，不污染调用方的
 * catch-and-continue 主流程事务，与原 HTTP 内调时代语义一致。
 */
@Slf4j
@Service
public class PaymentAccountingService {

    private final BusinessAccountingService businessAccountingService;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final TransactionTemplate writeOffTxTemplate;

    public PaymentAccountingService(BusinessAccountingService businessAccountingService,
                                    ReceivableService receivableService,
                                    PayableService payableService,
                                    PlatformTransactionManager transactionManager) {
        this.businessAccountingService = businessAccountingService;
        this.receivableService = receivableService;
        this.payableService = payableService;
        this.writeOffTxTemplate = new TransactionTemplate(transactionManager);
        this.writeOffTxTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 付款时创建付款凭证并核销应付账款
     * Dr. Accounts Payable (应付账款)
     * Cr. Bank Deposit (银行存款)
     *
     * @param paymentId     付款单ID
     * @param paymentNo     付款单编号
     * @param supplierId    供应商ID
     * @param supplierName  供应商名称
     * @param amount        付款金额
     * @param payableId     应付款ID (用于核销)
     */
    public void postPaymentVoucher(Long paymentId, String paymentNo, String supplierId,
                                   String supplierName, BigDecimal amount, Long payableId) {
        log.info("创建付款凭证: paymentNo={}, amount={}", paymentNo, amount);

        // 1. Create payment voucher
        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("PAYMENT");
        voucherRequest.setSourceId(paymentId);
        voucherRequest.setSourceNo(paymentNo);
        voucherRequest.setSupplierId(supplierId);
        voucherRequest.setSupplierName(supplierName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("付款 - " + paymentNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. AP, Cr. Bank Deposit
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("支付应付账款");
        debitEntry.setSubjectCode("2202");  // 应付账款
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);
        debitEntry.setAuxUnit(supplierName);  // 核算项-供应商（辅助核算余额表按往来单位归集）

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("银行存款");
        creditEntry.setSubjectCode("1002");  // 银行存款
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("付款凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());

        // 2. Write off the AP (独立事务)
        if (payableId != null) {
            writeOffTxTemplate.executeWithoutResult(tx -> payableService.writeOff(payableId, amount));
            log.info("应付核销成功: payableId={}", payableId);
        }
    }

    /**
     * 收款时创建收款凭证并核销应收账款
     * Dr. Bank Deposit (银行存款)
     * Cr. Accounts Receivable (应收账款)
     *
     * @param receiptId     收款单ID
     * @param receiptNo     收款单编号
     * @param customerId    客户ID
     * @param customerName  客户名称
     * @param amount        收款金额
     * @param receivableId  应收款ID (用于核销)
     */
    public void postReceiptVoucher(Long receiptId, String receiptNo, String customerId,
                                   String customerName, BigDecimal amount, Long receivableId) {
        log.info("创建收款凭证: receiptNo={}, amount={}", receiptNo, amount);

        // 1. Create receipt voucher
        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("RECEIPT");
        voucherRequest.setSourceId(receiptId);
        voucherRequest.setSourceNo(receiptNo);
        voucherRequest.setCustomerId(customerId);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("收款 - " + receiptNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. Bank Deposit, Cr. AR
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("银行存款");
        debitEntry.setSubjectCode("1002");  // 银行存款
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("收回账款");
        creditEntry.setSubjectCode("1122");  // 应收账款
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);
        creditEntry.setAuxUnit(customerName);  // 核算项-客户（辅助核算余额表按往来单位归集）

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("收款凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());

        // 2. Write off the AR (独立事务)
        if (receivableId != null) {
            writeOffTxTemplate.executeWithoutResult(tx -> receivableService.writeOff(receivableId, amount));
            log.info("应收核销成功: receivableId={}", receivableId);
        }
    }

    /**
     * 预付款单记账时创建预付款凭证
     * Dr. Prepayment (预付账款 1123)
     * Cr. Bank Deposit (银行存款 1002)
     *
     * @param paymentId    预付款单ID
     * @param paymentNo    预付款单编号
     * @param supplierId   供应商ID
     * @param supplierName 供应商名称
     * @param amount       预付金额
     */
    public void postPrePaymentVoucher(Long paymentId, String paymentNo, String supplierId,
                                      String supplierName, BigDecimal amount) {
        log.info("创建预付款凭证: paymentNo={}, amount={}", paymentNo, amount);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("PRE_PAYMENT");
        voucherRequest.setSourceId(paymentId);
        voucherRequest.setSourceNo(paymentNo);
        voucherRequest.setSupplierId(supplierId);
        voucherRequest.setSupplierName(supplierName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("预付款 - " + paymentNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. Prepayment, Cr. Bank Deposit
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("预付账款");
        debitEntry.setSubjectCode("1123");  // 预付账款
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);
        debitEntry.setAuxUnit(supplierName);  // 核算项-供应商（辅助核算余额表按往来单位归集）

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("银行存款");
        creditEntry.setSubjectCode("1002");  // 银行存款
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("预付款凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());
    }
}
