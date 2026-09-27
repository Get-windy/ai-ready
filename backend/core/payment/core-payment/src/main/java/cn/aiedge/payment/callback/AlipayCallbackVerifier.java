package cn.aiedge.payment.callback;

import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.payment.PaymentCallbackVerifier;
import cn.aiedge.payment.dto.PaymentChannelParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 支付宝异步通知验签实现（RSA2 / SHA256withRSA）。
 *
 * <h2>为什么是它先落地</h2>
 * 平台要支持「每个租户自选渠道」—— 微信、支付宝、或都接。支付宝这条链路最短：
 * 验签只需要**一个支付宝公钥**（银行/微信那侧还要证书链或平台证书轮换），
 * 适合先把 {@link PaymentCallbackVerifier} 的契约跑通并自证正确。
 *
 * <h2>验签口径（严格按支付宝异步通知规则）</h2>
 * <ol>
 *   <li>报文是 {@code application/x-www-form-urlencoded} 表单（不是 JSON）；</li>
 *   <li>待验签串 = 除 {@code sign} 与 {@code sign_type} 外的**所有非空参数**，
 *       按参数名**字典序升序**，以 {@code k=v} 用 {@code &} 连接；
 *       <b>使用 URL 解码后的值</b>（与支付宝一致）；</li>
 *   <li>用支付宝公钥以 {@code SHA256withRSA} 验证 {@code sign}（Base64）；</li>
 *   <li>额外校验 {@code app_id} 与本地配置一致 —— 否则**别的应用的合法回调**
 *       也能验签通过并落到本租户头上（签名只证明「来自支付宝」，不证明「发给谁」）。</li>
 * </ol>
 *
 * <h2>凭据从哪读</h2>
 * {@code sys_project_config} 的 {@code payment.channel.alipay}（值为
 * {@link PaymentChannelParam} 的 JSON）。回调没有会话，故按**路径里的租户**读取：
 * 用 {@link MyBatisPlusConfig#setTempTenantId} 临时设定租户再读，读完**必须清理**
 * （ThreadLocal + 线程池复用，不清理会串租户）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayCallbackVerifier implements PaymentCallbackVerifier {

    /** 渠道码，与 {@code AlipayChannel#getChannelCode()} 保持一致 */
    public static final String CHANNEL_CODE = "ALIPAY";

    /** 渠道参数在 sys_project_config 中的键（小写渠道码，见 PaymentConfigCatalog） */
    public static final String CONFIG_KEY = "payment.channel.alipay";

    /** 支付宝语义下的「支付成功」 */
    private static final Set<String> SUCCESS_TRADE_STATUS =
            Set.of("TRADE_SUCCESS", "TRADE_FINISHED");

    /** 不参与验签的字段 */
    private static final Set<String> EXCLUDED_FIELDS = Set.of("sign", "sign_type");

    /** 按租户严格读取凭据（不回落平台行、不依赖会话）—— 见 TenantChannelCredentialReader 类注释 */
    private final TenantChannelCredentialReader credentialReader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean isConfigured(Long tenantId) {
        PaymentChannelParam param = loadParam(tenantId);
        return param != null
                && Boolean.TRUE.equals(param.getEnabled())
                // 不是「非空」而是「能解析成 RSA 公钥」：填了半截/填成私钥/丢了 PEM 头尾
                // 从非空看都是配了，但验签必然失败 —— 那种情况必须等同于「未配置」
                && PaymentCredentialValidator.isValidRsaPublicKey(param.getAlipayPublicKey());
    }

    @Override
    public PaymentCallbackResult verify(PaymentCallbackContext context) {
        PaymentChannelParam param = loadParam(context.tenantId());
        if (param == null || !hasText(param.getAlipayPublicKey())) {
            // fail-closed：没配公钥就不存在「验签通过」这回事
            throw new PaymentCallbackVerificationException("支付宝渠道未配置公钥，拒绝回调");
        }
        if (!Boolean.TRUE.equals(param.getEnabled())) {
            throw new PaymentCallbackVerificationException("支付宝渠道未启用，拒绝回调");
        }

        Map<String, String> params = parseFormUrlEncoded(context.rawBodyAsString());
        String sign = params.get("sign");
        if (!hasText(sign)) {
            throw new PaymentCallbackVerificationException("回调缺少 sign 参数");
        }

        String content = buildSignContent(params);
        if (!verifyRsa2(content, sign, param.getAlipayPublicKey())) {
            throw new PaymentCallbackVerificationException("支付宝回调验签失败");
        }

        // 签名只证明「来自支付宝」，不证明「发给本应用」—— 必须再比 app_id
        if (hasText(param.getAppId()) && !param.getAppId().equals(params.get("app_id"))) {
            throw new PaymentCallbackVerificationException(
                    "回调 app_id 与本租户配置不符：回调=" + params.get("app_id") + ", 配置=" + param.getAppId());
        }

        boolean success = SUCCESS_TRADE_STATUS.contains(params.get("trade_status"));
        return new PaymentCallbackResult(
                params.get("out_trade_no"),
                params.get("trade_no"),
                parseAmount(params.get("total_amount")),
                success,
                context.rawBodyAsString());
    }

    @Override
    public String ackBody(boolean success) {
        // 支付宝要求应答纯文本 success / failure（不是 JSON）
        return success ? "success" : "failure";
    }

    // ─────────────────────────── 内部实现 ───────────────────────────

    /** 按租户读取渠道参数。**严格本租户**，绝不回落平台行（凭据是身份，不是默认值）。 */
    private PaymentChannelParam loadParam(Long tenantId) {
        String raw = credentialReader.read(tenantId, CONFIG_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, PaymentChannelParam.class);
        } catch (Exception e) {
            log.warn("支付渠道配置解析失败：tenantId={}, channel={}, reason={}", tenantId, CHANNEL_CODE, e.getMessage());
            return null;
        }
    }

    /**
     * 构造待验签串：剔除 sign/sign_type 与空值，按参数名升序，{@code k=v} 以 {@code &} 连接。
     *
     * <p>2026-09-26：实现改为委托 {@link cn.aiedge.payment.crypto.Rsa2#buildSignContent} ——
     * 通道侧（{@code AlipayChannel}）**签名**时用的是同一份实现，两边口径不可能再漂移。</p>
     */
    static String buildSignContent(Map<String, String> params) {
        return cn.aiedge.payment.crypto.Rsa2.buildSignContent(params, EXCLUDED_FIELDS);
    }

    /** 解析 x-www-form-urlencoded 报文（保留解码后的值，勿二次编码）。 */
    static Map<String, String> parseFormUrlEncoded(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!hasText(raw)) {
            return map;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = idx < 0 ? pair : pair.substring(0, idx);
            String value = idx < 0 ? "" : pair.substring(idx + 1);
            map.put(decode(key), decode(value));
        }
        return map;
    }

    private static String decode(String v) {
        try {
            return URLDecoder.decode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }

    /**
     * RSA2（SHA256withRSA）验签。公钥为 Base64（可带 PEM 头尾，自动剥离）。
     *
     * <p>2026-09-26：实现改为委托 {@link cn.aiedge.payment.crypto.Rsa2#verify}（同一份实现
     * 也供通道签名时使用）。</p>
     */
    static boolean verifyRsa2(String content, String signBase64, String publicKeyBase64) {
        return cn.aiedge.payment.crypto.Rsa2.verify(content, signBase64, publicKeyBase64);
    }

    /** 剥离 PEM 头尾与换行，便于直接 Base64 解码。 */
    private static String stripPem(String key) {
        return key.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    }

    static BigDecimal parseAmount(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
