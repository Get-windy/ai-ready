package cn.aiedge.base.payment;

/**
 * 支付渠道回调的**验签契约**（平台提供接口与方法，渠道实现按租户接）。
 *
 * <h2>为什么需要这份契约</h2>
 * 本仓已有一套支付渠道抽象（{@code cn.aiedge.payment.channel.PaymentChannel}），
 * 含微信/支付宝/银联/银行/现金五个实现。但实测（2026-09-21）**这些实现全是桩**：
 * <ul>
 *   <li>{@code WechatChannel.handleCallback} 只是把回调报文包成 {@code PaymentRecord}
 *       并硬编码 {@code status = 2}（成功），**没有任何验签**；</li>
 *   <li>{@code AlipayChannel.handleCallback} 直接写着 {@code // TODO: 解析支付宝回调数据}；</li>
 *   <li>{@code createPayment}/{@code closePayment}/{@code createRefund} 同样是桩。</li>
 * </ul>
 * 也就是说：**「回调成功」这件事当前是可以被凭空断言出来的**。
 * 谁把回调端点放出去（例如加进匿名白名单让微信/支付宝服务器可达），
 * 谁就等于开放了「任意人 POST 一个单号即把订单标成已支付」。
 *
 * <h2>契约口径</h2>
 * <ul>
 *   <li><b>fail-closed</b>：没有任何 verifier 认领该渠道，或 verifier 判定凭据未配置
 *       ⇒ 回调**一律拒绝**，不落任何业务状态。宁可收不到回调（走对账补），
 *       也不能把「没验签」当成「验过了」。</li>
 *   <li><b>凭据按租户</b>：渠道由租户自选（微信 / 支付宝 / 都接），
 *       因此同一渠道在不同租户下的商户号与密钥不同。实现方按
 *       {@link PaymentCallbackContext#tenantId()} 取该租户的凭据。</li>
 *   <li><b>应答体由渠道决定</b>：微信要求应答 {@code SUCCESS}、支付宝要求 {@code success}，
 *       否则网关会持续重推。故由 {@link #ackBody(boolean)} 交回实现方决定，
 *       而不是统一包成平台自己的 JSON —— 实测商城回调当前返回
 *       {@code ApiResponse} JSON，网关会当成失败无限重试。</li>
 *   <li><b>原始报文必须原样保留</b>：验签针对**字节级原始 body**，
 *       任何先反序列化再拼回去的做法都会改变签名。故 {@link PaymentCallbackContext#rawBody()}
 *       是 byte[]，实现方不得用「反序列化后的 Map 再序列化」来验签。</li>
 * </ul>
 *
 * <h2>接入一个渠道要做什么</h2>
 * <ol>
 *   <li>在 {@code core-payment} 实现本接口，{@link #channelCode()} 返回渠道码
 *       （与 {@code PaymentChannel#getChannelCode()} 一致，如 {@code WECHAT}/{@code ALIPAY}）；</li>
 *   <li>{@link #verify} 里按租户取凭据并**真实验签**：微信 APIv3 用平台证书验
 *       {@code Wechatpay-Signature}（或 APIv3 密钥 AES-GCM 解密 resource）；支付宝用公钥 RSA2 验签。
 *       验签失败抛 {@link PaymentCallbackVerificationException}，**不要返回 null 表示失败**；</li>
 *   <li>把验签通过后的业务字段（商户订单号、金额、支付状态）回填进
 *       {@link PaymentCallbackResult}；</li>
 *   <li>注册成 Spring Bean 即可被 {@code List<PaymentCallbackVerifier>} 收到，无需改调用方。</li>
 * </ol>
 *
 * @see PaymentCallbackContext
 * @see PaymentCallbackResult
 */
public interface PaymentCallbackVerifier {

    /** 渠道码，与 {@code PaymentChannel#getChannelCode()} 保持一致（如 WECHAT / ALIPAY）。 */
    String channelCode();

    /**
     * 该租户在此渠道下是否已配置好可验签的凭据。
     *
     * <p>返回 {@code false} 时调用方应直接拒绝回调（fail-closed），
     * 不要让 {@link #verify} 去猜一个默认密钥。</p>
     */
    boolean isConfigured(Long tenantId);

    /**
     * 验签并解析回调。
     *
     * @throws PaymentCallbackVerificationException 验签失败、报文非法、或凭据缺失
     */
    PaymentCallbackResult verify(PaymentCallbackContext context);

    /**
     * 该渠道期望的应答体。
     *
     * @param success 是否处理成功
     * @return 直接写给网关的字符串（微信 "SUCCESS" / 支付宝 "success"）
     */
    String ackBody(boolean success);
}
