package cn.aiedge.payment.channel;

import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 微信支付渠道适配器
 */
@Component
public class WechatChannel implements PaymentChannel {

    @Override
    public String getChannelCode() {
        return "WECHAT";
    }

    @Override
    public String getChannelName() {
        return "微信支付";
    }

    @Override
    public ChannelPayResult createPayment(PaymentRequest request) {
        // TODO(微信)：JSAPI/Native 下单需要商户号 + APIv3 密钥 + 证书。
        // 在凭据齐备前**不伪造**渠道单号，直接拒绝，避免出现"下单成功但根本付不了"的假象。
        throw new IllegalStateException("微信支付通道尚未接入（需配置商户号与 APIv3 凭据）");
    }

    @Override
    public PaymentRecord queryPayment(String channelOrderNo) {
        PaymentRecord record = new PaymentRecord();
        record.setChannelOrderNo(channelOrderNo);
        record.setChannel(getChannelCode());
        record.setStatus(0);
        return record;
    }

    @Override
    public void closePayment(String channelOrderNo) {
        // TODO: 调用微信支付API关闭订单
    }

    @Override
    public String createRefund(RefundRequest request, PaymentRequest originalPayment) {
        return "WECHAT_REFUND_" + request.getId();
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
        return BigDecimal.valueOf(0.01);
    }

    @Override
    public BigDecimal getMaxAmount() {
        return BigDecimal.valueOf(50000);
    }
}