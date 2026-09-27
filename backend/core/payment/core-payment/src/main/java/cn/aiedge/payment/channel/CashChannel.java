package cn.aiedge.payment.channel;

import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 现金/线下支付渠道适配器
 */
@Component
public class CashChannel implements PaymentChannel {

    @Override
    public String getChannelCode() {
        return "CASH";
    }

    @Override
    public String getChannelName() {
        return "现金/线下";
    }

    @Override
    public ChannelPayResult createPayment(PaymentRequest request) {
        // 现金为**线下渠道**：无收银台可跳，当场收款由人工确认（confirmOfflinePayment）
        return ChannelPayResult.ofChannelOrderNo("CASH_" + request.getId());
    }

    @Override
    public PaymentRecord queryPayment(String channelOrderNo) {
        PaymentRecord record = new PaymentRecord();
        record.setChannelOrderNo(channelOrderNo);
        record.setChannel(getChannelCode());
        record.setStatus(2); // 线下支付默认成功
        return record;
    }

    @Override
    public void closePayment(String channelOrderNo) {
        // 线下支付无需关闭
    }

    @Override
    public String createRefund(RefundRequest request, PaymentRequest originalPayment) {
        // 线下退款需人工处理
        return "CASH_REFUND_" + request.getId();
    }

    @Override
    public RefundRecord queryRefund(String channelRefundNo) {
        RefundRecord record = new RefundRecord();
        record.setChannelRefundNo(channelRefundNo);
        record.setChannel(getChannelCode());
        record.setStatus(1); // 线下退款默认成功
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