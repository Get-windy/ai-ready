package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.PreReceipt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface PreReceiptService extends IService<PreReceipt> {

    PreReceipt getByPreReceiptNo(String preReceiptNo);

    Page<PreReceipt> pageList(String keyword, Long customerId, String status, int pageNum, int pageSize);

    PreReceipt createPreReceipt(PreReceipt receipt);

    void offsetToReceipt(Long preReceiptId, Long receiptId, BigDecimal amount);

    PreReceipt forfeit(Long id, String reason);

    PreReceipt refund(Long id, String reason);

    void calculateRemaining(Long id);

    String generatePreReceiptNo();
}
