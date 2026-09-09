package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.dto.PreReceiptDTO;
import cn.aiedge.erp.payment.dto.PreReceiptQuery;
import cn.aiedge.erp.payment.dto.PreReceiptSaveDTO;
import cn.aiedge.erp.payment.entity.PreReceipt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface PreReceiptService extends IService<PreReceipt> {

    PreReceipt getByPreReceiptNo(String preReceiptNo);

    Page<PreReceipt> pageList(String keyword, Long customerId, String status, int pageNum, int pageSize);

    Page<PreReceipt> pageQuery(PreReceiptQuery query, int pageNum, int pageSize);

    PreReceipt createPreReceipt(PreReceipt receipt);

    /** 保存草稿（新建或更新，含收款账户明细，不记账） */
    PreReceipt saveDraft(PreReceiptSaveDTO dto);

    /** 记账：预收余额增加 + 生成凭证（KJPZ-，2203）+ 记录资金流水 */
    PreReceipt confirm(Long id, Long operatorId, String operatorName);

    /** 预收款详情（含收款账户明细） */
    PreReceiptDTO getDetail(Long id);

    /** 查询结算单位（客户）预收余额 */
    BigDecimal getCustomerAdvanceBalance(Long customerId);

    void offsetToReceipt(Long preReceiptId, Long receiptId, BigDecimal amount);

    PreReceipt forfeit(Long id, String reason);

    PreReceipt refund(Long id, String reason);

    void calculateRemaining(Long id);

    String generatePreReceiptNo();
}
