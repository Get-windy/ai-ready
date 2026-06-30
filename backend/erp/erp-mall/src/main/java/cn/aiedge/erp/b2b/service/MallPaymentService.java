package cn.aiedge.erp.b2b.service;

import java.util.Map;

public interface MallPaymentService {

    /** 创建支付请求 */
    void createPayment(Map<String, Object> paymentRequest);

    /** 查询支付状态 */
    void getPaymentStatus(String id);

    /** 处理支付回调 */
    void handleCallback(Map<String, Object> callbackData);
}
