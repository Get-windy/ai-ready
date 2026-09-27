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
     * 渠道下单：返回**渠道订单标识 + 客户端付款地址/参数**。
     *
     * <p>2026-09-26 由「返回 {@code String}（渠道订单号）」改为返回
     * {@link ChannelPayResult} —— 真接支付宝/银联后，下单必须同时给出
     * 「查单/退款用的键」与「客户端去哪儿付」。原设计只有前者，结果是
     * 要么把收银台 URL 塞进 {@code channel_order_no}（之后查单必炸），
     * 要么客户端拿不到付款地址（C 端只能跳空白页）。</p>
     *
     * @param request 支付请求（{@code bizNo} 即提交给渠道的商户单号 / out_trade_no）
     */
    ChannelPayResult createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     * @param channelOrderNo 渠道订单号
     * @return 支付记录
     */
    PaymentRecord queryPayment(String channelOrderNo);


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