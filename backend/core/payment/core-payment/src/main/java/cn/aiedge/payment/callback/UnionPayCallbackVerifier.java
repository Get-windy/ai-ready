package cn.aiedge.payment.callback;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.payment.PaymentCallbackContext;
import cn.aiedge.base.payment.PaymentCallbackResult;
import cn.aiedge.base.payment.PaymentCallbackVerificationException;
import cn.aiedge.base.payment.PaymentCallbackVerifier;
import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.payment.dto.PaymentChannelParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 银联（UnionPay 全渠道）异步通知验签实现。
 *
 * <h2>与另两个渠道的异同</h2>
 * <table border="1">
 *   <caption>三个渠道的验签差异</caption>
 *   <tr><th>渠道</th><th>签名算法</th><th>待验签串</th><th>证书选择</th><th>业务字段</th></tr>
 *   <tr><td>支付宝</td><td>RSA2(SHA256)</td><td>参数名升序，用**解码后**的值</td><td>单一公钥</td>
 *       <td>{@code out_trade_no}/{@code trade_status}</td></tr>
 *   <tr><td>微信 V3</td><td>RSA(SHA256)，签 {@code ts\nnonce\nbody\n}</td><td>原始报文</td>
 *       <td>按 {@code Wechatpay-Serial}</td><td>resource 需 AES-GCM 解密</td></tr>
 *   <tr><td>银联</td><td>RSA（SHA1 或 SHA256，看 {@code signMethod}）</td>
 *       <td>参数名升序，值需 **URL 编码**</td><td>按 {@code certId}</td>
 *       <td>{@code orderId}/{@code respCode}</td></tr>
 * </table>
 *
 * <h2>三个容易踩的点</h2>
 * <ol>
 *   <li><b>待验签串里的值要 URL 编码</b>：银联的规则是对 value 做 URL 编码后再拼
 *       {@code k=v&…}（与支付宝「用解码后的值」相反）。照抄支付宝的写法会 100% 验签失败。</li>
 *   <li><b>金额单位是分</b>：{@code txnAmt} 单位是分，本系统是元，必须换算，
 *       否则会被「金额不符」把正常回调全拒掉。</li>
 *   <li><b>签名算法由报文决定</b>：{@code signMethod} 12=…
 *       实际取值：{@code 01}=RSA(SHA-1)、{@code 11}=RSA(SHA-256)。
 *       未知取值一律拒绝，不要「猜一个默认算法」—— 猜错等于验签失效。</li>
 * </ol>
 *
 * <h2>⚠️ 关于应答体</h2>
 * 全渠道异步通知要求商户处理成功时返回 HTTP 200。应答正文各接入版本约定不一，
 * 这里按最常见的 {@code ok} / {@code fail} 实现；**上线前请对照你们签约版本的通知规范确认**，
 * 若要求为空应答或不校验正文，改 {@link #ackBody(boolean)} 一处即可。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayCallbackVerifier implements PaymentCallbackVerifier {

    public static final String CHANNEL_CODE = "UNIONPAY";

    /** 渠道参数在 sys_project_config 中的键（小写渠道码，见 PaymentConfigCatalog） */
    public static final String CONFIG_KEY = "payment.channel.unionpay";

    /** 银联「交易成功」应答码 */
    private static final String RESP_CODE_SUCCESS = "00";

    /** 不参与验签的字段 */
    private static final String FIELD_SIGNATURE = "signature";

    private final SysConfigService sysConfigService;

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
                && !parseCerts(param.getUnionPayCerts()).isEmpty();
    }

    @Override
    public PaymentCallbackResult verify(PaymentCallbackContext context) {
        PaymentChannelParam param = loadParam(context.tenantId());
        if (param == null || !Boolean.TRUE.equals(param.getEnabled())) {
            throw new PaymentCallbackVerificationException("银联渠道未启用，拒绝回调");
        }
        Map<String, String> certs = parseCerts(param.getUnionPayCerts());
        if (certs.isEmpty()) {
            throw new PaymentCallbackVerificationException("银联平台证书未配置，无法验签");
        }

        Map<String, String> params = parseFormUrlEncoded(context.rawBodyAsString());
        String signature = params.get(FIELD_SIGNATURE);
        if (!hasText(signature)) {
            throw new PaymentCallbackVerificationException("回调缺少 signature 参数");
        }
        String certId = params.get("certId");
        if (!hasText(certId)) {
            throw new PaymentCallbackVerificationException("回调缺少 certId，无法确定用哪张证书验签");
        }
        String publicKeyPem = certs.get(certId);
        if (publicKeyPem == null) {
            throw new PaymentCallbackVerificationException(
                    "回调使用了未配置的银联证书 certId=" + certId + "（已配置 " + certs.size() + " 张）");
        }

        String algorithm = resolveAlgorithm(params.get("signMethod"));
        // 与支付宝的关键差异：银联的待验签串里 value 要 **URL 编码**
        String content = buildSignContent(params);
        if (!verifyRsa(content, signature, publicKeyPem, algorithm)) {
            throw new PaymentCallbackVerificationException("银联回调验签失败");
        }

        boolean success = RESP_CODE_SUCCESS.equals(params.get("respCode"));
        return new PaymentCallbackResult(
                params.get("orderId"),
                params.get("queryId"),
                parseFenToYuan(params.get("txnAmt")),
                success,
                context.rawBodyAsString());
    }

    @Override
    public String ackBody(boolean success) {
        return success ? "ok" : "fail";
    }

    // ─────────────────────────── 内部实现 ───────────────────────────

    /**
     * signMethod → JCA 算法名。
     *
     * <p>未知取值**直接拒绝**：猜一个默认算法会让验签用错摘要，后果是
     * 「以为验了签、其实验的算法和对方签的不是一回事」。</p>
     */
    private static String resolveAlgorithm(String signMethod) {
        if (signMethod == null) {
            throw new PaymentCallbackVerificationException("回调缺少 signMethod");
        }
        return switch (signMethod.trim()) {
            case "11" -> "SHA256withRSA";
            case "01" -> "SHA1withRSA";
            default -> throw new PaymentCallbackVerificationException(
                    "不支持的银联签名算法 signMethod=" + signMethod);
        };
    }

    /**
     * 构造待验签串：剔除 {@code signature} 与空值，按参数名 ASCII 升序，
     * **value 做 URL 编码**（UTF-8），以 {@code k=v&…} 拼接。
     */
    static String buildSignContent(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (FIELD_SIGNATURE.equals(key) || !hasText(value)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(urlEncode(value));
        }
        return sb.toString();
    }

    private static String urlEncode(String v) {
        try {
            return URLEncoder.encode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new PaymentCallbackVerificationException("待验签串 URL 编码失败：" + e.getMessage(), e);
        }
    }

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
        } catch (IllegalArgumentException e) {
            // 非法百分号转义等：原样返回，交给后续验签自然失败，不因解析问题抛 500
            return v;
        }
    }

    /** 分 → 元 */
    static BigDecimal parseFenToYuan(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean verifyRsa(String content, String signBase64, String publicKeyPem, String algorithm) {
        try {
            byte[] keyBytes = Base64.getDecoder()
                    .decode(publicKeyPem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", ""));
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            log.warn("银联验签异常，按不通过处理：{}", e.getMessage());
            return false;
        }
    }

    private PaymentChannelParam loadParam(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        MyBatisPlusConfig.setTempTenantId(tenantId);
        try {
            String raw = sysConfigService.getValue(CONFIG_KEY, null);
            if (!hasText(raw)) {
                return null;
            }
            return objectMapper.readValue(raw, PaymentChannelParam.class);
        } catch (Exception e) {
            log.warn("读取银联渠道配置失败：tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        } finally {
            MyBatisPlusConfig.clearTempTenantId();
        }
    }

    private Map<String, String> parseCerts(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!hasText(json)) {
            return map;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) {
                return map;
            }
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (hasText(e.getKey()) && hasText(e.getValue().asText())) {
                    map.put(e.getKey(), e.getValue().asText());
                }
            }
        } catch (Exception e) {
            log.warn("银联证书表不是合法 JSON，按未配置处理：{}", e.getMessage());
        }
        return map;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
