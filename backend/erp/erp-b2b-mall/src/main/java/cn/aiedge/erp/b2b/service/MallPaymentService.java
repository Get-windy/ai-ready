package cn.aiedge.erp.b2b.service;

import java.util.Map;

public interface MallPaymentService {

    void createPayment(Map<String, Object> paymentRequest);

    void getPaymentStatus(String id);

    void handleCallback(Map<String, Object> callbackData);
}
