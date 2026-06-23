package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.PrePayment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface PrePaymentService extends IService<PrePayment> {

    PrePayment getByPrePaymentNo(String prePaymentNo);

    Page<PrePayment> pageList(String keyword, Long supplierId, String status, int pageNum, int pageSize);

    PrePayment createPrePayment(PrePayment payment);

    void offsetToPayment(Long prePaymentId, Long paymentId, BigDecimal amount);

    PrePayment recover(Long id, String reason);

    PrePayment refund(Long id, String reason);

    void calculateRemaining(Long id);

    String generatePrePaymentNo();
}
