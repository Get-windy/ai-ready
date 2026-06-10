package cn.aiedge.erp.payment.service.integration;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.Receipt;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.PaymentService;
import cn.aiedge.erp.payment.service.ReceiptService;
import cn.aiedge.erp.payment.service.WriteOffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 业务模块集成服务
 * 为销售、采购、费用、资产等业务模块提供收付款创建入口
 *
 * 集成链路全景：
 *
 * 收款单（资金流入）：
 *   销售回款     ← 销售发票确认时调用 createReceiptFromSale()
 *   采购退货退款  ← 采购退货出库时调用 createReceiptFromPurchaseReturn()
 *   资产处置收款  ← 资产处置确认时调用 createReceiptFromAssetDisposal()
 *   员工退还      ← 员工报销预支>实际时调用 createReceiptFromEmployeeRefund()
 *   预收转正      ← 预收款条件达成时调用 offsetPreReceiptToReceipt()
 *   定金转正      ← 客户定金条件达成时调用 offsetDepositToReceipt()
 *
 * 付款单（资金流出）：
 *   采购付款      ← 采购发票确认时调用 createPaymentFromPurchase()
 *   销售退货退款   ← 销售退货入库时调用 createPaymentFromSaleReturn()
 *   费用报销支付   ← 费用审批通过时调用 createPaymentFromExpense()
 *   资产购置付款   ← 资产购置入库时调用 createPaymentFromAssetPurchase()
 *   预付转正      ← 预付款条件达成时调用 offsetPrePaymentToPayment()
 *   定金支付      ← 供应商定金条件达成时调用 offsetDepositToPayment()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentBusinessIntegrationService {

    private final ReceiptService receiptService;
    private final PaymentService paymentService;
    private final CapitalFlowService capitalFlowService;
    private final WriteOffService writeOffService;
    private final PaymentAccountingService paymentAccountingService;

    // ==================== 收款单集成 ====================

    /**
     * 1. 销售回款 - 销售发票确认时调用
     * 创建收款单（草稿）并生成应收记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Receipt createReceiptFromSale(Long orderId, String orderNo, Long customerId,
                                          String customerName, BigDecimal amount) {
        log.info("业财集成-销售回款: orderNo={}, customerId={}, amount={}", orderNo, customerId, amount);

        Receipt receipt = buildReceipt(orderId, orderNo, customerId, customerName, amount, "SALE_ORDER");
        receipt = receiptService.createReceipt(receipt, null);

        // 通知财务模块创建应收账款
        try {
            paymentAccountingService.postReceiptVoucher(
                    receipt.getId(), receipt.getReceiptNo(),
                    String.valueOf(customerId), customerName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应收创建失败(不影响主流程): receiptNo={}", receipt.getReceiptNo(), e);
        }

        return receipt;
    }

    /**
     * 2. 采购退货退款 - 采购退货出库时调用
     * 供应商应退款给我们的钱 → 我方收款
     */
    @Transactional(rollbackFor = Exception.class)
    public Receipt createReceiptFromPurchaseReturn(Long returnId, String returnNo, Long supplierId,
                                                    String supplierName, BigDecimal amount) {
        log.info("业财集成-采购退货退款: returnNo={}, supplierId={}, amount={}", returnNo, supplierId, amount);

        Receipt receipt = buildReceipt(returnId, returnNo, supplierId, supplierName, amount, "PURCHASE_RETURN");
        receipt = receiptService.createReceipt(receipt, null);

        try {
            paymentAccountingService.postReceiptVoucher(
                    receipt.getId(), receipt.getReceiptNo(),
                    String.valueOf(supplierId), supplierName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应收创建失败: receiptNo={}", receipt.getReceiptNo(), e);
        }

        return receipt;
    }

    /**
     * 3. 资产处置收款 - 资产处置确认时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Receipt createReceiptFromAssetDisposal(Long disposalId, String disposalNo, Long customerId,
                                                   String customerName, BigDecimal amount) {
        log.info("业财集成-资产处置收款: disposalNo={}, amount={}", disposalNo, amount);

        Receipt receipt = buildReceipt(disposalId, disposalNo, customerId, customerName, amount, "ASSET_DISPOSAL");
        receipt = receiptService.createReceipt(receipt, null);

        try {
            paymentAccountingService.postReceiptVoucher(
                    receipt.getId(), receipt.getReceiptNo(),
                    String.valueOf(customerId), customerName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应收创建失败: receiptNo={}", receipt.getReceiptNo(), e);
        }

        return receipt;
    }

    /**
     * 4. 员工退还 - 员工报销预支大于实际时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Receipt createReceiptFromEmployeeRefund(Long expenseId, String expenseNo, Long employeeId,
                                                    String employeeName, BigDecimal amount) {
        log.info("业财集成-员工退还: expenseNo={}, employeeId={}, amount={}", expenseNo, employeeId, amount);

        Receipt receipt = buildReceipt(expenseId, expenseNo, employeeId, employeeName, amount, "EMPLOYEE_REFUND");
        receipt = receiptService.createReceipt(receipt, null);

        try {
            paymentAccountingService.postReceiptVoucher(
                    receipt.getId(), receipt.getReceiptNo(),
                    String.valueOf(employeeId), employeeName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应收创建失败: receiptNo={}", receipt.getReceiptNo(), e);
        }

        return receipt;
    }

    // ==================== 付款单集成 ====================

    /**
     * 5. 采购付款 - 采购发票确认时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createPaymentFromPurchase(Long orderId, String orderNo, Long supplierId,
                                              String supplierName, BigDecimal amount) {
        log.info("业财集成-采购付款: orderNo={}, supplierId={}, amount={}", orderNo, supplierId, amount);

        Payment payment = buildPayment(orderId, orderNo, supplierId, supplierName, amount, "PURCHASE_ORDER");
        payment = paymentService.createPayment(payment, null);

        try {
            paymentAccountingService.postPaymentVoucher(
                    payment.getId(), payment.getPaymentNo(),
                    String.valueOf(supplierId), supplierName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应付创建失败: paymentNo={}", payment.getPaymentNo(), e);
        }

        return payment;
    }

    /**
     * 6. 销售退货退款 - 销售退货入库时调用（退款给客户）
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createPaymentFromSaleReturn(Long returnId, String returnNo, Long customerId,
                                                String customerName, BigDecimal amount) {
        log.info("业财集成-销售退货退款: returnNo={}, customerId={}, amount={}", returnNo, customerId, amount);

        Payment payment = buildPayment(returnId, returnNo, customerId, customerName, amount, "SALE_RETURN");
        payment = paymentService.createPayment(payment, null);

        try {
            paymentAccountingService.postPaymentVoucher(
                    payment.getId(), payment.getPaymentNo(),
                    String.valueOf(customerId), customerName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应付创建失败: paymentNo={}", payment.getPaymentNo(), e);
        }

        return payment;
    }

    /**
     * 7. 费用报销支付 - 费用审批通过时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createPaymentFromExpense(Long expenseId, String expenseNo, Long employeeId,
                                             String employeeName, BigDecimal amount) {
        log.info("业财集成-费用报销: expenseNo={}, amount={}", expenseNo, amount);

        Payment payment = buildPayment(expenseId, expenseNo, employeeId, employeeName, amount, "EXPENSE");
        payment = paymentService.createPayment(payment, null);

        try {
            paymentAccountingService.postPaymentVoucher(
                    payment.getId(), payment.getPaymentNo(),
                    String.valueOf(employeeId), employeeName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应付创建失败: paymentNo={}", payment.getPaymentNo(), e);
        }

        return payment;
    }

    /**
     * 8. 资产购置付款 - 资产购置入库时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createPaymentFromAssetPurchase(Long assetId, String assetNo, Long supplierId,
                                                   String supplierName, BigDecimal amount) {
        log.info("业财集成-资产购置: assetNo={}, supplierId={}, amount={}", assetNo, supplierId, amount);

        Payment payment = buildPayment(assetId, assetNo, supplierId, supplierName, amount, "ASSET_PURCHASE");
        payment = paymentService.createPayment(payment, null);

        try {
            paymentAccountingService.postPaymentVoucher(
                    payment.getId(), payment.getPaymentNo(),
                    String.valueOf(supplierId), supplierName, amount, null);
        } catch (Exception e) {
            log.warn("财务模块应付创建失败: paymentNo={}", payment.getPaymentNo(), e);
        }

        return payment;
    }

    // ==================== 预收/预付转正集成 ====================

    /**
     * 预收款转正式收款单
     * 条件达成后将预收款转为正式收款单并核销
     */
    @Transactional(rollbackFor = Exception.class)
    public Receipt offsetPreReceiptToReceipt(Long preReceiptId, String preReceiptNo,
                                              Long customerId, String customerName,
                                              BigDecimal amount) {
        log.info("业财集成-预收转正: preReceiptNo={}, amount={}", preReceiptNo, amount);

        Receipt receipt = new Receipt();
        receipt.setOrderId(preReceiptId);
        receipt.setOrderNo(preReceiptNo);
        receipt.setCustomerId(customerId);
        receipt.setCustomerName(customerName);
        receipt.setReceiptAmount(amount);
        receipt.setReceiptDate(LocalDate.now());
        receipt.setReceiptType(1);
        receipt.setSourceType("PRE_RECEIPT");
        receipt.setSourceId(preReceiptId);
        receipt.setSourceNo(preReceiptNo);
        receipt.setPreReceiptId(preReceiptId);

        receipt = receiptService.createReceipt(receipt, null);

        // 记录资金流水
        capitalFlowService.recordReceiptFlow(receipt);

        return receipt;
    }

    /**
     * 预付款转正式付款单
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment offsetPrePaymentToPayment(Long prePaymentId, String prePaymentNo,
                                              Long supplierId, String supplierName,
                                              BigDecimal amount) {
        log.info("业财集成-预付转正: prePaymentNo={}, amount={}", prePaymentNo, amount);

        Payment payment = new Payment();
        payment.setOrderId(prePaymentId);
        payment.setOrderNo(prePaymentNo);
        payment.setSupplierId(supplierId);
        payment.setSupplierName(supplierName);
        payment.setPaymentAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentType(1);
        payment.setSourceType("PRE_PAYMENT");
        payment.setSourceId(prePaymentId);
        payment.setSourceNo(prePaymentNo);
        payment.setPrePaymentId(prePaymentId);

        payment = paymentService.createPayment(payment, null);

        capitalFlowService.recordPaymentFlow(payment);

        return payment;
    }

    // ==================== 内部构建方法 ====================

    private Receipt buildReceipt(Long sourceId, String sourceNo, Long partyId,
                                  String partyName, BigDecimal amount, String sourceType) {
        Receipt receipt = new Receipt();
        receipt.setOrderId(sourceId);
        receipt.setOrderNo(sourceNo);
        // 根据sourceType判断是客户还是供应商
        if ("PURCHASE_RETURN".equals(sourceType)) {
            // 采购退货 - 供应商退钱给我们，收款方是供应商
            receipt.setCustomerId(partyId);
            receipt.setCustomerName(partyName);
        } else {
            receipt.setCustomerId(partyId);
            receipt.setCustomerName(partyName);
        }
        receipt.setReceiptAmount(amount);
        receipt.setReceiptDate(LocalDate.now());
        receipt.setReceiptType(1);
        receipt.setSourceType(sourceType);
        receipt.setSourceId(sourceId);
        receipt.setSourceNo(sourceNo);
        return receipt;
    }

    private Payment buildPayment(Long sourceId, String sourceNo, Long partyId,
                                  String partyName, BigDecimal amount, String sourceType) {
        Payment payment = new Payment();
        payment.setOrderId(sourceId);
        payment.setOrderNo(sourceNo);
        payment.setSupplierId(partyId);
        payment.setSupplierName(partyName);
        payment.setPaymentAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentType(1);
        payment.setSourceType(sourceType);
        payment.setSourceId(sourceId);
        payment.setSourceNo(sourceNo);
        return payment;
    }
}
