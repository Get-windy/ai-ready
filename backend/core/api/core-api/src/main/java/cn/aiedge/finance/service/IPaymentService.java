package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Payment;
import cn.aiedge.finance.dto.PaymentCreateRequest;
import cn.aiedge.finance.dto.PaymentQueryRequest;
import cn.aiedge.finance.dto.PaymentVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 付款记录服务接口
 */
public interface IPaymentService extends IService<Payment> {

    /**
     * 创建付款记录
     */
    Long createPayment(PaymentCreateRequest request);

    /**
     * 分页查询付款记录
     */
    Page<PaymentVO> pagePayments(PaymentQueryRequest request);

    /**
     * 根据ID获取付款记录详情
     */
    PaymentVO getPaymentById(Long id);

    /**
     * 删除付款记录
     */
    void deletePayment(Long id);
}
