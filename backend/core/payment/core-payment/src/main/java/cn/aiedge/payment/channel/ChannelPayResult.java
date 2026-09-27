package cn.aiedge.payment.channel;

import java.util.Collections;
import java.util.Map;

/**
 * 渠道下单结果。
 *
 * <p><b>为什么要它</b>：原接口 {@code String createPayment(PaymentRequest)} 只能回一个字符串，
 * 而"下单"实际要回**两样**东西：</p>
 * <ul>
 *   <li>{@code channelOrderNo} —— 渠道侧的订单标识。对支付宝/银联这类「商户单号即渠道单号」的
 *       渠道，它就是**我们提交给渠道的商户单号**（{@code out_trade_no}）；后续
 *       查单/关单/退款都以它为键，所以必须能被存下来复用。</li>
 *   <li>{@code payUrl} / {@code payParams} —— 客户端**去哪儿付**。支付宝是收银台 URL；
 *       银联是网关 URL + 表单参数；微信后面接 JSAPI 时是 prepay_id 之类。</li>
 * </ul>
 * <p>原设计把这两样挤进一个 String，结果是：要么存进 {@code channel_order_no} 的是一串 URL
 * （之后查单直接报错），要么客户端拿不到付款地址（C 端只能跳空白页）。</p>
 *
 * @param channelOrderNo 渠道订单标识（= 商户单号；查单/关单/退款的键）
 * @param payUrl         客户端跳转地址（线下渠道可为空）
 * @param payParams      需要客户端拼接/表单提交的参数（可为空）
 * @param rawResponse    渠道原始应答（留痕，便于对账排障）
 */
public record ChannelPayResult(
        String channelOrderNo,
        String payUrl,
        Map<String, String> payParams,
        String rawResponse) {

    public ChannelPayResult {
        payParams = payParams == null ? Collections.emptyMap() : Map.copyOf(payParams);
    }

    /** 线下/无跳转渠道：只有渠道单号，没有付款地址。 */
    public static ChannelPayResult ofChannelOrderNo(String channelOrderNo) {
        return new ChannelPayResult(channelOrderNo, null, Collections.emptyMap(), null);
    }

    /** 有付款地址的渠道（支付宝收银台 / 银联网关）。 */
    public static ChannelPayResult ofUrl(String channelOrderNo, String payUrl, String rawResponse) {
        return new ChannelPayResult(channelOrderNo, payUrl, Collections.emptyMap(), rawResponse);
    }
}
