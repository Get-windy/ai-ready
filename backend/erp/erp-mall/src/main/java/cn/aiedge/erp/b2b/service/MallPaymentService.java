package cn.aiedge.erp.b2b.service;

import cn.aiedge.base.payment.PaymentCallbackResult;

import java.util.Map;

public interface MallPaymentService {

    /** 创建支付请求 */
    void createPayment(Map<String, Object> paymentRequest);

    /** 查询支付状态 */
    void getPaymentStatus(String id);

    /**
     * 处理**已验签通过**的支付回调。
     *
     * <p>⚠️ 原接口是 {@code handleCallback(Map callbackData)} —— 直接吃未验签的报文就改单，
     * 已删除。验签由 {@code PaymentCallbackVerifier} 负责，本方法只做「验签通过之后」的事：
     * 按租户定位订单、校验金额、幂等改状态。</p>
     *
     * @param tenantId 回调所属租户。**验签通过后该值可信**（签名是用该租户的凭据验的），
     *                 故用它锁死订单归属，避免跨租户改单
     * @param result   渠道无关的验签结果
     * @throws IllegalStateException 订单不存在 / 金额不符 —— 调用方应回「失败应答」让网关重推
     */
    void handleVerifiedCallback(Long tenantId, PaymentCallbackResult result);
}
