package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PaymentItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService extends IService<Payment> {

    Payment getByPaymentNo(String paymentNo);

    Page<Payment> pageList(String keyword, Long supplierId, Long orderId, Integer status, String sourceType, int pageNum, int pageSize);

    List<Payment> exportList(String keyword, Long supplierId, Long orderId, Integer status);

    List<Payment> listBySupplierId(Long supplierId);

    List<Payment> listByOrderId(Long orderId);

    String generatePaymentNo();

    Payment createPayment(Payment payment, List<PaymentItem> items);

    Payment createFromOrder(Long orderId);

    Payment createFromInvoice(Long invoiceId);

    Payment updatePayment(Long paymentId, Payment payment, List<PaymentItem> items);

    Payment submitForApproval(Long paymentId);

    Payment approve(Long paymentId, Long approverId, String note);

    Payment reject(Long paymentId, String reason);

    Payment startVerify(Long paymentId);

    PaymentItem verifyItem(Long itemId, BigDecimal verifyAmount);

    Payment completeVerify(Long paymentId);

    Payment complete(Long paymentId);

    Payment cancel(Long paymentId, String reason);

    void calculateTotals(Long paymentId);

    /**
     * 核销付款单
     * @param paymentId 付款单ID
     * @param amount 核销金额
     */
    Payment writeOff(Long paymentId, BigDecimal amount);

    List<PaymentItem> getItems(Long paymentId);

    PaymentItem addItem(Long paymentId, PaymentItem item);

    void removeItem(Long itemId);
}