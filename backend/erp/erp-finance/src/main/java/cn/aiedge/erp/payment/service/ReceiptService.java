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

    /**
     * 分页查询收款单（金标准多条件）
     */
    Page<Receipt> pageList(
            String keyword, Long customerId, Long orderId, Integer status, String sourceType,
            String startDate, String endDate, String customerName, String handlerName,
            String departmentName, String creatorName, String bookkeeperName, String remark,
            int pageNum, int pageSize);

    /**
     * 分页查询收款单（待确认款项专用：支持待核销/核销中多状态 + 账户/单号过滤）
     */
    Page<Receipt> pageListPending(
            String keyword, Long customerId, Long orderId, Integer status, String sourceType,
            String startDate, String endDate, String customerName, String handlerName,
            String departmentName, String creatorName, String bookkeeperName, String remark,
            String statuses, String receiptNo, String orderNo, String deliveryNo,
            String receiptAccount1, String receiptAccount2,
            int pageNum, int pageSize);

    /**
     * 按明细分页查询收款单（收款明细 tab）
     */
    Page<cn.aiedge.erp.payment.dto.ReceiptItemDetailVO> pageDetail(
            Long receiptId, Long customerId, String keyword, Integer status,
            String tradeUnit, String sourceHandler, String settlementNo,
            String startDate, String endDate, int pageNum, int pageSize);

    /**
     * 生成收款单号（SKD- 前缀）
     */
    String nextNo();

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

    /**
     * 核销收款单
     * @param receiptId 收款单ID
     * @param amount 核销金额
     */
    Receipt writeOff(Long receiptId, BigDecimal amount);

    List<ReceiptItem> getItems(Long receiptId);

    ReceiptItem addItem(Long receiptId, ReceiptItem item);

    void removeItem(Long itemId);
}