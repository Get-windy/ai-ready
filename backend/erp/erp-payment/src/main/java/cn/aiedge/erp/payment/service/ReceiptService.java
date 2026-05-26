package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PaymentItem;
import cn.aiedge.erp.payment.entity.Receipt;
import cn.aiedge.erp.payment.entity.ReceiptItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface ReceiptService extends IService<Receipt> {

    Receipt getByReceiptNo(String receiptNo);

    Page<Receipt> pageList(String keyword, Long customerId, Long orderId, Integer status, int pageNum, int pageSize);

    List<Receipt> listByCustomerId(Long customerId);

    List<Receipt> listByOrderId(Long orderId);

    String generateReceiptNo();

    Receipt createReceipt(Receipt receipt, List<ReceiptItem> items);

    Receipt createFromOrder(Long orderId);

    Receipt createFromInvoice(Long invoiceId);

    Receipt updateReceipt(Long receiptId, Receipt receipt, List<ReceiptItem> items);

    Receipt submitForApproval(Long receiptId);

    Receipt approve(Long receiptId, Long approverId, String note);

    Receipt reject(Long receiptId, String reason);

    Receipt startVerify(Long receiptId);

    ReceiptItem verifyItem(Long itemId, BigDecimal verifyAmount);

    Receipt completeVerify(Long receiptId);

    Receipt complete(Long receiptId);

    Receipt cancel(Long receiptId, String reason);

    void calculateTotals(Long receiptId);

    List<ReceiptItem> getItems(Long receiptId);

    ReceiptItem addItem(Long receiptId, ReceiptItem item);

    void removeItem(Long itemId);
}