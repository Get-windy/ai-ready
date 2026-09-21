package cn.aiedge.base.payment;

/**
 * 验签通过后的回调业务字段（渠道无关）。
 *
 * <p>由各渠道实现从自己的报文格式里解析出来，调用方只认这几个字段 ——
 * 这样「订单标记已支付」的逻辑不必知道微信与支付宝的报文差异。</p>
 *
 * @param merchantOrderNo 商户订单号（对应本系统的单据号）
 * @param channelOrderNo  渠道订单号（微信 transaction_id / 支付宝 trade_no），可为 null
 * @param paidAmount      实付金额（用于与订单金额比对，防止改价重放）
 * @param success         该渠道语义下「支付成功」
 * @param rawPayload      验签通过后的原始业务字段（仅用于日志留存/排查，不参与判断）
 */
public record PaymentCallbackResult(
        String merchantOrderNo,
        String channelOrderNo,
        java.math.BigDecimal paidAmount,
        boolean success,
        String rawPayload
) {
}
