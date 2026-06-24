package cn.aiedge.payment.channel;

import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付宝渠道适配器
 */
@Component
public class AlipayChannel implements PaymentChannel {

    @Override
    public String getChannelCode() {
        return "ALIPAY";
    }

    @Override
    public String getChannelName() {
        return "支付宝";
    }

    @Override
    public String createPayment(PaymentRequest request) {
        // TODO: 调用支付宝API创建订单
        // 实际实现需要集成支付宝SDK
        return "ALIPAY_" + request.getId();
    }

    @Override
    public PaymentRecord queryPayment(String channelOrderNo) {
        // TODO: 调用支付宝API查询订单
        PaymentRecord record = new PaymentRecord();
        record.setChannelOrderNo(channelOrderNo);
        record.setChannel(getChannelCode());
        record.setStatus(0);
        return record;
    }

    @Override
    public PaymentRecord handleCallback(String callbackData) {
        // TODO: 解析支付宝回调数据
        PaymentRecord record = new PaymentRecord();
        record.setChannel(getChannelCode());
        record.setCallbackData(callbackData);
        record.setStatus(2);
        return record;
    }

    @Override
    public void closePayment(String channelOrderNo) {
        // TODO: 调用支付宝API关闭订单
    }

    @Override
    public String createRefund(RefundRequest request, PaymentRequest originalPayment) {
        // TODO: 调用支付宝API创建退款
        return "ALIPAY_REFUND_" + request.getId();
    }

    @Override
    public RefundRecord queryRefund(String channelRefundNo) {
        // TODO: 调用支付宝API查询退款
        RefundRecord record = new RefundRecord();
        record.setChannelRefundNo(channelRefundNo);
        record.setChannel(getChannelCode());
        record.setStatus(0);
        return record;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public BigDecimal getMinAmount() {
        return BigDecimal.valueOf(0.01);
    }

    @Override
    public BigDecimal getMaxAmount() {
        return BigDecimal.valueOf(50000);
    }
}