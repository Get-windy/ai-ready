package cn.aiedge.payment.channel;

import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 银行转账渠道适配器
 */
@Component
public class BankChannel implements PaymentChannel {

    @Override
    public String getChannelCode() {
        return "BANK";
    }

    @Override
    public String getChannelName() {
        return "银行转账";
    }

    @Override
    public String createPayment(PaymentRequest request) {
        // 银行转账需要人工确认
        return "BANK_" + request.getId();
    }

    @Override
    public PaymentRecord queryPayment(String channelOrderNo) {
        PaymentRecord record = new PaymentRecord();
        record.setChannelOrderNo(channelOrderNo);
        record.setChannel(getChannelCode());
        record.setStatus(0); // 需人工确认
        return record;
    }

    @Override
    public PaymentRecord handleCallback(String callbackData) {
        // 银行转账无自动回调，需人工录入
        PaymentRecord record = new PaymentRecord();
        record.setChannel(getChannelCode());
        record.setCallbackData(callbackData);
        record.setStatus(0);
        return record;
    }

    @Override
    public void closePayment(String channelOrderNo) {
        // 银行转账无需关闭
    }

    @Override
    public String createRefund(RefundRequest request, PaymentRequest originalPayment) {
        return "BANK_REFUND_" + request.getId();
    }

    @Override
    public RefundRecord queryRefund(String channelRefundNo) {
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
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getMaxAmount() {
        return BigDecimal.valueOf(999999999);
    }
}