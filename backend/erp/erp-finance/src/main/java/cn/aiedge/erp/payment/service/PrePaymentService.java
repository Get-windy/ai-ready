package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.dto.PrePaymentDTO;
import cn.aiedge.erp.payment.dto.PrePaymentQuery;
import cn.aiedge.erp.payment.dto.PrePaymentSaveDTO;
import cn.aiedge.erp.payment.entity.PrePayment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface PrePaymentService extends IService<PrePayment> {

    PrePayment getByPrePaymentNo(String prePaymentNo);

    Page<PrePayment> pageList(String keyword, Long supplierId, String status, int pageNum, int pageSize);

    Page<PrePayment> pageQuery(PrePaymentQuery query, int pageNum, int pageSize);

    PrePayment createPrePayment(PrePayment payment);

    /** 保存草稿（新建或更新，含付款账户明细，不记账） */
    PrePayment saveDraft(PrePaymentSaveDTO dto);

    /** 记账：预付余额增加 + 生成凭证（KJPZ-，1123 预付账款）+ 记录资金流水 */
    PrePayment confirm(Long id, Long operatorId, String operatorName);

    /** 预付款详情（含付款账户明细） */
    PrePaymentDTO getDetail(Long id);

    /** 查询结算单位（供应商）预付余额 */
    BigDecimal getSupplierAdvanceBalance(Long supplierId);

    void offsetToPayment(Long prePaymentId, Long paymentId, BigDecimal amount);

    PrePayment recover(Long id, String reason);

    PrePayment refund(Long id, String reason);

    void calculateRemaining(Long id);

    String generatePrePaymentNo();
}
