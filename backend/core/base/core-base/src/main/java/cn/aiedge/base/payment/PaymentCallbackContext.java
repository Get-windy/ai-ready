package cn.aiedge.base.payment;

import java.util.Map;

/**
 * 支付回调的验签输入。
 *
 * <p>{@code rawBody} 是**字节级原始报文**，验签必须基于它 ——
 * 先反序列化成 Map 再序列化回去会改变签名（键序、空格、转义都算）。</p>
 *
 * @param tenantId  回调所属租户（渠道由租户自选，凭据按租户取）
 * @param channel   渠道码（如 WECHAT / ALIPAY）
 * @param rawBody   原始请求体字节
 * @param headers   请求头（微信需要 Wechatpay-Signature / -Timestamp / -Nonce / -Serial；
 *                  支付宝的签名在表单参数里，用 rawBody 解析）
 * @param remoteIp  调用方 IP（仅用于日志与风控，**不可**作为验签依据）
 */
public record PaymentCallbackContext(
        Long tenantId,
        String channel,
        byte[] rawBody,
        Map<String, String> headers,
        String remoteIp
) {

    /** 以 UTF-8 还原原始报文文本（仅用于日志/解析，验签请用 rawBody）。 */
    public String rawBodyAsString() {
        return rawBody == null ? "" : new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** 读取请求头（大小写不敏感的常见写法差异由调用方归一后再传入）。 */
    public String header(String name) {
        return headers == null ? null : headers.get(name);
    }
}
