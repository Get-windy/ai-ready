package cn.aiedge.payment.crypto;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * 支付渠道的 RSA 签名/验签工具（SHA256withRSA，即「RSA2」）。
 *
 * <p><b>为什么单独抽出来</b>：2026-09-26 真接支付宝/银联时，通道侧要**签名**（下单、查单、退款），
 * 而回调侧要**验签**（{@code AlipayCallbackVerifier} / {@code UnionPayCallbackVerifier} 早已实现）。
 * 两边各写一份 RSA 代码，只要有一处细节不同（用解码前还是解码后的值、是否剔除空值、
 * 私钥用 PKCS8 还是 PKCS1），就会出现「**我们签的，自己验不过**」或更糟的
 * 「签名形同虚设」—— 而且这种错**不会编译报错、只在真实联调时炸**。
 * 抽成一处后，通道与验签器共用同一份实现，口径天然一致（单测再钉死一次）。</p>
 *
 * <p><b>待签串口径（支付宝 / 银联通用形态）</b>：排除指定字段与空值 → 参数名**字典序升序** →
 * {@code k=v} 以 {@code &} 连接。极少数差异（例如支付宝用 URL 解码后的值、银联用原值）
 * 由调用方在传入前处理好 —— 本类只负责「把给定的串签名/验签」。</p>
 */
@Slf4j
public final class Rsa2 {

    private static final String ALGORITHM = "SHA256withRSA";

    private Rsa2() {
    }

    /**
     * 用**应用私钥**（PKCS8、Base64，可带 PEM 头尾）对待签串做 SHA256withRSA 签名。
     *
     * @return Base64 签名；私钥缺失/格式非法时抛 {@link IllegalStateException}
     *         —— 支付签名失败**绝不能**静默降级成"不签"，那等于把订单暴露给伪造。
     */
    public static String sign(String content, String privateKeyBase64) {
        if (privateKeyBase64 == null || privateKeyBase64.isBlank()) {
            throw new IllegalStateException("支付渠道未配置应用私钥，无法签名");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(stripPem(privateKeyBase64));
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付渠道签名失败：" + e.getMessage(), e);
        }
    }

    /**
     * 用**渠道公钥**（X509、Base64，可带 PEM 头尾）验证 Base64 签名。
     *
     * <p>任何异常（公钥格式错、签名 Base64 错、算法不可用）一律返回 false —— 与
     * {@code AlipayCallbackVerifier} 既有口径一致：**验签异常即视为不通过**。</p>
     */
    public static boolean verify(String content, String signBase64, String publicKeyBase64) {
        if (content == null || signBase64 == null || publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            return false;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(stripPem(publicKeyBase64));
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            log.warn("RSA2 验签异常，按不通过处理：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 构造待签串：剔除 {@code exclude} 中的字段与**空值**，按参数名升序，{@code k=v} 以 {@code &} 连接。
     *
     * @param params  参数（值需按各自渠道的口径提前处理好：是否 URL 解码由调用方决定）
     * @param exclude 不参与签名的字段（如 {@code sign} / {@code sign_type} / {@code signature} / {@code signMethod}）
     */
    public static String buildSignContent(Map<String, String> params, Set<String> exclude) {
        return buildSignContent(params, exclude, null);
    }

    /**
     * 同 {@link #buildSignContent(Map, Set)}，但可对**值**做变换。
     *
     * <p><b>为什么需要它</b>：各渠道对「待签串里的值是否 URL 编码」并不统一 ——
     * 支付宝用**原值**，而本仓的银联验签器（{@code UnionPayCallbackVerifier}）按
     * 「值 UTF-8 URL 编码后」构造待验签串。签名与验签必须用**同一份口径**，
     * 否则会出现「我们签的、自己验不过」。故把这一步参数化，两边都走本方法。</p>
     *
     * @param valueTransform 值变换（null = 原值不变）；如银联传 {@code v -> URLEncoder.encode(v, UTF_8)}
     */
    public static String buildSignContent(Map<String, String> params, Set<String> exclude,
                                          UnaryOperator<String> valueTransform) {
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (key == null || value == null || value.isBlank()) {
                continue;
            }
            if (exclude != null && exclude.contains(key)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(valueTransform == null ? value : valueTransform.apply(value));
        }
        return sb.toString();
    }

    /** UTF-8 URL 编码（银联的待签串要求对值编码；支付宝用原值，故只在此提供工具、不默认启用）。 */
    public static String urlEncode(String v) {
        return java.net.URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    /** 剥离 PEM 头尾与空白，便于直接 Base64 解码。 */
    public static String stripPem(String key) {
        return key == null ? null : key.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    }
}
