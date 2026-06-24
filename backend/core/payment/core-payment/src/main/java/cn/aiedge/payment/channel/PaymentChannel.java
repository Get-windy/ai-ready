package cn.aiedge.payment.channel;

import cn.aiedge.payment.entity.PaymentRequest;
import cn.aiedge.payment.entity.PaymentRecord;
import cn.aiedge.payment.entity.RefundRequest;
import cn.aiedge.payment.entity.RefundRecord;

import java.math.BigDecimal;

/**
 * 支付渠道适配器接口
 */
public interface PaymentChannel {

    /** 获取渠道编码 */
    String getChannelCode();

    /** 获取渠道名称 */
    String getChannelName();

    /**
     * 创建支付订单
     * @param request 支付请求
     * @return 渠道订单号
     */
    String createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     * @param channelOrderNo 渠道订单号
     * @return 支付记录
     */
    PaymentRecord queryPayment(String channelOrderNo);

    /**
     * 处理支付回调
     * @param callbackData 回调数据
     * @return 支付记录
     */
    PaymentRecord handleCallback(String callbackData);

    /**
     * 关闭支付订单
     * @param channelOrderNo 渠道订单号
     */
    void closePayment(String channelOrderNo);

    /**
     * 创建退款
     * @param request 退款请求
     * @param originalPayment 原支付请求
     * @return 渠道退款号
     */
    String createRefund(RefundRequest request, PaymentRequest originalPayment);

    /**
     * 查询退款状态
     * @param channelRefundNo 渠道退款号
     * @return 退款记录
     */
    RefundRecord queryRefund(String channelRefundNo);

    /**
     * 检查渠道是否可用
     */
    boolean isAvailable();

    /**
     * 获取渠道最低支付金额
     */
    BigDecimal getMinAmount();

    /**
     * 获取渠道最高支付金额
     */
    BigDecimal getMaxAmount();
}